package com.yugentech.mynt.transactions.repository

import com.yugentech.mynt.database.dao.TransactionDao
import com.yugentech.mynt.database.mapper.toEntity
import com.yugentech.mynt.database.mapper.toExpense
import com.yugentech.mynt.database.model.Expense
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber

class TransactionRepositoryImpl(
    private val transactionDao: TransactionDao
) : TransactionRepository {

    override val transactions: Flow<List<Expense>> = transactionDao.getAllTransactions()
        .map { entities -> entities.map { it.toExpense() } }

    override suspend fun getTransaction(transactionId: String): Expense? {
        return transactionDao.getTransaction(transactionId)?.toExpense()
    }

    // Inserts a new transaction or overwrites the existing one with the same id
    override suspend fun saveTransaction(expense: Expense) {
        Timber.d("Saving transaction ${expense.id}")
        transactionDao.upsertTransaction(expense.toEntity())
    }

    override suspend fun saveTransactions(expenses: List<Expense>) {
        Timber.d("Saving ${expenses.size} transactions")
        transactionDao.upsertTransactions(expenses.map { it.toEntity() })
    }

    override suspend fun deleteTransaction(transactionId: String) {
        Timber.d("Deleting transaction $transactionId")
        transactionDao.deleteTransaction(transactionId)
    }
}
