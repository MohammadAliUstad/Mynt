package com.yugentech.mynt.ui.split.parent

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yugentech.mynt.split.model.SPLIT_SELF
import com.yugentech.mynt.ui.shared.util.FinanceColors
import com.yugentech.mynt.ui.shared.util.LocalCurrencySymbol

@Composable
fun SplitBillScreen(
    state: SplitScreenState,
    onBackClick: () -> Unit
) {
    var tab by state::tab
    val people = state.people
    val bills = state.bills
    val form = state.form
    val locale = LocalLocale.current.platformLocale

    // The AI message should use the user's currency symbol.
    val currency = LocalCurrencySymbol.current
    SideEffect { state.currencySymbol = currency }

    LaunchedEffect(bills.size) {
        if (bills.isEmpty() && tab == SplitTab.BILL) tab = SplitTab.SPLIT
    }

    // Leaving the Settled tab drops any selection.
    LaunchedEffect(tab) {
        if (tab != SplitTab.SETTLED) state.clearSelection()
    }

    fun addPerson(raw: String) {
        val name = raw.trim().replaceFirstChar { it.titlecase(locale) }
        if (name.isEmpty()) return
        val existing = people.firstOrNull { it.equals(name, ignoreCase = true) }
        if (existing == null) {
            people.add(name)
            form.selected.add(name)
        } else if (existing !in form.selected) {
            form.selected.add(existing)
        }
    }

    fun removePerson(name: String) {
        people.remove(name)
        form.selected.remove(name)
        form.inputs.remove(name)
        if (form.paidBy == name) form.paidBy = SPLIT_SELF
    }

    fun canRemove(name: String): Boolean =
        name != SPLIT_SELF && bills.none { it.paidBy == name || name in it.shares }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = "Split bill",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            }

            SplitTabSelector(
                selected = tab,
                billEnabled = bills.isNotEmpty(),
                onSelect = { tab = it },
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(Modifier.height(12.dp))

            AnimatedContent(
                targetState = tab,
                modifier = Modifier.weight(1f),
                transitionSpec = {
                    val forward = targetState.ordinal > initialState.ordinal
                    (slideInHorizontally(tween(300)) { w -> if (forward) w / 4 else -w / 4 } + fadeIn(tween(300))) togetherWith
                            (slideOutHorizontally(tween(300)) { w -> if (forward) -w / 4 else w / 4 } + fadeOut(tween(200)))
                },
                label = "splitTab"
            ) { current ->
                when (current) {
                    SplitTab.SPLIT -> SplitForm(
                        form = form,
                        people = people.toList(),
                        billCount = bills.size,
                        onAddPerson = ::addPerson,
                        onRemovePerson = ::removePerson,
                        canRemove = ::canRemove,
                        onSubmit = { bill ->
                            state.saveBill(bill)
                            form.reset()
                            tab = SplitTab.BILL
                        }
                    )

                    SplitTab.BILL -> BillTab(
                        bills = bills.toList(),
                        people = people.toList(),
                        onDeleteBill = { bill -> state.deleteBill(bill) },
                        onAddAnother = { tab = SplitTab.SPLIT },
                        onUpdateBill = { updated -> state.updateBill(updated) },
                        onSettle = { state.settleAll() }
                    )

                    SplitTab.SETTLED -> SettledTab(
                        settled = state.settled.toList(),
                        selectedId = state.selectedSettledId,
                        onLongPress = state::select,
                        onTap = state::toggleIfSelecting,
                        onBounds = { id, rect -> state.cardBounds[id] = rect }
                    )
                }
            }
        }

        // The AI message panel grows out of the selected card and sits above everything here.
        SettleMessageOverlay(state = state)
    }

    if (state.confirmDelete) {
        AlertDialog(
            onDismissRequest = state::cancelDelete,
            title = { Text("Delete this settle-up?") },
            text = { Text("It will be removed from this phone and your cloud backup. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = state::confirmDeleteNow) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = state::cancelDelete) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SplitTabSelector(
    selected: SplitTab,
    billEnabled: Boolean,
    onSelect: (SplitTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = listOf(SplitTab.SPLIT to "Split", SplitTab.BILL to "Bill", SplitTab.SETTLED to "Settled")
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
    ) {
        BoxWithConstraints(modifier = Modifier.padding(4.dp)) {
            val cell = maxWidth / options.size
            val offset by animateDpAsState(
                targetValue = cell * options.indexOfFirst { it.first == selected },
                animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
                label = "splitTabIndicator"
            )
            Box(
                modifier = Modifier
                    .offset(x = offset)
                    .width(cell)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(FinanceColors.borrowed)
            )
            Row(modifier = Modifier.fillMaxSize()) {
                options.forEach { (option, label) ->
                    val enabled = option != SplitTab.BILL || billEnabled
                    val isSelected = option == selected
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .clickable(
                                enabled = enabled,
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onSelect(option) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = when {
                                isSelected -> Color.White
                                enabled -> MaterialTheme.colorScheme.onSurfaceVariant
                                else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                            }
                        )
                    }
                }
            }
        }
    }
}