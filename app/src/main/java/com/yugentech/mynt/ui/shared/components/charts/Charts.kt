package com.yugentech.mynt.ui.shared.components.charts

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yugentech.mynt.transactions.insights.CategoryShare
import com.yugentech.mynt.transactions.insights.DayTotal
import com.yugentech.mynt.transactions.insights.MonthSummary
import com.yugentech.mynt.ui.shared.util.FinanceColors
import com.yugentech.mynt.ui.shared.util.chartColor
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/** Ring of category segments that sweeps in from the top whenever the data changes. */
@Composable
fun CategoryDonut(
    shares: List<CategoryShare>,
    modifier: Modifier = Modifier,
    size: Dp = 168.dp,
    strokeWidth: Dp = 22.dp,
    highlighted: String? = null,
    centerContent: @Composable () -> Unit = {}
) {
    val sweep = remember { Animatable(0f) }
    LaunchedEffect(shares) {
        sweep.snapTo(0f)
        sweep.animateTo(1f, tween(durationMillis = 750, easing = FastOutSlowInEasing))
    }
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    val colors = shares.mapIndexed { index, share -> chartColor(share.category, index) }

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(
            modifier = Modifier
                .size(size)
                .semantics {
                    contentDescription = shares.joinToString { "${it.category} ${(it.share * 100).toInt()}%" }
                }
        ) {
            val stroke = strokeWidth.toPx()
            val inset = stroke / 2 + 6.dp.toPx()
            val arcSize = Size(this.size.width - inset * 2, this.size.height - inset * 2)
            val topLeft = Offset(inset, inset)

            drawArc(track, 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke))

            if (shares.isEmpty()) return@Canvas
            val gap = if (shares.size > 1) 3f else 0f
            var start = -90f
            shares.forEachIndexed { index, share ->
                val full = share.share * 360f
                val visible = (full * sweep.value - gap).coerceAtLeast(0.5f)
                val isDimmed = highlighted != null && highlighted != share.category
                drawArc(
                    color = colors[index].copy(alpha = if (isDimmed) 0.3f else 1f),
                    startAngle = start + gap / 2,
                    sweepAngle = visible,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(
                        width = if (highlighted == share.category) stroke * 1.25f else stroke,
                        cap = StrokeCap.Butt
                    )
                )
                start += full * sweep.value
            }
        }
        centerContent()
    }
}

/**
 * Six months side by side: income and spending as paired bars, the selected month in
 * full colour and the rest softened. Tapping a month selects it.
 */
@Composable
fun MonthlyTrendChart(
    months: List<MonthSummary>,
    selected: YearMonth,
    onMonthSelected: (YearMonth) -> Unit,
    modifier: Modifier = Modifier,
    chartHeight: Dp = 150.dp
) {
    val maxValue = months.maxOfOrNull { maxOf(it.income, it.expense) }?.takeIf { it > 0 } ?: 1.0
    val incomeColor = FinanceColors.income
    val expenseColor = FinanceColors.expense
    val grid = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)

    Column(modifier = modifier) {
        Box(modifier = Modifier.fillMaxWidth().height(chartHeight)) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val dash = PathEffect.dashPathEffect(floatArrayOf(6f, 8f))
                listOf(0.25f, 0.5f, 0.75f).forEach { fraction ->
                    val y = size.height * (1 - fraction)
                    drawLine(grid, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx(), pathEffect = dash)
                }
            }
            Row(modifier = Modifier.matchParentSize(), horizontalArrangement = Arrangement.SpaceEvenly) {
                months.forEach { summary ->
                    val isSelected = summary.month == selected
                    val alpha = if (isSelected) 1f else 0.38f
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onMonthSelected(summary.month) },
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Bar(
                            fraction = (summary.income / maxValue).toFloat(),
                            color = incomeColor.copy(alpha = alpha)
                        )
                        Spacer(Modifier.width(4.dp))
                        Bar(
                            fraction = (summary.expense / maxValue).toFloat(),
                            color = expenseColor.copy(alpha = alpha)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            months.forEach { summary ->
                val isSelected = summary.month == selected
                Text(
                    text = summary.month.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    ),
                    color = if (isSelected) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun Bar(fraction: Float, color: Color) {
    // Starts from zero so bars grow in the first time they appear, not only on change.
    val animatable = remember { Animatable(0f) }
    LaunchedEffect(fraction) {
        animatable.animateTo(fraction.coerceIn(0f, 1f), tween(600, easing = FastOutSlowInEasing))
    }
    val animated = animatable.value
    Canvas(modifier = Modifier.width(12.dp).fillMaxHeight()) {
        val height = (size.height * animated).coerceAtLeast(if (fraction > 0f) 3.dp.toPx() else 0f)
        drawRoundRect(
            color = color,
            topLeft = Offset(0f, size.height - height),
            size = Size(size.width, height),
            cornerRadius = CornerRadius(size.width / 2, size.width / 2)
        )
    }
}

/**
 * Spending per day across the month, with a dashed line at the daily average and today
 * marked. Days still to come are left blank.
 */
@Composable
fun DailySpendChart(
    days: List<DayTotal>,
    averageDaily: Double,
    modifier: Modifier = Modifier,
    chartHeight: Dp = 110.dp,
    today: LocalDate = LocalDate.now()
) {
    val maxValue = days.maxOfOrNull { it.expense }?.takeIf { it > 0 } ?: 1.0
    val barColor = MaterialTheme.colorScheme.primary
    val todayColor = MaterialTheme.colorScheme.tertiary
    val muted = MaterialTheme.colorScheme.surfaceContainerHighest
    val averageColor = MaterialTheme.colorScheme.onSurfaceVariant

    val grow = remember { Animatable(0f) }
    LaunchedEffect(days.firstOrNull()?.date) {
        grow.snapTo(0f)
        grow.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
    }

    Column(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxWidth().height(chartHeight)) {
            if (days.isEmpty()) return@Canvas
            val slot = size.width / days.size
            val barWidth = (slot * 0.6f).coerceAtMost(10.dp.toPx())
            days.forEachIndexed { index, day ->
                val x = slot * index + (slot - barWidth) / 2
                val isFuture = day.date.isAfter(today)
                val fraction = (day.expense / maxValue).toFloat() * grow.value
                val height = if (day.expense > 0) (size.height * fraction).coerceAtLeast(3.dp.toPx())
                else 2.dp.toPx()
                drawRoundRect(
                    color = when {
                        isFuture || day.expense <= 0 -> muted
                        day.date == today -> todayColor
                        else -> barColor
                    },
                    topLeft = Offset(x, size.height - height),
                    size = Size(barWidth, height),
                    cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
                )
            }
            if (averageDaily > 0) {
                val y = size.height * (1 - (averageDaily / maxValue).toFloat().coerceIn(0f, 1f))
                drawLine(
                    color = averageColor.copy(alpha = 0.7f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp)) {
            val labels = listOf(1, 8, 15, 22, days.size).distinct()
            labels.forEachIndexed { i, day ->
                Text(
                    text = "$day",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = when (i) {
                        0 -> TextAlign.Start
                        labels.lastIndex -> TextAlign.End
                        else -> TextAlign.Center
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/** A rounded progress track that turns to the warning colour past [warnAt] and red past 100%. */
@Composable
fun BudgetProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 12.dp,
    warnAt: Float = 0.85f
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "budgetProgress"
    )
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    val color = when {
        progress > 1f -> FinanceColors.expense
        progress >= warnAt -> FinanceColors.lent
        else -> MaterialTheme.colorScheme.primary
    }
    Canvas(modifier = modifier.fillMaxWidth().height(height)) {
        val radius = CornerRadius(size.height / 2, size.height / 2)
        drawRoundRect(track, cornerRadius = radius)
        if (animated > 0f) {
            drawRoundRect(
                color = color,
                size = Size((size.width * animated).coerceAtLeast(size.height), size.height),
                cornerRadius = radius
            )
        }
    }
}
