package com.yugentech.mynt.database.model

/**
 * Transaction type classification.
 */
enum class TransactionType {
    EXPENSE,
    INCOME,
    LENT,
    BORROWED
}

/**
 * Data class representing a financial transaction.
 *
 * [timestamp] is when the entry was first created; it is kept across edits so a corrected
 * entry doesn't jump to the top of the list.
 */
data class Expense(
    val id: String,
    val title: String,
    val amount: Double,
    val category: String,
    val date: String,
    val monthYear: String,
    val type: TransactionType = TransactionType.EXPENSE,
    val personName: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

val expenseCategories = listOf(
    "Food", "Groceries", "Travel", "Shopping", "Bills", "Rent",
    "Health", "Entertainment", "Education", "EMI", "Other"
)
val incomeCategories = listOf("Salary", "Freelance", "Business", "Gift", "Investment", "Refund", "Other")

const val LENDING_CATEGORY = "Lending"
const val BORROWING_CATEGORY = "Borrowing"

// Recorded against a person to square up what's owed, so their balance returns to zero
// while the history of who lent what stays intact.
const val SETTLEMENT_CATEGORY = "Settlement"
