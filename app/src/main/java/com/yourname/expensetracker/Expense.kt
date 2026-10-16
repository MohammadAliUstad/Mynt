package com.yourname.expensetracker

import androidx.compose.ui.graphics.Color
import java.util.Locale

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
 */
data class Expense(
    val id: String,
    val title: String,
    val amount: Double,
    val category: String,
    val date: String,
    val monthYear: String,
    val type: TransactionType = TransactionType.EXPENSE,
    val personName: String = ""
)

val expenseCategories = listOf("Food", "Travel", "Shopping", "Bills", "Entertainment", "EMI", "Other")
val incomeCategories = listOf("Salary", "Freelance", "Gift", "Investment", "Other")

fun getCategoryIconAndColor(category: String, type: TransactionType = TransactionType.EXPENSE): Pair<String, Color> {
    return when (type) {
        TransactionType.LENT -> Pair("↗️", Color(0xFFE8F5E9))
        TransactionType.BORROWED -> Pair("↙️", Color(0xFFFFEBEE))
        TransactionType.INCOME -> {
            when (category.lowercase(Locale.getDefault())) {
                "salary" -> Pair("💼", Color(0xFFE8F5E9))
                "freelance" -> Pair("💻", Color(0xFFE0F7FA))
                "gift" -> Pair("🎁", Color(0xFFF3E5F5))
                "investment" -> Pair("📈", Color(0xFFFFF8E1))
                else -> Pair("💵", Color(0xFFE8F5E9))
            }
        }
        else -> {
            when (category.lowercase(Locale.getDefault())) {
                "food" -> Pair("🍔", Color(0xFFFFEBEE))
                "travel" -> Pair("🚗", Color(0xFFE3F2FD))
                "shopping" -> Pair("🛍️", Color(0xFFF3E5F5))
                "bills" -> Pair("📄", Color(0xFFE8F5E9))
                "entertainment" -> Pair("🎬", Color(0xFFFFF3E0))
                "emi" -> Pair("🏦", Color(0xFFE1F5FE))
                else -> Pair("💰", Color(0xFFECEFF1))
            }
        }
    }
}