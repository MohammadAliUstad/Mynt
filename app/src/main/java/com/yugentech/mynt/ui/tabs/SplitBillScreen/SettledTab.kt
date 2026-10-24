package com.yugentech.mynt.ui.split.parent

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yugentech.mynt.split.model.SettledRecord
import com.yugentech.mynt.ui.shared.components.EmptyState
import com.yugentech.mynt.ui.shared.components.MyntCard
import java.time.format.DateTimeFormatter

private val SettledCardShape = RoundedCornerShape(26.dp)

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun SettledTab(
    settled: List<SettledRecord>,
    selectedId: String?,
    onLongPress: (String) -> Unit,
    onTap: (String) -> Unit,
    onBounds: (String, Rect) -> Unit
) {
    val locale = LocalLocale.current.platformLocale
    val haptic = LocalHapticFeedback.current
    val scheme = MaterialTheme.colorScheme

    if (settled.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(32.dp))
            MyntCard(modifier = Modifier.fillMaxWidth()) {
                EmptyState(
                    icon = Icons.Rounded.CheckCircle,
                    title = "Nothing settled yet",
                    message = "When you settle a bill, the record will show up here."
                )
            }
        }
        return
    }

    // Ensure the newest / freshest total settle-ups always appear at the top of the list
    val sortedSettled = remember(settled) {
        settled.sortedWith(
            compareByDescending<SettledRecord> { it.settledOn }
                .thenByDescending { it.id }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        AnimatedVisibility(visible = selectedId == null) {
            Text(
                "Long-press a summary to select it",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )
        }

        sortedSettled.forEach { record ->
            key(record.id) {
                val isSelected = record.id == selectedId
                val borderColor by animateColorAsState(
                    if (isSelected) scheme.primary else Color.Transparent,
                    label = "settledBorder"
                )
                val cardColor by animateColorAsState(
                    if (isSelected) lerp(scheme.surfaceContainer, scheme.primaryContainer, 0.45f)
                    else scheme.surfaceContainer,
                    label = "settledColor"
                )

                AppearAnimated {
                    Box(
                        modifier = Modifier
                            .padding(bottom = 10.dp)
                            .onGloballyPositioned { onBounds(record.id, it.boundsInRoot()) }
                    ) {
                        MyntCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(SettledCardShape)
                                .border(2.dp, borderColor, SettledCardShape)
                                .combinedClickable(
                                    onClick = { onTap(record.id) },
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onLongPress(record.id)
                                    }
                                ),
                            color = cardColor,
                            contentPadding = 14.dp
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Total settle-up",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        "Settled on " + record.settledOn.format(DateTimeFormatter.ofPattern("d MMM yyyy", locale)),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = scheme.onSurfaceVariant,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                }
                                AnimatedVisibility(
                                    visible = isSelected,
                                    enter = scaleIn() + fadeIn()
                                ) {
                                    Icon(
                                        Icons.Rounded.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = scheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            if (record.transfers.isEmpty()) {
                                Text(
                                    "Everyone was settled up.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = scheme.onSurfaceVariant
                                )
                            } else {
                                TableLabels()
                                record.transfers.forEachIndexed { index, transfer ->
                                    if (index > 0) {
                                        HorizontalDivider(
                                            color = scheme.outlineVariant.copy(alpha = 0.5f)
                                        )
                                    }
                                    TransferRow(from = transfer.from, to = transfer.to, amount = transfer.amount)
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.navigationBarsPadding().height(104.dp))
    }
}