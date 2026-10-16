package com.yourname.expensetracker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LentBorrowedScreen(
    expenses: List<Expense>,
    onBackClick: () -> Unit,
    onTransactionClick: (Expense) -> Unit
) {
    var selectedCalendar by remember { mutableStateOf(Calendar.getInstance()) }
    var showMonthPicker by remember { mutableStateOf(false) }

    val monthYearFormatter = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
    val currentMonthYear = monthYearFormatter.format(selectedCalendar.time)

    // Filter transactions for Lent and Borrowed in the selected month
    val lentList = expenses.filter { it.monthYear.equals(currentMonthYear, true) && it.type == TransactionType.LENT }
    val borrowedList = expenses.filter { it.monthYear.equals(currentMonthYear, true) && it.type == TransactionType.BORROWED }

    val totalLent = lentList.sumOf { it.amount }
    val totalBorrowed = borrowedList.sumOf { it.amount }
    val netBalance = totalLent - totalBorrowed

    val combinedList = (lentList + borrowedList).sortedByDescending { it.date }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lent & Borrowed", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Month Selector Header
            MonthYearSelectorButton(
                selectedCalendar = selectedCalendar,
                onClick = { showMonthPicker = true }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Overview Card with Donut Chart and Totals
            HomeOverviewCard(
                totalIncome = totalLent,
                totalExpense = totalBorrowed
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Transactions",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (combinedList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No lent or borrowed transactions for this month.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(combinedList) { tx ->
                        TransactionItemRow(transaction = tx, onClick = { onTransactionClick(tx) })
                    }
                }
            }
        }

        if (showMonthPicker) {
            MonthYearPickerDialog(
                selectedCalendar = selectedCalendar,
                onMonthYearSelected = {
                    selectedCalendar = it
                    showMonthPicker = false
                },
                onDismissRequest = { showMonthPicker = false }
            )
        }
    }
}

@Composable
fun TransactionItemRow(transaction: Expense, onClick: () -> Unit) {
    val isLent = transaction.type == TransactionType.LENT
    val amountColor = if (isLent) Color(0xFF43A047) else Color(0xFFE53935)
    val sign = if (isLent) "+" else "-"

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isLent) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = if (isLent) "↗️" else "↙️", fontSize = 18.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (transaction.personName.isNotBlank()) transaction.personName else transaction.title,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${transaction.category} • ${transaction.date}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
            Text(
                text = "$sign₹${String.format(Locale.getDefault(), "%,.0f", transaction.amount)}",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = amountColor
            )
        }
    }
}