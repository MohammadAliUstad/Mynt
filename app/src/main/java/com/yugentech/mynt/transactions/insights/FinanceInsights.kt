package com.yugentech.mynt.transactions.insights

import com.yugentech.mynt.database.model.Expense
import com.yugentech.mynt.database.model.TransactionType
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.util.Locale

data class MonthSummary(
    val month: YearMonth,
    val income: Double,
    val expense: Double,
    val lent: Double,
    val borrowed: Double,
    val transactionCount: Int
) {
    val net: Double get() = income - expense

    /** Share of income kept this month, from 0 to 1; null when nothing was earned. */
    val savingsRate: Float?
        get() = if (income > 0) (net / income).toFloat().coerceIn(-1f, 1f) else null
}

data class CategoryShare(
    val category: String,
    val amount: Double,
    val share: Float,
    val count: Int
)

data class DayTotal(
    val date: LocalDate,
    val expense: Double,
    val income: Double
)

/**
 * What one person and you owe each other, across every lent and borrowed entry.
 * A positive [balance] means they owe you; negative means you owe them.
 */
data class PersonBalance(
    val name: String,
    val balance: Double,
    val totalLent: Double,
    val totalBorrowed: Double,
    val entryCount: Int,
    val lastActivity: LocalDate
) {
    val isSettled: Boolean get() = kotlin.math.abs(balance) < 0.005
}

data class BudgetStatus(
    val budget: Double,
    val spent: Double,
    val daysInMonth: Int,
    /** Days left in the month, today included; 0 for a month already over. */
    val daysLeft: Int,
    val isCurrentMonth: Boolean
) {
    val remaining: Double get() = budget - spent
    val progress: Float get() = if (budget > 0) (spent / budget).toFloat() else 0f
    val isOver: Boolean get() = spent > budget

    /** How much can still be spent per day for the rest of the month without going over. */
    val dailyAllowance: Double
        get() = if (daysLeft > 0) (remaining / daysLeft).coerceAtLeast(0.0) else 0.0

    /** Where spending lands by month end if it carries on at today's pace. */
    val projectedSpend: Double
        get() {
            val daysElapsed = daysInMonth - daysLeft + 1
            return if (isCurrentMonth && daysElapsed > 0) spent / daysElapsed * daysInMonth else spent
        }
}

/** A short, plain observation about the month for the insight cards. */
data class Insight(
    val title: String,
    val detail: String,
    val tone: Tone
) {
    enum class Tone { POSITIVE, NEUTRAL, WARNING }
}

/**
 * Pure calculations over the transaction list. Nothing here touches Android, so the
 * screens, the reminders and the tests all agree on what a month's numbers are.
 */
object FinanceInsights {

    fun inMonth(all: List<Expense>, month: YearMonth): List<Expense> =
        all.filter { it.yearMonth == month }

    fun summary(all: List<Expense>, month: YearMonth): MonthSummary {
        val entries = inMonth(all, month)
        return MonthSummary(
            month = month,
            income = entries.total(TransactionType.INCOME),
            expense = entries.total(TransactionType.EXPENSE),
            lent = entries.total(TransactionType.LENT),
            borrowed = entries.total(TransactionType.BORROWED),
            transactionCount = entries.size
        )
    }

    fun categoryBreakdown(
        entries: List<Expense>,
        type: TransactionType = TransactionType.EXPENSE
    ): List<CategoryShare> {
        val ofType = entries.filter { it.type == type }
        val total = ofType.sumOf { it.amount }
        if (total <= 0) return emptyList()
        return ofType
            .groupBy { it.category.ifBlank { "Other" } }
            .map { (category, list) ->
                val sum = list.sumOf { it.amount }
                CategoryShare(category, sum, (sum / total).toFloat(), list.size)
            }
            .sortedByDescending { it.amount }
    }

    /** Spending and earning for every day of [month], zero-filled so a chart has no gaps. */
    fun dailyTotals(all: List<Expense>, month: YearMonth): List<DayTotal> {
        val byDay = inMonth(all, month).groupBy { it.localDate }
        return (1..month.lengthOfMonth()).map { day ->
            val date = month.atDay(day)
            val entries = byDay[date].orEmpty()
            DayTotal(
                date = date,
                expense = entries.total(TransactionType.EXPENSE),
                income = entries.total(TransactionType.INCOME)
            )
        }
    }

    /** The [count] months ending with [endMonth], oldest first. */
    fun monthlyTrend(all: List<Expense>, endMonth: YearMonth, count: Int = 6): List<MonthSummary> =
        (count - 1 downTo 0).map { summary(all, endMonth.minusMonths(it.toLong())) }

    fun budgetStatus(
        all: List<Expense>,
        month: YearMonth,
        budget: Double,
        today: LocalDate = TransactionDates.today()
    ): BudgetStatus? {
        if (budget <= 0) return null
        val currentMonth = YearMonth.from(today)
        val daysLeft = when {
            month.isBefore(currentMonth) -> 0
            month.isAfter(currentMonth) -> month.lengthOfMonth()
            else -> month.lengthOfMonth() - today.dayOfMonth + 1
        }
        return BudgetStatus(
            budget = budget,
            spent = inMonth(all, month).total(TransactionType.EXPENSE),
            daysInMonth = month.lengthOfMonth(),
            daysLeft = daysLeft,
            isCurrentMonth = month == currentMonth
        )
    }

    /**
     * Consecutive days with at least one entry, counting back from today. A day without
     * entries yet doesn't break the streak until it's over, so it counts back from
     * yesterday in that case.
     */
    fun loggingStreak(all: List<Expense>, today: LocalDate = TransactionDates.today()): Int {
        if (all.isEmpty()) return 0
        val days = all.mapTo(HashSet()) { it.localDate }
        var cursor = if (today in days) today else today.minusDays(1)
        var streak = 0
        while (cursor in days) {
            streak++
            cursor = cursor.minusDays(1)
        }
        return streak
    }

    /** Everyone with lent or borrowed entries, those with money outstanding first. */
    fun personBalances(all: List<Expense>): List<PersonBalance> {
        return all
            .filter { it.type == TransactionType.LENT || it.type == TransactionType.BORROWED }
            .groupBy { it.counterparty().lowercase(Locale.getDefault()) }
            .map { (_, entries) ->
                val lent = entries.total(TransactionType.LENT)
                val borrowed = entries.total(TransactionType.BORROWED)
                PersonBalance(
                    // The most recent spelling of the name is the one shown.
                    name = entries.maxBy { it.timestamp }.counterparty(),
                    balance = lent - borrowed,
                    totalLent = lent,
                    totalBorrowed = borrowed,
                    entryCount = entries.size,
                    lastActivity = entries.maxOf { it.localDate }
                )
            }
            .sortedWith(
                compareBy<PersonBalance> { it.isSettled }
                    .thenByDescending { kotlin.math.abs(it.balance) }
                    .thenByDescending { it.lastActivity }
            )
    }

    /**
     * A handful of observations comparing [month] with the one before it, most useful first.
     * [formatAmount] turns a number into the user's currency text.
     */
    fun insights(
        all: List<Expense>,
        month: YearMonth,
        formatAmount: (Double) -> String
    ): List<Insight> {
        val current = summary(all, month)
        val previous = summary(all, month.minusMonths(1))
        val categories = categoryBreakdown(inMonth(all, month))
        val result = mutableListOf<Insight>()

        if (current.expense > 0 && previous.expense > 0) {
            val change = (current.expense - previous.expense) / previous.expense
            val percent = kotlin.math.abs(change * 100).toInt()
            if (percent >= 1) {
                result += if (change < 0) {
                    Insight(
                        title = "Spending down $percent%",
                        detail = "${formatAmount(previous.expense - current.expense)} less than last month.",
                        tone = Insight.Tone.POSITIVE
                    )
                } else {
                    Insight(
                        title = "Spending up $percent%",
                        detail = "${formatAmount(current.expense - previous.expense)} more than last month.",
                        tone = if (percent >= 20) Insight.Tone.WARNING else Insight.Tone.NEUTRAL
                    )
                }
            }
        }

        categories.firstOrNull()?.let { top ->
            result += Insight(
                title = "${top.category} leads at ${(top.share * 100).toInt()}%",
                detail = "${formatAmount(top.amount)} across ${top.count} " +
                        (if (top.count == 1) "entry" else "entries") + ".",
                tone = if (top.share >= 0.5f) Insight.Tone.WARNING else Insight.Tone.NEUTRAL
            )
        }

        current.savingsRate?.let { rate ->
            val percent = (rate * 100).toInt()
            result += when {
                rate >= 0.2f -> Insight(
                    "Saving $percent% of income",
                    "You kept ${formatAmount(current.net)} this month. Nicely done.",
                    Insight.Tone.POSITIVE
                )
                rate >= 0f -> Insight(
                    "Saving $percent% of income",
                    "Aim for 20% by trimming your top category.",
                    Insight.Tone.NEUTRAL
                )
                else -> Insight(
                    "Spending exceeds income",
                    "You're ${formatAmount(-current.net)} over what came in.",
                    Insight.Tone.WARNING
                )
            }
        }

        inMonth(all, month)
            .filter { it.type == TransactionType.EXPENSE }
            .maxByOrNull { it.amount }
            ?.let { biggest ->
                result += Insight(
                    title = "Biggest expense",
                    detail = "${biggest.title} · ${formatAmount(biggest.amount)} on ${biggest.date}.",
                    tone = Insight.Tone.NEUTRAL
                )
            }

        return result
    }

    /** Average spent per day: over days elapsed for the current month, the whole month otherwise. */
    fun averageDailySpend(
        all: List<Expense>,
        month: YearMonth,
        today: LocalDate = TransactionDates.today()
    ): Double {
        val spent = inMonth(all, month).total(TransactionType.EXPENSE)
        val days = when {
            month == YearMonth.from(today) -> today.dayOfMonth
            month.isAfter(YearMonth.from(today)) -> return 0.0
            else -> month.lengthOfMonth()
        }
        return if (days > 0) spent / days else 0.0
    }

    fun daysBetween(from: LocalDate, to: LocalDate): Long = ChronoUnit.DAYS.between(from, to)

    private fun List<Expense>.total(type: TransactionType): Double =
        filter { it.type == type }.sumOf { it.amount }

    private fun Expense.counterparty(): String =
        personName.trim().ifBlank { title.trim() }.ifBlank { "Someone" }
}