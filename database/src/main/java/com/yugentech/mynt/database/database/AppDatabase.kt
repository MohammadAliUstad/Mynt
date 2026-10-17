package com.yugentech.mynt.database.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.yugentech.mynt.database.dao.TransactionDao
import com.yugentech.mynt.database.entity.TransactionEntity

@Database(
    entities = [
        TransactionEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
}
