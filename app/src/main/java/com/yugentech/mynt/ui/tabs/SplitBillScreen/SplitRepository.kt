package com.yugentech.mynt.split.repository

import com.yugentech.mynt.split.model.SettledRecord
import com.yugentech.mynt.split.model.SplitBill
import kotlinx.coroutines.flow.Flow

interface SplitRepository {
    val activeBills: Flow<List<SplitBill>>
    val settledRecords: Flow<List<SettledRecord>>
    suspend fun saveBill(bill: SplitBill)
    suspend fun deleteBill(billId: String)
    suspend fun clearActiveBills()
    suspend fun saveSettledRecord(record: SettledRecord)
    suspend fun deleteSettledRecord(recordId: String)
}