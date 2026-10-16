package com.yourname.expensetracker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yourname.expensetracker.ui.theme.ExpenseTrackerTheme
import kotlin.math.abs

private val IncomeGreen = Color(0xFF4CAF50)
private val ExpenseRed = Color(0xFFEF5350)

/**
 * Home Overview card: title, Income/Expense totals on the left,
 * big donut chart with the Net amount inside on the right.
 */
@Composable
fun HomeOverviewCard(
    totalIncome: Double,
    totalExpense: Double,
    modifier: Modifier = Modifier
) {
    val net = totalIncome - totalExpense

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "Overview",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    OverviewStat(label = "Income", amount = totalIncome, barColor = IncomeGreen)
                    Spacer(modifier = Modifier.height(18.dp))
                    OverviewStat(label = "Expenses", amount = totalExpense, barColor = ExpenseRed)
                }

                Spacer(modifier = Modifier.width(12.dp))

                DonutChart(
                    income = totalIncome,
                    expense = totalExpense,
                    modifier = Modifier.padding(8.dp),
                    size = 112.dp,
                    strokeWidth = 14.dp
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Net",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                        Text(
                            text = (if (net >= 0) "+" else "-") + SettingsManager.formatAmount(abs(net)),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            ),
                            color = if (net >= 0) IncomeGreen else ExpenseRed,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OverviewStat(label: String, amount: Double, barColor: Color) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(16.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(barColor)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = SettingsManager.formatAmount(amount),
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(showBackground = true, name = "Home Overview Card Preview")
@Composable
fun HomeOverviewCardPreview() {
    ExpenseTrackerTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            HomeOverviewCard(totalIncome = 65000.0, totalExpense = 41800.0)
        }
    }
}