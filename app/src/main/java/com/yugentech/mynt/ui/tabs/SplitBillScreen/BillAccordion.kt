package com.yugentech.mynt.ui.split.parent

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yugentech.mynt.split.model.SplitBill
import com.yugentech.mynt.ui.shared.components.MyntCard
import com.yugentech.mynt.ui.shared.util.FinanceColors
import com.yugentech.mynt.ui.shared.util.LocalCurrencySymbol
import com.yugentech.mynt.ui.shared.util.formatAmount
import java.time.format.DateTimeFormatter
import kotlin.math.hypot
import kotlin.math.round

private val SplitEaseOutBack = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)
private val SplitEaseOutQuart = CubicBezierEasing(0.25f, 1f, 0.5f, 1f)

@Composable
private fun Modifier.cardRipple(trigger: Int, color: Color): Modifier {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(trigger) {
        if (trigger == 0) return@LaunchedEffect
        progress.snapTo(0f)
        progress.animateTo(1f, tween(650, easing = SplitEaseOutQuart))
    }
    return clip(RoundedCornerShape(26.dp)).drawWithContent {
        drawContent()
        val p = progress.value
        if (p > 0f && p < 1f) {
            drawCircle(
                color = color.copy(alpha = 0.22f * (1f - p)),
                radius = hypot(size.width, size.height) * p,
                center = Offset(size.width * 0.12f, size.height - 36.dp.toPx())
            )
        }
    }
}

private fun reshare(amount: Double, remaining: Map<String, Double>): Map<String, Double> {
    if (remaining.isEmpty()) return remaining
    val sum = remaining.values.sum()
    val raw = if (sum > 0.0) remaining.mapValues { it.value * amount / sum }
    else remaining.mapValues { amount / remaining.size }
    val cents = raw.mapValues { round(it.value * 100) / 100.0 }
    val diff = round((amount - cents.values.sum()) * 100) / 100.0
    if (diff == 0.0) return cents
    val target = cents.maxByOrNull { it.value }?.key ?: return cents
    return cents.toMutableMap().also { it[target] = round((it.getValue(target) + diff) * 100) / 100.0 }
}

/**
 * Adds [name] with an equal share and scales everyone else down proportionally,
 * so the shares still add up to exactly [amount].
 */
private fun addShare(amount: Double, current: Map<String, Double>, name: String): Map<String, Double> {
    if (current.isEmpty()) return mapOf(name to amount)
    val newShare = round(amount / (current.size + 1) * 100) / 100.0
    val others = reshare(amount - newShare, current)
    return others + (name to newShare)
}

@Composable
private fun EditableTransferRow(
    from: String,
    to: String,
    amount: Double,
    editing: Boolean,
    canRemove: Boolean,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currency = LocalCurrencySymbol.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            AnimatedVisibility(
                visible = editing && canRemove,
                enter = scaleIn(tween(260, easing = SplitEaseOutBack), initialScale = 0.2f) +
                        fadeIn(tween(160)) +
                        expandHorizontally(tween(260, easing = SplitEaseOutQuart)),
                exit = scaleOut(tween(160, easing = SplitEaseOutQuart), targetScale = 0.2f) +
                        fadeOut(tween(120)) +
                        shrinkHorizontally(tween(160, easing = SplitEaseOutQuart))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.RemoveCircleOutline,
                        contentDescription = "Remove $from from this bill",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onRemove)
                    )
                    Spacer(Modifier.width(6.dp))
                }
            }
            Text(
                from,
                Modifier.weight(1f, fill = false),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = FinanceColors.expense,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            formatAmount(amount, currency),
            Modifier.weight(1f),
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Start,
            maxLines = 1
        )
        Text(
            to,
            Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = FinanceColors.income,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Edit mode only: tap someone already in the session, or type a new name, to add them to this bill. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddPersonSection(
    candidates: List<String>,
    onAdd: (String) -> Unit
) {
    var newName by remember { mutableStateOf("") }

    fun submit() {
        onAdd(newName)
        newName = ""
    }

    Column {
        Spacer(Modifier.height(8.dp))
        Text(
            "Add someone",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
        )

        if (candidates.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                candidates.forEach { name ->
                    key(name) {
                        AssistChip(
                            onClick = { onAdd(name) },
                            label = { Text(name) },
                            leadingIcon = {
                                Icon(
                                    Icons.Rounded.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            shape = CircleShape
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        PillTextField(
            value = newName,
            onValueChange = { newName = it },
            placeholder = "Or type a new name",
            capitalization = KeyboardCapitalization.Words,
            leading = { Icon(Icons.Rounded.Person, contentDescription = null) },
            trailing = {
                IconButton(onClick = { submit() }) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add person")
                }
            },
            onDone = { submit() }
        )
    }
}

@Composable
internal fun BillAccordion(
    bill: SplitBill,
    allPeople: List<String>,
    editing: Boolean,
    onEditingChange: (Boolean) -> Unit,
    onUpdate: (SplitBill) -> Unit
) {
    val currency = LocalCurrencySymbol.current
    val locale = LocalLocale.current.platformLocale
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    var expanded by remember { mutableStateOf(false) }
    var draftTitle by remember { mutableStateOf(bill.title) }
    var payerMenuOpen by remember { mutableStateOf(false) }
    var rippleTrigger by remember { mutableIntStateOf(0) }
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(250),
        label = "billArrow"
    )

    val owing = bill.shares.filter { (name, share) -> name != bill.paidBy && share > 0.0 }
    val participants = (bill.shares.keys + bill.paidBy).distinct()
    val canRemove = bill.shares.size > 1

    // People already in the session who aren't sharing this bill yet.
    val candidates = allPeople.filter { person ->
        bill.shares.keys.none { it.equals(person, ignoreCase = true) }
    }

    fun addPerson(raw: String) {
        val typed = raw.trim().replaceFirstChar { it.titlecase(locale) }
        if (typed.isEmpty()) return
        // Use the spelling already in the session, so "arbaz" and "Arbaz" stay one person.
        val name = allPeople.firstOrNull { it.equals(typed, ignoreCase = true) } ?: typed
        if (bill.shares.keys.any { it.equals(name, ignoreCase = true) }) return
        onUpdate(bill.copy(shares = addShare(bill.amount, bill.shares, name)))
    }

    fun toggleEdit() {
        if (editing) {
            val title = draftTitle.trim()
            if (title.isNotEmpty() && title != bill.title) onUpdate(bill.copy(title = title))
            payerMenuOpen = false
            onEditingChange(false)
        } else {
            draftTitle = bill.title
            onEditingChange(true)
        }
        rippleTrigger++
    }

    MyntCard(
        modifier = Modifier
            .fillMaxWidth()
            .cardRipple(rippleTrigger, MaterialTheme.colorScheme.primary),
        onClick = { if (!editing) expanded = !expanded },
        contentPadding = 14.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (editing) {
                Box(modifier = Modifier.weight(1f)) {
                    PillTextField(
                        value = draftTitle,
                        onValueChange = { draftTitle = it },
                        placeholder = "What for?",
                        capitalization = KeyboardCapitalization.Sentences
                    )
                }
            } else {
                Text(
                    bill.title,
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                Icons.Rounded.KeyboardArrowDown,
                contentDescription = if (expanded) "Collapse" else "Expand",
                modifier = Modifier.rotate(arrowRotation),
                tint = muted
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(modifier = Modifier.animateContentSize(tween(260, easing = SplitEaseOutQuart))) {
                Spacer(Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Paid by ", style = MaterialTheme.typography.bodySmall, color = muted)
                    Box {
                        if (editing) {
                            val onChip = MaterialTheme.colorScheme.onPrimaryContainer
                            Row(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .clickable { payerMenuOpen = true }
                                    .padding(start = 10.dp, end = 4.dp, top = 3.dp, bottom = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    bill.paidBy,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = onChip,
                                    maxLines = 1
                                )
                                Icon(
                                    Icons.Rounded.KeyboardArrowDown,
                                    contentDescription = "Change who paid",
                                    tint = onChip,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            DropdownMenu(
                                expanded = payerMenuOpen,
                                onDismissRequest = { payerMenuOpen = false }
                            ) {
                                participants.forEach { name ->
                                    DropdownMenuItem(
                                        text = { Text(name) },
                                        onClick = {
                                            payerMenuOpen = false
                                            if (name != bill.paidBy) onUpdate(bill.copy(paidBy = name))
                                        }
                                    )
                                }
                            }
                        } else {
                            Text(bill.paidBy, style = MaterialTheme.typography.bodySmall, color = muted)
                        }
                    }
                    Text(
                        " · ${formatAmount(bill.amount, currency)} · " +
                                bill.date.format(DateTimeFormatter.ofPattern("d MMM yyyy", locale)),
                        modifier = Modifier.weight(1f, fill = false),
                        style = MaterialTheme.typography.bodySmall,
                        color = muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(10.dp))

                if (owing.isEmpty()) {
                    Text(
                        "Nobody owes anything for this bill.",
                        style = MaterialTheme.typography.bodySmall,
                        color = muted
                    )
                } else {
                    TableLabels()
                    owing.entries.forEachIndexed { index, entry ->
                        key(entry.key) {
                            EditableTransferRow(
                                from = entry.key,
                                to = bill.paidBy,
                                amount = entry.value,
                                editing = editing,
                                canRemove = canRemove,
                                onRemove = {
                                    onUpdate(bill.copy(shares = reshare(bill.amount, bill.shares - entry.key)))
                                }
                            )
                        }
                    }
                }

                AnimatedVisibility(
                    visible = editing,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    AddPersonSection(
                        candidates = candidates,
                        onAdd = ::addPerson
                    )
                }

                Spacer(Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = { toggleEdit() }) {
                        Icon(
                            if (editing) Icons.Rounded.Done else Icons.Rounded.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(if (editing) "Done" else "Edit")
                    }
                }
            }
        }
    }
}