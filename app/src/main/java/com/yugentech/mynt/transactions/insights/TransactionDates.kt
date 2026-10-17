package com.yugentech.mynt.transactions.insights

import com.yugentech.mynt.database.model.Expense
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Reading and writing the dates stored on a transaction.
 *
 * Entries keep their date as display text ("05 Oct 2026") and their month as "October 2026",
 * written with the device locale. Everything that groups, filters or sorts by date goes
 * through here instead of comparing those strings, so a mistyped or old-format date still
 * lands in the right month.
 */
object TransactionDates {

    private const val DAY_PATTERN = "dd MMM yyyy"
    private const val DAY_PATTERN_SHORT = "d MMM yyyy"
    private const val MONTH_PATTERN = "MMMM yyyy"

    // Parsing the same few hundred strings on every recomposition adds up; they never change.
    private val dayCache = ConcurrentHashMap<String, LocalDate>()
    private val monthCache = ConcurrentHashMap<String, YearMonth>()

    private fun format(pattern: String) = SimpleDateFormat(pattern, Locale.getDefault()).apply {
        isLenient = false
    }

    fun dayLabel(date: LocalDate): String = format(DAY_PATTERN).format(date.toDate())

    fun monthLabel(date: LocalDate): String = format(MONTH_PATTERN).format(date.toDate())

    fun monthLabel(month: YearMonth): String = monthLabel(month.atDay(1))

    fun parseDay(text: String): LocalDate? {
        if (text.isBlank()) return null
        dayCache[text]?.let { return it }
        val parsed = listOf(DAY_PATTERN, DAY_PATTERN_SHORT).firstNotNullOfOrNull { pattern ->
            runCatching { format(pattern).parse(text.trim()) }.getOrNull()
        }?.toLocalDate() ?: return null
        dayCache[text] = parsed
        return parsed
    }

    fun parseMonth(text: String): YearMonth? {
        if (text.isBlank()) return null
        monthCache[text]?.let { return it }
        val parsed = runCatching { format(MONTH_PATTERN).parse(text.trim()) }.getOrNull()
            ?.toLocalDate()
            ?.let { YearMonth.from(it) }
            ?: return null
        monthCache[text] = parsed
        return parsed
    }

    fun today(): LocalDate = LocalDate.now()

    private fun LocalDate.toDate(): Date = Date.from(atStartOfDay(ZoneId.systemDefault()).toInstant())

    private fun Date.toLocalDate(): LocalDate = Instant.ofEpochMilli(time)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()

    fun fromMillis(millis: Long): LocalDate = Instant.ofEpochMilli(millis)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
}

/** The day this transaction happened on; falls back to its month, then to when it was logged. */
val Expense.localDate: LocalDate
    get() = TransactionDates.parseDay(date)
        ?: TransactionDates.parseMonth(monthYear)?.atDay(1)
        ?: TransactionDates.fromMillis(timestamp)

val Expense.yearMonth: YearMonth
    get() = TransactionDates.parseDay(date)?.let { YearMonth.from(it) }
        ?: TransactionDates.parseMonth(monthYear)
        ?: YearMonth.from(TransactionDates.fromMillis(timestamp))

/** Newest day first; entries on the same day keep the order they were logged in. */
fun List<Expense>.sortedByDateDescending(): List<Expense> =
    sortedWith(compareByDescending<Expense> { it.localDate }.thenByDescending { it.timestamp })

/** Copies the expense onto [date], keeping both stored date fields in step. */
fun Expense.onDate(date: LocalDate): Expense = copy(
    date = TransactionDates.dayLabel(date),
    monthYear = TransactionDates.monthLabel(date)
)
