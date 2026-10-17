package com.yugentech.mynt.ui.tabs.statsScreen.parent

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yugentech.mynt.database.model.Expense
import com.yugentech.mynt.transactions.insights.CategoryShare
import com.yugentech.mynt.transactions.insights.FinanceInsights
import com.yugentech.mynt.ui.shared.components.AnimatedAmountText
import com.yugentech.mynt.ui.shared.components.CategoryBadge
import com.yugentech.mynt.ui.shared.components.EmptyState
import com.yugentech.mynt.ui.shared.components.MonthSwitcher
import com.yugentech.mynt.ui.shared.components.MyntCard
import com.yugentech.mynt.ui.shared.components.SectionHeader
import com.yugentech.mynt.ui.shared.components.charts.CategoryDonut
import com.yugentech.mynt.ui.shared.components.charts.DailySpendChart
import com.yugentech.mynt.ui.shared.components.charts.MonthlyTrendChart
import com.yugentech.mynt.ui.shared.components.monthTitle
import com.yugentech.mynt.ui.shared.util.FinanceColors
import com.yugentech.mynt.ui.shared.util.LocalCurrencySymbol
import com.yugentech.mynt.ui.shared.util.categoryStyle
import com.yugentech.mynt.ui.shared.util.chartColor
import com.yugentech.mynt.ui.shared.util.formatAmountShort
import com.yugentech.mynt.ui.tabs.homeScreen.parent.InsightCard
import java.time.YearMonth

/** A month taken apart: where the money went, how each day looked, and how it compares. */
@Composable
fun StatsScreen(
    expenses: List<Expense>,
    bottomPadding: Dp
) {
    var month by rememberSaveable { mutableStateOf(YearMonth.now()) }
    var highlighted by remember { mutableStateOf<String?>(null) }
    val currency = LocalCurrencySymbol.current

    val summary = remember(expenses, month) { FinanceInsights.summary(expenses, month) }
    val categories = remember(expenses, month) {
        FinanceInsights.categoryBreakdown(FinanceInsights.inMonth(expenses, month))
    }
    val daily = remember(expenses, month) { FinanceInsights.dailyTotals(expenses, month) }
    val averageDaily = remember(expenses, month) { FinanceInsights.averageDailySpend(expenses, month) }
    val trend = remember(expenses, month) {
        // The window stays on the latest six months, and only slides back once you pick an older one.
        val latest = YearMonth.now()
        val end = if (month.isBefore(latest.minusMonths(5))) month.plusMonths(5) else latest
        FinanceInsights.monthlyTrend(expenses, end, 6)
    }
    val insights = remember(expenses, month, currency) {
        FinanceInsights.insights(expenses, month) { formatAmountShort(it, currency) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
            .padding(bottom = bottomPadding)
    ) {
        Spacer(Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Insights",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
            MonthSwitcher(month = month, onMonthChange = {
                month = it
                highlighted = null
            })
        }

        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SummaryTile("Income", summary.income, FinanceColors.income, Modifier.weight(1f))
            SummaryTile("Spent", summary.expense, FinanceColors.expense, Modifier.weight(1f))
            SummaryTile(
                label = if (summary.net >= 0) "Saved" else "Overspent",
                amount = kotlin.math.abs(summary.net),
                color = if (summary.net >= 0) MaterialTheme.colorScheme.primary else FinanceColors.expense,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(20.dp))

        if (summary.transactionCount == 0) {
            MyntCard(modifier = Modifier.fillMaxWidth()) {
                EmptyState(
                    icon = Icons.Rounded.Insights,
                    title = "No entries in ${monthTitle(month)}",
                    message = "Log a few expenses and Mynt will show where your money goes."
                )
            }
        } else {
            if (categories.isNotEmpty()) {
                SectionHeader(title = "Where it went")
                Spacer(Modifier.height(8.dp))
                MyntCard(modifier = Modifier.fillMaxWidth().animateContentSize()) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        val focused = categories.firstOrNull { it.category == highlighted }
                        CategoryDonut(shares = categories, highlighted = highlighted) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = focused?.category ?: "Spent",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                AnimatedAmountText(
                                    amount = focused?.amount ?: summary.expense,
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (focused != null) {
                                    Text(
                                        "${(focused.share * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    categories.forEachIndexed { index, share ->
                        CategoryLegendRow(
                            share = share,
                            color = chartColor(share.category, index),
                            isHighlighted = highlighted == share.category,
                            onClick = {
                                highlighted = if (highlighted == share.category) null else share.category
                            }
                        )
                        if (index < categories.lastIndex) Spacer(Modifier.height(6.dp))
                    }
                }

                Spacer(Modifier.height(20.dp))
            }

            SectionHeader(title = "Daily spending")
            Spacer(Modifier.height(8.dp))
            MyntCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Average per day",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        AnimatedAmountText(
                            amount = averageDaily,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    val busiest = daily.maxByOrNull { it.expense }
                    if (busiest != null && busiest.expense > 0) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "Busiest day",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "${busiest.date.dayOfMonth} · ${formatAmountShort(busiest.expense)}",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                DailySpendChart(days = daily, averageDaily = averageDaily)
            }

            Spacer(Modifier.height(20.dp))
        }

        SectionHeader(title = "Last 6 months")
        Spacer(Modifier.height(8.dp))
        MyntCard(modifier = Modifier.fillMaxWidth()) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LegendDot("Income", FinanceColors.income)
                LegendDot("Spent", FinanceColors.expense)
            }
            Spacer(Modifier.height(16.dp))
            MonthlyTrendChart(
                months = trend,
                selected = month,
                onMonthSelected = { month = it }
            )
        }

        if (insights.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            SectionHeader(title = "What stands out")
            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                insights.forEach { InsightCard(insight = it, modifier = Modifier.fillMaxWidth()) }
            }
        }
    }
}

@Composable
private fun SummaryTile(label: String, amount: Double, color: Color, modifier: Modifier = Modifier) {
    MyntCard(modifier = modifier, contentPadding = 14.dp, shape = RoundedCornerShape(22.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(2.dp))
        AnimatedAmountText(
            amount = amount,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = color
        )
    }
}

@Composable
private fun CategoryLegendRow(
    share: CategoryShare,
    color: Color,
    isHighlighted: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isHighlighted) color.copy(alpha = 0.1f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CategoryBadge(style = categoryStyle(share.category), size = 38.dp, shape = RoundedCornerShape(12.dp))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    share.category,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    formatAmountShort(share.amount),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(
                    progress = { share.share },
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp),
                    color = color,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    strokeCap = StrokeCap.Round,
                    gapSize = 0.dp,
                    drawStopIndicator = {}
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    "${(share.share * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LegendDot(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
