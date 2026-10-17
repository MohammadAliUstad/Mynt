package com.yugentech.mynt.transactions.export

import com.yugentech.mynt.database.model.Expense
import com.yugentech.mynt.database.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Locale

class CsvExporterTest {

    @Before
    fun setUp() {
        Locale.setDefault(Locale.US)
    }

    @Test
    fun `plain values pass through untouched`() {
        assertEquals("Lunch", CsvExporter.escape("Lunch"))
    }

    @Test
    fun `commas quotes and newlines are quoted per RFC 4180`() {
        assertEquals("\"Rent, Oct\"", CsvExporter.escape("Rent, Oct"))
        assertEquals("\"Say \"\"hi\"\"\"", CsvExporter.escape("Say \"hi\""))
        assertEquals("\"two\nlines\"", CsvExporter.escape("two\nlines"))
    }

    @Test
    fun `formula-looking text is defused but numbers are not`() {
        assertEquals("'=SUM(A1)", CsvExporter.escape("=SUM(A1)"))
        assertEquals("'@cmd", CsvExporter.escape("@cmd"))
        assertEquals("-12.50", CsvExporter.escape("-12.50"))
    }

    @Test
    fun `rows use ISO dates and plain amounts`() {
        val csv = CsvExporter.export(
            listOf(
                Expense("1", "Coffee, large", 120.5, "Food", "05 Oct 2026", "October 2026", TransactionType.EXPENSE)
            ),
            currencySymbol = "₹"
        )
        val lines = csv.trim().lines()
        assertEquals("Date,Type,Title,Category,Person,Amount,Currency", lines[0])
        assertEquals("2026-10-05,Expense,\"Coffee, large\",Food,,120.50,₹", lines[1])
    }
}
