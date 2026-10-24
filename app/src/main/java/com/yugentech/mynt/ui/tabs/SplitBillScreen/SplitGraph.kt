package com.yugentech.mynt.ui.split.parent

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.yugentech.mynt.split.model.SplitBill
import com.yugentech.mynt.ui.shared.util.LocalCurrencySymbol
import com.yugentech.mynt.ui.shared.util.formatAmountShort

@Composable
internal fun SplitGraph(
    people: List<String>,
    bills: List<SplitBill>
) {
    val currency = LocalCurrencySymbol.current
    var isEqualized by remember { mutableStateOf(false) }
    val textMeasurer = rememberTextMeasurer()

    // 1. Math calculations
    // How much each person actually paid in total
    val actualContributions = people.associateWith { person ->
        bills.filter { it.paidBy.equals(person, ignoreCase = true) }.sumOf { it.amount }
    }

    // Each person's fair share across all bills
    val fairShares = people.associateWith { person ->
        bills.sumOf { bill ->
            bill.shares.entries.firstOrNull { it.key.equals(person, ignoreCase = true) }?.value ?: 0.0
        }
    }

    // Determine the maximum value so the graph scale stays locked during animations
    val maxContribution = maxOf(
        actualContributions.values.maxOrNull() ?: 0.0,
        fairShares.values.maxOrNull() ?: 0.0
    )

    // 2. Animate the values smoothly when tapped
    val animatedValues = people.associateWith { person ->
        val target = if (isEqualized) (fairShares[person] ?: 0.0) else (actualContributions[person] ?: 0.0)
        animateFloatAsState(
            targetValue = target.toFloat(),
            animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
            label = "bar_anim_$person"
        ).value
    }

    // 3. Theming
    val barColor = MaterialTheme.colorScheme.primary
    val axisColor = MaterialTheme.colorScheme.onSurface
    val textColor = MaterialTheme.colorScheme.onSurface
    val nameStyle = MaterialTheme.typography.labelLarge
    val valueStyle = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
    val axisLabelStyle = MaterialTheme.typography.bodyMedium

    val interactionSource = remember { MutableInteractionSource() }

    // Scale graph height dynamically so everyone fits without getting squished
    val graphHeight = maxOf(200.dp, (people.size * 56 + 60).dp)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(graphHeight)
            .padding(vertical = 12.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null // Removes the grey ripple so the animation is the focus
            ) {
                isEqualized = !isEqualized
            }
    ) {
        val width = size.width
        val height = size.height

        // Sizing constraints
        val nameAreaWidth = 80.dp.toPx()
        val valueAreaWidth = 80.dp.toPx()
        val maxBarWidth = (width - nameAreaWidth - valueAreaWidth - 16.dp.toPx()).coerceAtLeast(0f)

        // Draw Y Axis (Straight vertical line)
        drawLine(
            color = axisColor,
            start = Offset(nameAreaWidth, 0f),
            end = Offset(nameAreaWidth, height - 24.dp.toPx()),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Draw X Axis Bottom Label
        val labelText = if (isEqualized) "Fair Share" else "Contribution"
        val labelLayout = textMeasurer.measure(labelText, style = axisLabelStyle)
        drawText(
            textLayoutResult = labelLayout,
            color = textColor,
            topLeft = Offset(
                x = nameAreaWidth + (maxBarWidth / 2f) - (labelLayout.size.width / 2f),
                y = height - labelLayout.size.height.toFloat()
            )
        )

        val barHeight = 24.dp.toPx()
        val totalBarSpace = height - 32.dp.toPx()
        val count = maxOf(1, people.size)
        val verticalSpacing = (totalBarSpace - (people.size * barHeight)) / (count + 1)

        // Draw Bars & Text
        people.forEachIndexed { index, person ->
            val currentValue = animatedValues[person] ?: 0f
            val barWidth = if (maxContribution > 0) {
                (currentValue / maxContribution.toFloat()) * maxBarWidth
            } else 0f

            val yOffset = verticalSpacing + (index * (barHeight + verticalSpacing))

            // Draw Person's Name
            val maxNameWidth = (nameAreaWidth - 12.dp.toPx()).toInt().coerceAtLeast(1)
            val nameLayout = textMeasurer.measure(
                text = person,
                style = nameStyle,
                overflow = TextOverflow.Ellipsis,
                constraints = Constraints(maxWidth = maxNameWidth)
            )
            drawText(
                textLayoutResult = nameLayout,
                color = textColor,
                topLeft = Offset(
                    x = (nameAreaWidth - 12.dp.toPx() - nameLayout.size.width).coerceAtLeast(0f),
                    y = yOffset + (barHeight / 2f) - (nameLayout.size.height / 2f)
                )
            )

            // Draw Animated Bar
            if (barWidth > 0f) {
                drawRect(
                    color = barColor,
                    topLeft = Offset(nameAreaWidth, yOffset),
                    size = Size(barWidth, barHeight)
                )
            }

            // Draw Animated Value
            val valueLayout = textMeasurer.measure(
                text = formatAmountShort(currentValue.toDouble(), currency),
                style = valueStyle
            )
            val valueX = (nameAreaWidth + barWidth + 8.dp.toPx())
                .coerceAtMost(width - valueLayout.size.width)
            drawText(
                textLayoutResult = valueLayout,
                color = textColor,
                topLeft = Offset(
                    x = valueX,
                    y = yOffset + (barHeight / 2f) - (valueLayout.size.height / 2f)
                )
            )
        }
    }
}