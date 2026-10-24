package com.yugentech.mynt.database.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.yugentech.mynt.database.dao.SplitDao
import com.yugentech.mynt.database.dao.TransactionDao
import com.yugentech.mynt.database.entity.SettledRecordEntity
import com.yugentech.mynt.database.entity.SplitBillEntity
import com.yugentech.mynt.database.entity.TransactionEntity

@Database(
    entities = [
        TransactionEntity::class,
        SplitBillEntity::class,
        SettledRecordEntity::class
    ],
    version = 3, // Bumped from 2 to 3 to include the new note column in TransactionEntity
    exportSchema = false
)
@TypeConverters(DatabaseConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun splitDao(): SplitDao
}