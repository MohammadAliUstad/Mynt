package com.yugentech.mynt.transactions.export

import com.yugentech.mynt.database.model.Expense
import com.yugentech.mynt.transactions.insights.localDate
import com.yugentech.mynt.transactions.insights.sortedByDateDescending
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Every entry as a spreadsheet-friendly CSV: ISO dates so it sorts properly anywhere,
 * plain numbers for amounts, and quoting per RFC 4180.
 */
object CsvExporter {

    private val header = listOf("Date", "Type", "Title", "Category", "Person", "Amount", "Currency")

    fun export(expenses: List<Expense>, currencySymbol: String): String = buildString {
        appendLine(header.joinToString(","))
        expenses.sortedByDateDescending().forEach { expense ->
            val row = listOf(
                expense.localDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                expense.type.name.lowercase(Locale.ROOT).replaceFirstChar { it.uppercase() },
                expense.title,
                expense.category,
                expense.personName,
                String.format(Locale.ROOT, "%.2f", expense.amount),
                currencySymbol
            )
            appendLine(row.joinToString(",") { escape(it) })
        }
    }

    fun fileName(today: java.time.LocalDate = java.time.LocalDate.now()): String =
        "mynt-transactions-${today.format(DateTimeFormatter.ISO_LOCAL_DATE)}.csv"

    internal fun escape(value: String): String {
        // Leading =, +, - or @ would run as a formula in a spreadsheet; neutralise it.
        val safe = if (value.isNotEmpty() && value.first() in "=+-@" && value.toDoubleOrNull() == null) "'$value" else value
        val needsQuotes = safe.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        return if (needsQuotes) "\"" + safe.replace("\"", "\"\"") + "\"" else safe
    }
}
