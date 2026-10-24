package com.yugentech.mynt.ui.split.parent

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.yugentech.mynt.split.logic.SplitCalculator
import com.yugentech.mynt.split.model.SplitBill
import com.yugentech.mynt.ui.shared.components.EmptyState
import com.yugentech.mynt.ui.shared.components.MyntCard
import com.yugentech.mynt.ui.shared.components.SectionHeader
import kotlinx.coroutines.delay

@Composable
internal fun BillTab(
    bills: List<SplitBill>,
    people: List<String>,
    onDeleteBill: (SplitBill) -> Unit,
    onAddAnother: () -> Unit,
    onUpdateBill: (SplitBill) -> Unit,
    onSettle: () -> Unit
) {
    var showGraph by rememberSaveable { mutableStateOf(false) }

    val scrollState = rememberScrollState()
    var lockedOffset by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(lockedOffset) {
        val target = lockedOffset ?: return@LaunchedEffect
        val end = System.currentTimeMillis() + 500
        while (System.currentTimeMillis() < end) {
            withFrameNanos { }
            if (scrollState.value != target) scrollState.scrollTo(target)
        }
        lockedOffset = null
    }

    fun switchView(graph: Boolean) {
        if (graph == showGraph) return
        lockedOffset = scrollState.value
        showGraph = graph
    }

    val transfers = remember(people, bills) { SplitCalculator.settle(people, bills) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp)
    ) {
        SectionHeader(
            title = "Who owes whom",
            actionLabel = "Add another bill",
            onAction = onAddAnother
        )
        Spacer(Modifier.height(6.dp))

        if (bills.isEmpty()) {
            MyntCard(modifier = Modifier.fillMaxWidth()) {
                EmptyState(
                    icon = Icons.AutoMirrored.Rounded.ReceiptLong,
                    title = "No bills yet",
                    message = "Add a bill on the Split tab."
                )
            }
        } else {
            AppearAnimated {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ViewToggle(showGraph = showGraph, onChange = { switchView(it) })

                    AnimatedContent(
                        targetState = showGraph,
                        transitionSpec = {
                            (fadeIn(tween(320)) + scaleIn(tween(320), initialScale = 0.96f)) togetherWith
                                    (fadeOut(tween(160)) + scaleOut(tween(160), targetScale = 0.98f))
                        },
                        label = "splitView"
                    ) { graph ->
                        if (graph) {
                            MyntCard(modifier = Modifier.fillMaxWidth(), contentPadding = 12.dp) {

                                // Updated Graph Implementation
                                SplitGraph(
                                    people = people,
                                    bills = bills
                                )

                                Text(
                                    text = "Tap the graph to visualize how equal the split is.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 4.dp).padding(top = 8.dp)
                                )
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    bills.forEach { bill ->
                                        key(bill.id) {
                                            StackInItem(onRemoved = { onDeleteBill(bill) }) { remove ->
                                                Box(modifier = Modifier.padding(bottom = 10.dp)) {
                                                    SwipeableBill(
                                                        bill = bill,
                                                        people = people,
                                                        onDelete = remove,
                                                        onUpdate = onUpdateBill,
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                SettleUpCard(transfers = transfers, onSettle = onSettle)
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.navigationBarsPadding().height(104.dp))
    }
}

@Composable
private fun StackInItem(
    onRemoved: () -> Unit,
    content: @Composable (remove: () -> Unit) -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    var removing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { visible = true }
    LaunchedEffect(removing) {
        if (removing) {
            visible = false
            delay(300)
            onRemoved()
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(320)) + slideInVertically(tween(320)) { it / 2 } + expandVertically(tween(320)),
        exit = fadeOut(tween(240)) + slideOutVertically(tween(240)) { it / 2 } + shrinkVertically(tween(240))
    ) {
        content { removing = true }
    }
}

@Composable
private fun SwipeableBill(
    bill: SplitBill,
    people: List<String>,
    onDelete: () -> Unit,
    onUpdate: (SplitBill) -> Unit
) {
    val state = rememberSwipeToDismissBoxState()
    var editing by remember { mutableStateOf(false) }

    LaunchedEffect(bill.id) {
        if (state.currentValue != SwipeToDismissBoxValue.Settled) {
            state.snapTo(SwipeToDismissBoxValue.Settled)
        }
    }

    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = false,
        gesturesEnabled = !editing,
        onDismiss = { value -> if (value == SwipeToDismissBoxValue.EndToStart) onDelete() },
        backgroundContent = {
            val active = state.targetValue == SwipeToDismissBoxValue.EndToStart
            val background by animateColorAsState(
                if (active) MaterialTheme.colorScheme.errorContainer else Color.Transparent,
                label = "billSwipeBackground"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(26.dp))
                    .background(background)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = "Delete bill",
                    tint = if (active) MaterialTheme.colorScheme.onErrorContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    ) {
        BillAccordion(
            bill = bill,
            allPeople = people,
            editing = editing,
            onEditingChange = { editing = it },
            onUpdate = onUpdate
        )
    }
}

@Composable
private fun ViewToggle(showGraph: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(4.dp)
    ) {
        ToggleChip("Table", Icons.Rounded.TableChart, selected = !showGraph) { onChange(false) }
        ToggleChip("Graph", Icons.Rounded.Hub, selected = showGraph) { onChange(true) }
    }
}

@Composable
private fun ToggleChip(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    val content = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
    else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = content)
    }
}