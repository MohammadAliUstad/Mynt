package com.yourname.expensetracker

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yourname.expensetracker.ui.theme.ExpenseTrackerTheme

/**
 * Donut (ring) chart drawn with Compose Canvas comparing Income and Expense.
 */
@Composable
fun DonutChart(
    income: Double,
    expense: Double,
    modifier: Modifier = Modifier
) {
    val total = income + expense
    val incomeRatio = if (total > 0) (income / total).toFloat() else 0f
    val expenseRatio = if (total > 0) (expense / total).toFloat() else 0f

    // Animated target sweep angles
    val animatedIncomeRatio by animateFloatAsState(
        targetValue = incomeRatio,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "incomeRatioAnimation"
    )

    val animatedExpenseRatio by animateFloatAsState(
        targetValue = expenseRatio,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "expenseRatioAnimation"
    )

    val incomeColor = Color(0xFF4CAF50)  // Soft Green
    val expenseColor = Color(0xFFEF5350) // Soft Red
    val emptyColor = Color(0xFFE0E0E0)   // Light Gray

    Box(
        modifier = modifier.size(86.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(86.dp)) {
            val strokeWidth = 14.dp.toPx()

            if (total == 0.0) {
                // Draw empty gray ring when both income and expense are 0
                drawArc(
                    color = emptyColor,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth)
                )
            } else {
                val gapDegree = 6f
                val incomeAngle = (animatedIncomeRatio * 360f - gapDegree).coerceAtLeast(0f)
                val expenseAngle = (animatedExpenseRatio * 360f - gapDegree).coerceAtLeast(0f)

                var startAngle = -90f

                // Draw Income arc
                if (incomeAngle > 0f) {
                    drawArc(
                        color = incomeColor,
                        startAngle = startAngle,
                        sweepAngle = incomeAngle,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                startAngle += (animatedIncomeRatio * 360f)

                // Draw Expense arc
                if (expenseAngle > 0f) {
                    drawArc(
                        color = expenseColor,
                        startAngle = startAngle,
                        sweepAngle = expenseAngle,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Donut Chart Preview")
@Composable
fun DonutChartPreview() {
    ExpenseTrackerTheme {
        DonutChart(income = 50000.0, expense = 12500.0)
    }
}
