package com.yugentech.mynt.transactions.insights

import com.yugentech.mynt.database.model.Expense
import com.yugentech.mynt.database.model.SETTLEMENT_CATEGORY
import com.yugentech.mynt.database.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

class FinanceInsightsTest {

    private var nextId = 0

    @Before
    fun setUp() {
        Locale.setDefault(Locale.US)
    }

    private fun entry(
        amount: Double,
        date: LocalDate,
        type: TransactionType = TransactionType.EXPENSE,
        category: String = "Food",
        person: String = "",
        timestamp: Long = nextId.toLong()
    ) = Expense(
        id = "id-${nextId++}",
        title = category,
        amount = amount,
        category = category,
        date = "",
        monthYear = "",
        type = type,
        personName = person,
        timestamp = timestamp
    ).onDate(date)

    private val oct = YearMonth.of(2026, 10)

    @Test
    fun `stored date strings round-trip to the same day and month`() {
        val e = entry(10.0, LocalDate.of(2026, 10, 5))
        assertEquals("05 Oct 2026", e.date)
        assertEquals("October 2026", e.monthYear)
        assertEquals(LocalDate.of(2026, 10, 5), e.localDate)
        assertEquals(oct, e.yearMonth)
    }

    @Test
    fun `old single-digit dates still parse`() {
        val e = Expense("x", "t", 1.0, "Food", date = "5 Oct 2026", monthYear = "October 2026")
        assertEquals(LocalDate.of(2026, 10, 5), e.localDate)
    }

    @Test
    fun `unparseable date falls back to the month`() {
        val e = Expense("x", "t", 1.0, "Food", date = "garbage", monthYear = "October 2026")
        assertEquals(LocalDate.of(2026, 10, 1), e.localDate)
        assertEquals(oct, e.yearMonth)
    }

    @Test
    fun `summary only counts the requested month and separates types`() {
        val all = listOf(
            entry(100.0, LocalDate.of(2026, 10, 1)),
            entry(50.0, LocalDate.of(2026, 10, 2)),
            entry(1000.0, LocalDate.of(2026, 10, 3), TransactionType.INCOME, "Salary"),
            entry(200.0, LocalDate.of(2026, 10, 4), TransactionType.LENT, person = "Asha"),
            entry(999.0, LocalDate.of(2026, 9, 30))
        )
        val s = FinanceInsights.summary(all, oct)
        assertEquals(150.0, s.expense, 0.001)
        assertEquals(1000.0, s.income, 0.001)
        assertEquals(200.0, s.lent, 0.001)
        assertEquals(850.0, s.net, 0.001)
        assertEquals(0.85f, s.savingsRate!!, 0.001f)
        assertEquals(4, s.transactionCount)
    }

    @Test
    fun `savings rate is undefined without income`() {
        val s = FinanceInsights.summary(listOf(entry(10.0, LocalDate.of(2026, 10, 1))), oct)
        assertNull(s.savingsRate)
    }

    @Test
    fun `category breakdown is sorted and shares add to one`() {
        val day = LocalDate.of(2026, 10, 1)
        val shares = FinanceInsights.categoryBreakdown(
            listOf(
                entry(300.0, day, category = "Food"),
                entry(100.0, day, category = "Travel"),
                entry(100.0, day, category = "Food"),
                entry(5000.0, day, TransactionType.INCOME, "Salary")
            )
        )
        assertEquals(listOf("Food", "Travel"), shares.map { it.category })
        assertEquals(0.8f, shares[0].share, 0.001f)
        assertEquals(2, shares[0].count)
        assertEquals(1f, shares.sumOf { it.share.toDouble() }.toFloat(), 0.001f)
    }

    @Test
    fun `daily totals cover every day of the month`() {
        val days = FinanceInsights.dailyTotals(listOf(entry(40.0, LocalDate.of(2026, 10, 15))), oct)
        assertEquals(31, days.size)
        assertEquals(40.0, days[14].expense, 0.001)
        assertEquals(0.0, days[0].expense, 0.001)
    }

    @Test
    fun `budget mid-month gives remaining, daily allowance and projection`() {
        val today = LocalDate.of(2026, 10, 11) // 21 days left including today
        val all = listOf(entry(5000.0, LocalDate.of(2026, 10, 5)))
        val status = FinanceInsights.budgetStatus(all, oct, budget = 26000.0, today = today)!!
        assertEquals(21000.0, status.remaining, 0.001)
        assertEquals(21, status.daysLeft)
        assertEquals(1000.0, status.dailyAllowance, 0.001)
        // 5000 over 11 days, carried across 31
        assertEquals(5000.0 / 11 * 31, status.projectedSpend, 0.001)
        assertTrue(!status.isOver)
    }

    @Test
    fun `over budget never suggests a negative allowance`() {
        val today = LocalDate.of(2026, 10, 20)
        val status = FinanceInsights.budgetStatus(listOf(entry(1200.0, today)), oct, 1000.0, today)!!
        assertTrue(status.isOver)
        assertEquals(0.0, status.dailyAllowance, 0.001)
    }

    @Test
    fun `no budget means no status`() {
        assertNull(FinanceInsights.budgetStatus(emptyList(), oct, 0.0))
    }

    @Test
    fun `past month budget has no days left`() {
        val status = FinanceInsights.budgetStatus(emptyList(), YearMonth.of(2026, 9), 1000.0, LocalDate.of(2026, 10, 2))!!
        assertEquals(0, status.daysLeft)
        assertTrue(!status.isCurrentMonth)
    }

    @Test
    fun `streak counts consecutive days and survives an empty today`() {
        val today = LocalDate.of(2026, 10, 17)
        val all = listOf(
            entry(1.0, today.minusDays(1)),
            entry(1.0, today.minusDays(2)),
            entry(1.0, today.minusDays(3)),
            entry(1.0, today.minusDays(5))
        )
        assertEquals(3, FinanceInsights.loggingStreak(all, today))
        assertEquals(4, FinanceInsights.loggingStreak(all + entry(1.0, today), today))
        assertEquals(0, FinanceInsights.loggingStreak(listOf(entry(1.0, today.minusDays(2))), today))
    }

    @Test
    fun `person balances net lending against borrowing, case-insensitively`() {
        val day = LocalDate.of(2026, 10, 1)
        val balances = FinanceInsights.personBalances(
            listOf(
                entry(500.0, day, TransactionType.LENT, LENDING, "Rahul"),
                entry(200.0, day, TransactionType.BORROWED, BORROWING, "rahul"),
                entry(300.0, day, TransactionType.BORROWED, BORROWING, "Meera"),
                entry(999.0, day) // a plain expense is ignored
            )
        )
        assertEquals(2, balances.size)
        val rahul = balances.first { it.name.equals("rahul", ignoreCase = true) }
        assertEquals(300.0, rahul.balance, 0.001)
        val meera = balances.first { it.name == "Meera" }
        assertEquals(-300.0, meera.balance, 0.001)
    }

    @Test
    fun `a settlement entry brings a balance to zero`() {
        val day = LocalDate.of(2026, 10, 1)
        val loan = entry(750.0, day, TransactionType.LENT, LENDING, "Kiran")
        val payback = entry(750.0, day.plusDays(3), TransactionType.BORROWED, SETTLEMENT_CATEGORY, "Kiran")
        val balance = FinanceInsights.personBalances(listOf(loan, payback)).single()
        assertTrue(balance.isSettled)
        assertEquals(2, balance.entryCount)
    }

    @Test
    fun `settled people sort after those with money outstanding`() {
        val day = LocalDate.of(2026, 10, 1)
        val balances = FinanceInsights.personBalances(
            listOf(
                entry(100.0, day, TransactionType.LENT, LENDING, "Done"),
                entry(100.0, day, TransactionType.BORROWED, SETTLEMENT_CATEGORY, "Done"),
                entry(50.0, day, TransactionType.LENT, LENDING, "Open")
            )
        )
        assertEquals(listOf("Open", "Done"), balances.map { it.name })
    }

    @Test
    fun `sorting puts newest dates first and breaks ties by logging time`() {
        val a = entry(1.0, LocalDate.of(2026, 10, 2), timestamp = 1)
        val b = entry(1.0, LocalDate.of(2026, 10, 2), timestamp = 2)
        val c = entry(1.0, LocalDate.of(2026, 10, 9), timestamp = 0)
        assertEquals(listOf(c, b, a), listOf(a, b, c).sortedByDateDescending())
    }

    @Test
    fun `trend returns the requested months oldest first`() {
        val trend = FinanceInsights.monthlyTrend(emptyList(), oct, 6)
        assertEquals(YearMonth.of(2026, 5), trend.first().month)
        assertEquals(oct, trend.last().month)
        assertEquals(6, trend.size)
    }

    @Test
    fun `insights call out a drop in spending`() {
        val all = listOf(
            entry(1000.0, LocalDate.of(2026, 9, 10)),
            entry(600.0, LocalDate.of(2026, 10, 10))
        )
        val insights = FinanceInsights.insights(all, oct) { "₹${it.toInt()}" }
        val first = insights.first()
        assertEquals("Spending down 40%", first.title)
        assertEquals(Insight.Tone.POSITIVE, first.tone)
    }

    @Test
    fun `average daily spend uses days elapsed in the current month`() {
        val today = LocalDate.of(2026, 10, 10)
        val avg = FinanceInsights.averageDailySpend(listOf(entry(500.0, today)), oct, today)
        assertEquals(50.0, avg, 0.001)
    }

    private companion object {
        const val LENDING = "Lending"
        const val BORROWING = "Borrowing"
    }
}
