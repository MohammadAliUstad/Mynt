package com.yugentech.mynt.transactions.repository

import com.yugentech.mynt.database.model.Expense
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    val transactions: Flow<List<Expense>>
    suspend fun getTransaction(transactionId: String): Expense?
    suspend fun saveTransaction(expense: Expense)
    suspend fun saveTransactions(expenses: List<Expense>)
    suspend fun deleteTransaction(transactionId: String)
}
