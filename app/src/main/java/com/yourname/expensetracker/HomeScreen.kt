package com.yourname.expensetracker

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yourname.expensetracker.ui.theme.ExpenseTrackerTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Redesigned Home Screen featuring Month/Year Selector, Overview Card with Donut Chart,
 * and Recent Transactions list with Statement Import option.
 */
@Composable
fun HomeScreen(
    expenses: List<Expense> = emptyList()
) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Selected Calendar month & year state (defaults to current month)
    var selectedCalendar by remember { mutableStateOf(Calendar.getInstance()) }

    // Dialog state for Month/Year Picker
    var showMonthPicker by remember { mutableStateOf(false) }

    // Date formatter for matching monthYear string (e.g., "October 2026")
    val monthYearFormatter = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
    val currentMonthYear = monthYearFormatter.format(selectedCalendar.time)

    // Calculate totals and filtered transactions for the selected month
    val monthExpenses = expenses.filter {
        it.monthYear.equals(currentMonthYear, ignoreCase = true) && it.type == TransactionType.EXPENSE
    }
    val monthIncomes = expenses.filter {
        it.monthYear.equals(currentMonthYear, ignoreCase = true) && it.type == TransactionType.INCOME
    }

    val totalSpent = monthExpenses.sumOf { it.amount }
    val totalEarned = monthIncomes.sumOf { it.amount }

    // Combined recent transactions for selected month (newest first)
    val monthTransactions = expenses.filter {
        it.monthYear.equals(currentMonthYear, ignoreCase = true)
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    actionColor = MaterialTheme.colorScheme.primary,
                    dismissActionContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 110.dp) // Ensures last item is not hidden by floating bottom nav bar
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // 1. TOP MONTH/YEAR SELECTOR BUTTON (Placed on the LEFT)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MonthYearSelectorButton(
                    selectedCalendar = selectedCalendar,
                    onClick = { showMonthPicker = true }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2. OVERVIEW SECTION TITLE
            Text(
                text = "Overview",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 3. OVERVIEW CARD (Income/Expense totals, Donut Chart, Net Balance pill)
            OverviewCard(
                totalIncome = totalEarned,
                totalExpense = totalSpent
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 4. RECENT TRANSACTIONS SECTION TITLE
            Text(
                text = "Recent Transactions",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 5. RECENT TRANSACTIONS CARD
            RecentTransactionsCard(
                transactions = monthTransactions,
                onImportFileSelected = { fileName ->
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("$fileName: Import is coming soon")
                    }
                }
            )
        }

        // MONTH/YEAR PICKER DIALOG
        if (showMonthPicker) {
            MonthYearPickerDialog(
                selectedCalendar = selectedCalendar,
                onMonthYearSelected = { newCalendar ->
                    selectedCalendar = newCalendar
                    showMonthPicker = false
                },
                onDismissRequest = { showMonthPicker = false }
            )
        }
    }
}

@Preview(showBackground = true, name = "Home Screen Redesign Preview")
@Composable
fun HomeScreenPreview() {
    ExpenseTrackerTheme {
        HomeScreen(expenses = emptyList())
    }
}
