package com.yugentech.mynt.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.yugentech.mynt.database.model.TransactionType

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val amount: Double,
    val category: String,
    val date: String,
    val monthYear: String,
    val type: String = TransactionType.EXPENSE.name,
    val personName: String = "",
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis()
)