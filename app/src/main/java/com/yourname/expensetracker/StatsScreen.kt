package com.yourname.expensetracker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yourname.expensetracker.ui.theme.ExpenseTrackerTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

/**
 * Data model for category expense breakdown item.
 */
data class CategoryStat(
    val category: String,
    val amount: Double,
    val percentage: Float
)

/**
 * Stats Screen showing monthly total income, expenses, net balance,
 * and horizontal category breakdown progress bars.
 */
@Composable
fun StatsScreen(
    expenses: List<Expense> = emptyList()
) {
    // Month navigation calendar state
    var calendarState by remember { mutableStateOf(Calendar.getInstance()) }

    // Month formatter (e.g. "October 2026")
    val monthYearFormatter = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
    val currentMonthYear = monthYearFormatter.format(calendarState.time)

    // Calculate totals for selected month
    val monthExpenses = expenses.filter {
        it.monthYear.equals(currentMonthYear, ignoreCase = true) && it.type == TransactionType.EXPENSE
    }
    val monthIncomes = expenses.filter {
        it.monthYear.equals(currentMonthYear, ignoreCase = true) && it.type == TransactionType.INCOME
    }

    val totalExpenses = monthExpenses.sumOf { it.amount }
    val totalIncome = monthIncomes.sumOf { it.amount }
    val netSavings = totalIncome - totalExpenses

    // Group expenses by category
    val categoryBreakdown = if (totalExpenses > 0) {
        monthExpenses
            .groupBy { it.category }
            .map { (cat, list) ->
                val sum = list.sumOf { it.amount }
                CategoryStat(
                    category = cat,
                    amount = sum,
                    percentage = (sum / totalExpenses).toFloat()
                )
            }
            .sortedByDescending { it.amount }
    } else {
        emptyList()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Month Selector Header
            Row(modifier = Modifier.fillMaxWidth()) {
                MonthYearSelectorButton(
                    selectedCalendar = calendarState,
                    onClick = {
                        // Month picker can also be opened from Stats if needed
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Summary Cards (Income, Expenses, Net Savings)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Total Income Card
                StatSummaryCard(
                    title = "Income",
                    amount = totalIncome,
                    containerColor = Color(0xFFE8F5E9), // Soft Green
                    textColor = Color(0xFF2E7D32),
                    modifier = Modifier.weight(1f)
                )

                // Total Expense Card
                StatSummaryCard(
                    title = "Expenses",
                    amount = totalExpenses,
                    containerColor = Color(0xFFFFEBEE), // Soft Red
                    textColor = Color(0xFFC62828),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Net Savings Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Net Savings",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    val balancePrefix = if (netSavings >= 0) "+" else "-"
                    val formattedSavings = String.format(
                        Locale.getDefault(),
                        "%s%s",
                        balancePrefix,
                        SettingsManager.formatAmount(abs(netSavings))
                    )

                    Text(
                        text = formattedSavings,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Category Breakdown Title
            Text(
                text = "Expense Categories",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Breakdown List or Empty State
            if (categoryBreakdown.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(text = "📊", fontSize = 36.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No category data for this month",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Add expenses to see statistics breakdown",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(bottom = 110.dp) // Space for floating bottom nav bar
                ) {
                    items(categoryBreakdown) { stat ->
                        CategoryStatRow(stat = stat)
                    }
                }
            }
        }
    }
}

/**
 * Summary card for Income or Expenses.
 */
@Composable
private fun StatSummaryCard(
    title: String,
    amount: Double,
    containerColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = textColor.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            val formatted = SettingsManager.formatAmount(amount)
            Text(
                text = formatted,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                ),
                color = textColor
            )
        }
    }
}

/**
 * Row displaying category icon, category name, percentage, amount, and horizontal progress bar.
 */
@Composable
private fun CategoryStatRow(stat: CategoryStat) {
    val (icon, bgColor) = getCategoryIconAndColor(stat.category, TransactionType.EXPENSE)
    val percentageText = String.format(Locale.getDefault(), "%.0f%%", stat.percentage * 100)
    val formattedAmount = SettingsManager.formatAmount(stat.amount)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Icon Badge
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(bgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = icon,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Category Name
                Text(
                    text = stat.category,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                // Percentage and Amount
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formattedAmount,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = percentageText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Horizontal Progress Bar
            LinearProgressIndicator(
                progress = { stat.percentage },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Preview(showBackground = true, name = "Stats Screen Preview")
@Composable
fun StatsScreenPreview() {
    ExpenseTrackerTheme {
        StatsScreen(expenses = emptyList())
    }
}
