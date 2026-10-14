package com.yourname.expensetracker

import androidx.compose.ui.graphics.Color
import java.util.Locale

/**
 * Transaction type classification (Expense or Income).
 */
enum class TransactionType {
    EXPENSE,
    INCOME
}

/**
 * Data class representing a financial transaction (Expense or Income).
 *
 * @property id Unique identifier
 * @property title Name/description of the transaction (e.g., "Lunch" or "Salary")
 * @property amount Value in Rupees
 * @property category Category name
 * @property date Display date string (e.g., "18 Oct 2026")
 * @property monthYear Month and year for filtering (e.g., "October 2026")
 * @property type Transaction type (EXPENSE or INCOME)
 */
data class Expense(
    val id: String,
    val title: String,
    val amount: Double,
    val category: String,
    val date: String,
    val monthYear: String,
    val type: TransactionType = TransactionType.EXPENSE
)

// Category lists for Expense and Income
val expenseCategories = listOf("Food", "Travel", "Shopping", "Bills", "Entertainment", "Other")
val incomeCategories = listOf("Salary", "Freelance", "Gift", "Investment", "Other")

/**
 * Helper function returning category emoji and background color for Expense or Income.
 */
fun getCategoryIconAndColor(category: String, type: TransactionType = TransactionType.EXPENSE): Pair<String, Color> {
    return if (type == TransactionType.EXPENSE) {
        when (category.lowercase(Locale.getDefault())) {
            "food" -> Pair("🍔", Color(0xFFFFEBEE))
            "travel" -> Pair("🚗", Color(0xFFE3F2FD))
            "shopping" -> Pair("🛍️", Color(0xFFF3E5F5))
            "bills" -> Pair("📄", Color(0xFFE8F5E9))
            "entertainment" -> Pair("🎬", Color(0xFFFFF3E0))
            else -> Pair("💰", Color(0xFFECEFF1))
        }
    } else {
        when (category.lowercase(Locale.getDefault())) {
            "salary" -> Pair("💼", Color(0xFFE8F5E9))
            "freelance" -> Pair("💻", Color(0xFFE0F7FA))
            "gift" -> Pair("🎁", Color(0xFFF3E5F5))
            "investment" -> Pair("📈", Color(0xFFFFF8E1))
            else -> Pair("💵", Color(0xFFE8F5E9))
        }
    }
}
