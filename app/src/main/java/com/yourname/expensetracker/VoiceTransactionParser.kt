package com.yourname.expensetracker

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

/**
 * Helper object to parse spoken text into a structured Expense or Income transaction.
 */
object VoiceTransactionParser {

    /**
     * Parses spoken sentence into a structured [Expense] object, or returns null if no valid amount is found.
     */
    fun parse(spokenText: String): Expense? {
        val raw = spokenText.trim()
        if (raw.isBlank()) return null

        val lower = raw.lowercase(Locale.getDefault())

        // 1. Extract amount number (e.g., 250, 50000, 1,200.50)
        val regexAmount = Regex("""\b(\d+(?:,\d+)*(?:\.\d+)?)\b""")
        val matchAmount = regexAmount.find(raw) ?: return null
        val amountDouble = matchAmount.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null

        if (amountDouble <= 0.0) return null

        // 2. Determine Transaction Type (EXPENSE vs INCOME)
        val incomeKeywords = listOf(
            "earned", "receive", "received", "got", "salary", "freelance",
            "income", "gift", "investment", "dividend", "bonus", "credited", "added", "paycheck"
        )
        val isIncome = incomeKeywords.any { lower.contains(it) }
        val type = if (isIncome) TransactionType.INCOME else TransactionType.EXPENSE

        // 3. Determine Category
        val category = determineCategory(lower, type)

        // 4. Determine Title
        val title = determineTitle(raw, matchAmount.groupValues[0], category)

        // 5. Date & MonthYear (Defaults to Today)
        val calendar = Calendar.getInstance()
        val dateFormatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val monthYearFormatter = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

        return Expense(
            id = UUID.randomUUID().toString(),
            title = title,
            amount = amountDouble,
            category = category,
            date = dateFormatter.format(calendar.time),
            monthYear = monthYearFormatter.format(calendar.time),
            type = type
        )
    }

    private fun determineCategory(lower: String, type: TransactionType): String {
        return if (type == TransactionType.EXPENSE) {
            when {
                containsAny(lower, "food", "lunch", "dinner", "breakfast", "coffee", "snack", "snacks", "burger", "pizza", "restaurant", "tea", "groceries", "grocery") -> "Food"
                containsAny(lower, "travel", "auto", "cab", "uber", "ola", "bus", "train", "flight", "taxi", "fare", "fuel", "petrol", "diesel") -> "Travel"
                containsAny(lower, "shopping", "clothes", "shoes", "amazon", "flipkart", "dress", "mall", "market") -> "Shopping"
                containsAny(lower, "bill", "bills", "electricity", "water", "wifi", "internet", "recharge", "mobile", "rent", "utility") -> "Bills"
                containsAny(lower, "movie", "cinema", "show", "ticket", "tickets", "game", "netflix", "entertainment") -> "Entertainment"
                else -> "Other"
            }
        } else {
            when {
                containsAny(lower, "salary", "paycheck", "wages") -> "Salary"
                containsAny(lower, "freelance", "contract", "client", "gig") -> "Freelance"
                containsAny(lower, "gift", "present", "reward") -> "Gift"
                containsAny(lower, "investment", "stock", "stocks", "crypto", "dividend", "interest", "return") -> "Investment"
                else -> "Other"
            }
        }
    }

    private fun determineTitle(raw: String, amountStr: String, category: String): String {
        var cleaned = raw
            .replace(amountStr, "", ignoreCase = true)
            .replace(Regex("""(?i)\b(spent|paid|earned|received|got|bought|for|on|in|rs|rupees|INR)\b"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()

        if (cleaned.length < 2) {
            cleaned = category
        }

        return cleaned.split(" ").joinToString(" ") { word ->
            word.lowercase(Locale.getDefault()).replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            }
        }
    }

    private fun containsAny(text: String, vararg keywords: String): Boolean {
        return keywords.any { text.contains(it) }
    }
}
