package com.yugentech.mynt.transactions.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yugentech.mynt.database.model.Expense
import com.yugentech.mynt.transactions.repository.TransactionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TransactionsViewModel(
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    val transactions: StateFlow<List<Expense>> = transactionRepository.transactions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun saveTransaction(expense: Expense) {
        viewModelScope.launch {
            transactionRepository.saveTransaction(expense)
        }
    }

    fun saveTransactions(expenses: List<Expense>) {
        if (expenses.isEmpty()) return
        viewModelScope.launch {
            transactionRepository.saveTransactions(expenses)
        }
    }

    fun deleteTransaction(expense: Expense) {
        viewModelScope.launch {
            transactionRepository.deleteTransaction(expense.id)
        }
    }

    // Undo for a delete: the expense still carries its id and original timestamp,
    // so it goes back exactly where it was.
    fun restoreTransaction(expense: Expense) = saveTransaction(expense)
}
