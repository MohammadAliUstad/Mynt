package com.yourname.expensetracker

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

object FirestoreRepository {

    private const val TAG = "FirestoreRepo"
    private val db: FirebaseFirestore get() = FirebaseFirestore.getInstance()

    fun listenToUserTransactions(
        userId: String,
        onUpdate: (List<Expense>) -> Unit
    ): ListenerRegistration {
        return db.collection("users")
            .document(userId)
            .collection("transactions")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener

                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            val id = doc.id
                            val title = doc.getString("title") ?: ""
                            val amount = doc.getDouble("amount") ?: 0.0
                            val category = doc.getString("category") ?: "Other"
                            val date = doc.getString("date") ?: ""
                            val monthYear = doc.getString("monthYear") ?: ""
                            val personName = doc.getString("personName") ?: ""
                            val typeStr = doc.getString("type") ?: "EXPENSE"
                            val type = try {
                                TransactionType.valueOf(typeStr)
                            } catch (e: Exception) {
                                TransactionType.EXPENSE
                            }

                            Expense(
                                id = id,
                                title = title,
                                amount = amount,
                                category = category,
                                date = date,
                                monthYear = monthYear,
                                type = type,
                                personName = personName
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                    onUpdate(list)
                }
            }
    }

    fun addTransaction(userId: String, expense: Expense) {
        val docData = hashMapOf(
            "id" to expense.id,
            "title" to expense.title,
            "amount" to expense.amount,
            "category" to expense.category,
            "date" to expense.date,
            "monthYear" to expense.monthYear,
            "type" to expense.type.name,
            "personName" to expense.personName,
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("users")
            .document(userId)
            .collection("transactions")
            .document(expense.id)
            .set(docData)
    }

    fun deleteTransaction(userId: String, transactionId: String) {
        db.collection("users")
            .document(userId)
            .collection("transactions")
            .document(transactionId)
            .delete()
    }
}