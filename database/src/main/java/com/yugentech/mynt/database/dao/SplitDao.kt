package com.yugentech.mynt.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.yugentech.mynt.database.entity.SettledRecordEntity
import com.yugentech.mynt.database.entity.SplitBillEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SplitDao {
    @Query("SELECT * FROM split_bills ORDER BY dateIso DESC")
    fun getActiveBills(): Flow<List<SplitBillEntity>>

    @Query("SELECT * FROM split_bills")
    suspend fun getActiveBillsSync(): List<SplitBillEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBills(bills: List<SplitBillEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBill(bill: SplitBillEntity)

    @Query("DELETE FROM split_bills WHERE id = :id")
    suspend fun deleteBill(id: String)

    @Query("DELETE FROM split_bills")
    suspend fun clearActiveBills()

    @Query("SELECT * FROM settled_records ORDER BY settledOnIso DESC")
    fun getSettledRecords(): Flow<List<SettledRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSettledRecords(records: List<SettledRecordEntity>)

    @Query("DELETE FROM settled_records WHERE id = :id")
    suspend fun deleteSettledRecord(id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSettledRecord(record: SettledRecordEntity)
}