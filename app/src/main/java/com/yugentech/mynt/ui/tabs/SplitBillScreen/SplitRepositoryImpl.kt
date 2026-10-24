package com.yugentech.mynt.split.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.yugentech.mynt.database.dao.SplitDao
import com.yugentech.mynt.database.entity.SettledRecordEntity
import com.yugentech.mynt.database.entity.SplitBillEntity
import com.yugentech.mynt.database.entity.toEntity
import com.yugentech.mynt.database.entity.toModel
import com.yugentech.mynt.split.model.SettledRecord
import com.yugentech.mynt.split.model.SplitBill
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class SplitRepositoryImpl(
    private val splitDao: SplitDao,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : SplitRepository {

    override val activeBills: Flow<List<SplitBill>> = splitDao.getActiveBills()
        .map { entities -> entities.map { it.toModel() } }

    override val settledRecords: Flow<List<SettledRecord>> = splitDao.getSettledRecords()
        .map { entities -> entities.map { it.toModel() } }

    init {
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                // 1. Sync Active Bills from Cloud down to Room
                firestore.collection("users").document(user.uid).collection("split_bills")
                    .addSnapshotListener { snapshot, _ ->
                        if (snapshot != null) {
                            val cloudEntities = snapshot.documents.map { doc ->
                                SplitBillEntity(
                                    id = doc.id,
                                    title = doc.getString("title") ?: "",
                                    amount = doc.getDouble("amount") ?: 0.0,
                                    paidBy = doc.getString("paidBy") ?: "",
                                    sharesJson = doc.getString("sharesJson") ?: "{}",
                                    dateIso = doc.getString("dateIso") ?: ""
                                )
                            }
                            CoroutineScope(Dispatchers.IO).launch {
                                val cloudIds = cloudEntities.map { it.id }.toSet()
                                val localIds = splitDao.getActiveBillsSync().map { it.id }
                                val toDelete = localIds.filterNot { it in cloudIds }

                                toDelete.forEach { splitDao.deleteBill(it) }
                                if (cloudEntities.isNotEmpty()) {
                                    splitDao.upsertBills(cloudEntities)
                                } else {
                                    splitDao.clearActiveBills()
                                }
                            }
                        }
                    }

                // 2. Sync Settled Records from Cloud down to Room
                firestore.collection("users").document(user.uid).collection("settled_records")
                    .addSnapshotListener { snapshot, _ ->
                        if (snapshot != null) {
                            val cloudEntities = snapshot.documents.map { doc ->
                                SettledRecordEntity(
                                    id = doc.id,
                                    settledOnIso = doc.getString("settledOnIso") ?: "",
                                    peopleJson = doc.getString("peopleJson") ?: "[]",
                                    billsJson = doc.getString("billsJson") ?: "[]",
                                    transfersJson = doc.getString("transfersJson") ?: "[]"
                                )
                            }
                            CoroutineScope(Dispatchers.IO).launch {
                                if (cloudEntities.isNotEmpty()) {
                                    splitDao.upsertSettledRecords(cloudEntities)
                                }
                            }
                        }
                    }
            }
        }
    }

    override suspend fun saveBill(bill: SplitBill) {
        // Save Local
        val entity = bill.toEntity()
        splitDao.upsertBill(entity)

        // Save Cloud
        auth.currentUser?.uid?.let { uid ->
            val map = hashMapOf(
                "title" to entity.title,
                "amount" to entity.amount,
                "paidBy" to entity.paidBy,
                "sharesJson" to entity.sharesJson,
                "dateIso" to entity.dateIso
            )
            firestore.collection("users").document(uid).collection("split_bills")
                .document(bill.id).set(map)
        }
    }

    override suspend fun deleteBill(billId: String) {
        // Delete Local
        splitDao.deleteBill(billId)

        // Delete Cloud
        auth.currentUser?.uid?.let { uid ->
            firestore.collection("users").document(uid).collection("split_bills")
                .document(billId).delete()
        }
    }

    override suspend fun clearActiveBills() {
        // Clear Local
        val localBills = splitDao.getActiveBillsSync()
        splitDao.clearActiveBills()

        // Clear Cloud atomically via WriteBatch
        auth.currentUser?.uid?.let { uid ->
            if (localBills.isNotEmpty()) {
                val collection = firestore.collection("users").document(uid).collection("split_bills")
                val batch = firestore.batch()
                localBills.forEach { bill ->
                    batch.delete(collection.document(bill.id))
                }
                batch.commit()
            }
        }
    }

    override suspend fun saveSettledRecord(record: SettledRecord) {
        // Save Local
        val entity = record.toEntity()
        splitDao.upsertSettledRecord(entity)

        // Save Cloud
        auth.currentUser?.uid?.let { uid ->
            val map = hashMapOf(
                "settledOnIso" to entity.settledOnIso,
                "peopleJson" to entity.peopleJson,
                "billsJson" to entity.billsJson,
                "transfersJson" to entity.transfersJson
            )
            firestore.collection("users").document(uid).collection("settled_records")
                .document(record.id).set(map)
        }
    }

    override suspend fun deleteSettledRecord(recordId: String) {
        // Cloud first, so the sync listener can't bring the record back
        auth.currentUser?.uid?.let { uid ->
            firestore.collection("users").document(uid).collection("settled_records")
                .document(recordId).delete()
        }

        splitDao.deleteSettledRecord(recordId)
    }
}