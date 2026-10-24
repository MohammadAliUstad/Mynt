package com.yugentech.mynt.transactions.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.yugentech.mynt.database.dao.TransactionDao
import com.yugentech.mynt.database.entity.TransactionEntity
import com.yugentech.mynt.database.mapper.toEntity
import com.yugentech.mynt.database.mapper.toExpense
import com.yugentech.mynt.database.model.Expense
import com.yugentech.mynt.database.model.TransactionType
import com.yugentech.mynt.transactions.insights.FinanceInsights
import com.yugentech.mynt.transactions.insights.TransactionDates
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber

class TransactionRepositoryImpl(
    private val transactionDao: TransactionDao,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : TransactionRepository {

    override val transactions: Flow<List<Expense>> = transactionDao.getAllTransactions()
        .map { entities -> entities.map { it.toExpense() } }

    /** One snapshot from the cloud, waiting to be applied to Room. */
    private class CloudBatch(
        val uid: String,
        val upserts: List<TransactionEntity>,
        val removedIds: List<String>,
        val isLocalEcho: Boolean
    )

    // Everything coming down from the cloud goes through one queue with one consumer, so a delete
    // followed by an Undo is applied in exactly the order Firestore reported it.
    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val syncQueue = Channel<CloudBatch>(Channel.UNLIMITED)
    private var expensesListener: ListenerRegistration? = null

    init {
        syncScope.launch {
            for (batch in syncQueue) {
                try {
                    batch.removedIds.forEach { transactionDao.deleteTransaction(it) }
                    if (batch.upserts.isNotEmpty()) {
                        transactionDao.upsertTransactions(batch.upserts)
                    }
                    // Our own writes echo back here before the server confirms them, and
                    // saveTransaction already updated the streak for those.
                    if (!batch.isLocalEcho) updateUserStreak(batch.uid)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to apply cloud changes to the local database")
                }
            }
        }

        auth.addAuthStateListener { firebaseAuth ->
            // Signing in again replaces the old listener instead of stacking a second one on top.
            expensesListener?.remove()
            expensesListener = null

            val user = firebaseAuth.currentUser
            if (user != null) {
                // Sync Expenses from Cloud down to Room
                expensesListener = firestore.collection("users").document(user.uid).collection("expenses")
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            Timber.e(error, "Firestore SnapshotListener error for user ${user.uid}")
                            return@addSnapshotListener
                        }
                        if (snapshot != null) {
                            // Entries deleted elsewhere (another device) must disappear here too.
                            val removedIds = snapshot.documentChanges
                                .filter { it.type == DocumentChange.Type.REMOVED }
                                .map { it.document.id }

                            val cloudEntities = snapshot.documents.mapNotNull { doc ->
                                TransactionEntity(
                                    id = doc.id,
                                    title = doc.getString("title") ?: "",
                                    amount = doc.getDouble("amount") ?: 0.0,
                                    category = doc.getString("category") ?: "",
                                    date = doc.getString("date") ?: "",
                                    monthYear = doc.getString("monthYear") ?: "",
                                    type = doc.getString("type") ?: TransactionType.EXPENSE.name,
                                    personName = doc.getString("personName") ?: "",
                                    note = doc.getString("note") ?: "",
                                    timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                                )
                            }

                            syncQueue.trySend(
                                CloudBatch(
                                    uid = user.uid,
                                    upserts = cloudEntities,
                                    removedIds = removedIds,
                                    isLocalEcho = snapshot.metadata.hasPendingWrites()
                                )
                            )
                        }
                    }
            } else {
                Timber.w("No authenticated user present in FirebaseAuth")
            }
        }
    }

    private suspend fun updateUserStreak(uid: String) {
        try {
            val entities = transactionDao.getTransactionList()
            val expenses = entities.map { it.toExpense() }
            val streak = FinanceInsights.loggingStreak(expenses)
            val streakData = hashMapOf<String, Any>(
                "streak" to streak,
                "lastActiveDate" to TransactionDates.today().toString(),
                "updatedAt" to System.currentTimeMillis()
            )
            firestore.collection("users").document(uid)
                .set(streakData, SetOptions.merge())
                .addOnSuccessListener {
                    Timber.d("Updated streak ($streak) in Firestore for user $uid")
                }
                .addOnFailureListener { e ->
                    Timber.e(e, "Failed to update streak in Firestore for user $uid")
                }
        } catch (e: Exception) {
            Timber.e(e, "Error updating user streak in Firestore")
        }
    }

    override suspend fun getTransaction(transactionId: String): Expense? {
        return transactionDao.getTransaction(transactionId)?.toExpense()
    }

    override suspend fun saveTransaction(expense: Expense) {
        // Save Local
        transactionDao.upsertTransaction(expense.toEntity())

        // Save Cloud
        val uid = auth.currentUser?.uid
        if (uid != null) {
            val map = hashMapOf(
                "title" to expense.title,
                "amount" to expense.amount,
                "category" to expense.category,
                "date" to expense.date,
                "monthYear" to expense.monthYear,
                "type" to expense.type.name,
                "personName" to expense.personName,
                "note" to expense.note,
                "timestamp" to expense.timestamp
            )
            firestore.collection("users").document(uid).collection("expenses")
                .document(expense.id).set(map)
                .addOnSuccessListener {
                    Timber.d("Successfully saved transaction ${expense.id} to Firestore")
                }
                .addOnFailureListener { e ->
                    Timber.e(e, "Failed to save transaction ${expense.id} to Firestore")
                }
            updateUserStreak(uid)
        } else {
            Timber.w("Cannot save transaction to Firestore: user is not authenticated")
        }
    }

    override suspend fun saveTransactions(expenses: List<Expense>) {
        transactionDao.upsertTransactions(expenses.map { it.toEntity() })

        val uid = auth.currentUser?.uid
        if (uid != null) {
            val batch = firestore.batch()
            val collection = firestore.collection("users").document(uid).collection("expenses")
            expenses.forEach { expense ->
                val map = hashMapOf(
                    "title" to expense.title,
                    "amount" to expense.amount,
                    "category" to expense.category,
                    "date" to expense.date,
                    "monthYear" to expense.monthYear,
                    "type" to expense.type.name,
                    "personName" to expense.personName,
                    "note" to expense.note,
                    "timestamp" to expense.timestamp
                )
                batch.set(collection.document(expense.id), map)
            }
            batch.commit()
                .addOnSuccessListener {
                    Timber.d("Successfully saved batch of ${expenses.size} transactions to Firestore")
                }
                .addOnFailureListener { e ->
                    Timber.e(e, "Failed to save batch transactions to Firestore")
                }
            updateUserStreak(uid)
        } else {
            Timber.w("Cannot save batch transactions to Firestore: user is not authenticated")
        }
    }

    override suspend fun deleteTransaction(transactionId: String) {
        transactionDao.deleteTransaction(transactionId)

        val uid = auth.currentUser?.uid
        if (uid != null) {
            firestore.collection("users").document(uid).collection("expenses")
                .document(transactionId).delete()
                .addOnSuccessListener {
                    Timber.d("Successfully deleted transaction $transactionId from Firestore")
                }
                .addOnFailureListener { e ->
                    Timber.e(e, "Failed to delete transaction $transactionId from Firestore")
                }
            updateUserStreak(uid)
        } else {
            Timber.w("Cannot delete transaction from Firestore: user is not authenticated")
        }
    }
}