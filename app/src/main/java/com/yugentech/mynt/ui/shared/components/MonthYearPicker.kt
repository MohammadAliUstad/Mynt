package com.yugentech.mynt.ui.shared.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import java.text.DateFormatSymbols
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/**
 * "‹ October 2026 ›" — steps a month either way, and opens a month grid when the label is
 * tapped. Months after the current one are out of reach: there's nothing to see there yet.
 */
@Composable
fun MonthSwitcher(
    month: YearMonth,
    onMonthChange: (YearMonth) -> Unit,
    modifier: Modifier = Modifier
) {
    var showPicker by remember { mutableStateOf(false) }
    val current = YearMonth.now()

    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onMonthChange(month.minusMonths(1)) }, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Rounded.ChevronLeft, contentDescription = "Previous month")
            }

            AnimatedContent(
                targetState = month,
                transitionSpec = {
                    val forward = targetState.isAfter(initialState)
                    (slideInHorizontally(tween(220)) { w -> if (forward) w / 3 else -w / 3 } + fadeIn(tween(220))) togetherWith
                        (slideOutHorizontally(tween(220)) { w -> if (forward) -w / 3 else w / 3 } + fadeOut(tween(160)))
                },
                label = "monthLabel"
            ) { shown ->
                Text(
                    text = monthTitle(shown),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { showPicker = true }
                        .padding(horizontal = 6.dp, vertical = 6.dp)
                )
            }

            IconButton(
                onClick = { onMonthChange(month.plusMonths(1)) },
                enabled = month.isBefore(current),
                modifier = Modifier.size(40.dp)
            ) {
                Icon(Icons.Rounded.ChevronRight, contentDescription = "Next month")
            }
        }
    }

    if (showPicker) {
        MonthYearPickerDialog(
            selected = month,
            onMonthSelected = {
                onMonthChange(it)
                showPicker = false
            },
            onDismissRequest = { showPicker = false }
        )
    }
}

/** "October 2026", or just "October" for the current year to keep the pill short. */
fun monthTitle(month: YearMonth): String {
    val name = month.month.getDisplayName(TextStyle.FULL_STANDALONE, Locale.getDefault())
        .replaceFirstChar { it.titlecase(Locale.getDefault()) }
    return if (month.year == YearMonth.now().year) name else "$name ${month.year}"
}

@Composable
fun MonthYearPickerDialog(
    selected: YearMonth,
    onMonthSelected: (YearMonth) -> Unit,
    onDismissRequest: () -> Unit
) {
    var year by remember { mutableIntStateOf(selected.year) }
    val current = YearMonth.now()
    val shortMonths = remember { DateFormatSymbols.getInstance(Locale.getDefault()).shortMonths.take(12) }

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Jump to month",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { year-- }) {
                        Icon(Icons.Rounded.ChevronLeft, contentDescription = "Previous year")
                    }
                    Text(
                        text = "$year",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = { year++ }, enabled = year < current.year) {
                        Icon(Icons.Rounded.ChevronRight, contentDescription = "Next year")
                    }
                }

                Spacer(Modifier.height(8.dp))

                shortMonths.chunked(3).forEachIndexed { rowIndex, row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEachIndexed { columnIndex, name ->
                            val candidate = YearMonth.of(year, rowIndex * 3 + columnIndex + 1)
                            val isSelected = candidate == selected
                            val isFuture = candidate.isAfter(current)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        when {
                                            isSelected -> MaterialTheme.colorScheme.primary
                                            else -> MaterialTheme.colorScheme.surfaceContainerHighest
                                        }
                                    )
                                    .clickable(enabled = !isFuture) { onMonthSelected(candidate) }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = when {
                                        isSelected -> MaterialTheme.colorScheme.onPrimary
                                        isFuture -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            }
                        }
                    }
                    if (rowIndex < 3) Spacer(Modifier.height(8.dp))
                }

                Spacer(Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { onMonthSelected(current) }) { Text("This month") }
                    Spacer(Modifier.width(4.dp))
                    TextButton(onClick = onDismissRequest) { Text("Cancel") }
                }
            }
        }
    }
}
