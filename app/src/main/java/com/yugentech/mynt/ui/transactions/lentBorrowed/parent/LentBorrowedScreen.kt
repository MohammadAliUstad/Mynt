package com.yugentech.mynt.ui.transactions.lentBorrowed.parent

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.CallMade
import androidx.compose.material.icons.automirrored.rounded.CallReceived
import androidx.compose.material.icons.rounded.Handshake
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yugentech.mynt.database.model.Expense
import com.yugentech.mynt.database.model.SETTLEMENT_CATEGORY
import com.yugentech.mynt.database.model.TransactionType
import com.yugentech.mynt.transactions.insights.FinanceInsights
import com.yugentech.mynt.transactions.insights.PersonBalance
import com.yugentech.mynt.transactions.insights.TransactionDates
import com.yugentech.mynt.transactions.insights.onDate
import com.yugentech.mynt.transactions.insights.sortedByDateDescending
import com.yugentech.mynt.ui.shared.components.AnimatedAmountText
import com.yugentech.mynt.ui.shared.components.EmptyState
import com.yugentech.mynt.ui.shared.components.MyntCard
import com.yugentech.mynt.ui.shared.components.SectionHeader
import com.yugentech.mynt.ui.shared.components.TransactionRow
import com.yugentech.mynt.ui.shared.util.FinanceColors
import com.yugentech.mynt.ui.shared.util.formatAmountShort
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

/**
 * Who owes whom. Balances run across all time rather than a month, since a loan from
 * March is still owed in October. Settling records a payback entry, so the history stays.
 */
@Composable
fun LentBorrowedScreen(
    expenses: List<Expense>,
    onBackClick: () -> Unit,
    onAddClick: (TransactionType) -> Unit,
    onTransactionClick: (Expense) -> Unit,
    onSettle: (Expense) -> Unit
) {
    val people = remember(expenses) { FinanceInsights.personBalances(expenses) }
    val owedToYou = people.filter { it.balance > 0 }.sumOf { it.balance }
    val youOwe = people.filter { it.balance < 0 }.sumOf { -it.balance }
    var expanded by rememberSaveable { mutableStateOf<String?>(null) }
    var settling by remember { mutableStateOf<PersonBalance?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
            }
            Text(
                "Lend & owe",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BalanceTile(
                        label = "Owed to you",
                        amount = owedToYou,
                        color = FinanceColors.income,
                        icon = Icons.AutoMirrored.Rounded.CallReceived,
                        modifier = Modifier.weight(1f)
                    )
                    BalanceTile(
                        label = "You owe",
                        amount = youOwe,
                        color = FinanceColors.expense,
                        icon = Icons.AutoMirrored.Rounded.CallMade,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilledTonalButton(
                        onClick = { onAddClick(TransactionType.LENT) },
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.CallMade, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("I lent")
                    }
                    FilledTonalButton(
                        onClick = { onAddClick(TransactionType.BORROWED) },
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.CallReceived, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("I borrowed")
                    }
                }
            }

            if (people.isEmpty()) {
                item {
                    MyntCard(modifier = Modifier.fillMaxWidth()) {
                        EmptyState(
                            icon = Icons.Rounded.Handshake,
                            title = "No IOUs yet",
                            message = "Log money you lend or borrow and Mynt keeps a running balance for each person."
                        )
                    }
                }
            } else {
                item { SectionHeader(title = "People", modifier = Modifier.padding(top = 8.dp)) }
                items(people, key = { it.name.lowercase(Locale.getDefault()) }) { person ->
                    val key = person.name.lowercase(Locale.getDefault())
                    PersonCard(
                        person = person,
                        entries = expenses
                            .filter {
                                (it.type == TransactionType.LENT || it.type == TransactionType.BORROWED) &&
                                    it.personName.trim().ifBlank { it.title.trim() }.lowercase(Locale.getDefault()) == key
                            }
                            .sortedByDateDescending(),
                        isExpanded = expanded == key,
                        onToggle = { expanded = if (expanded == key) null else key },
                        onSettle = { settling = person },
                        onTransactionClick = onTransactionClick,
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
    }

    settling?.let { person ->
        val theyOwe = person.balance > 0
        AlertDialog(
            onDismissRequest = { settling = null },
            shape = RoundedCornerShape(28.dp),
            icon = { Icon(Icons.Rounded.Handshake, contentDescription = null) },
            title = { Text("Settle up with ${person.name}?") },
            text = {
                Text(
                    if (theyOwe) "This records ${person.name} paying you back ${formatAmountShort(person.balance)}."
                    else "This records you paying ${person.name} back ${formatAmountShort(-person.balance)}."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onSettle(settlementFor(person))
                    settling = null
                }) { Text("Settle") }
            },
            dismissButton = { TextButton(onClick = { settling = null }) { Text("Cancel") } }
        )
    }
}

/** A payback entry that brings [person]'s balance to exactly zero. */
private fun settlementFor(person: PersonBalance): Expense {
    val theyOwe = person.balance > 0
    return Expense(
        id = UUID.randomUUID().toString(),
        title = "Settled up",
        amount = kotlin.math.abs(person.balance),
        category = SETTLEMENT_CATEGORY,
        date = "",
        monthYear = "",
        // Their repayment is money coming back to you; yours is money going out to them.
        type = if (theyOwe) TransactionType.BORROWED else TransactionType.LENT,
        personName = person.name
    ).onDate(TransactionDates.today())
}

@Composable
private fun BalanceTile(
    label: String,
    amount: Double,
    color: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    MyntCard(modifier = modifier, contentPadding = 16.dp) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.height(10.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        AnimatedAmountText(
            amount = amount,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = color
        )
    }
}

@Composable
private fun PersonCard(
    person: PersonBalance,
    entries: List<Expense>,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onSettle: () -> Unit,
    onTransactionClick: (Expense) -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColor = when {
        person.isSettled -> MaterialTheme.colorScheme.onSurfaceVariant
        person.balance > 0 -> FinanceColors.income
        else -> FinanceColors.expense
    }
    val status = when {
        person.isSettled -> "All settled"
        person.balance > 0 -> "Owes you ${formatAmountShort(person.balance)}"
        else -> "You owe ${formatAmountShort(-person.balance)}"
    }
    val arrowRotation = if (isExpanded) 180f else 0f

    MyntCard(
        modifier = modifier.fillMaxWidth().animateContentSize(),
        onClick = onToggle,
        contentPadding = 14.dp
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(statusColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    person.name.first().uppercase(),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = statusColor
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    person.name,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "$status · ${person.lastActivity.format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = statusColor
                )
            }
            Icon(
                Icons.Rounded.KeyboardArrowDown,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                modifier = Modifier.rotate(arrowRotation),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column {
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        "Lent ${formatAmountShort(person.totalLent)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Borrowed ${formatAmountShort(person.totalBorrowed)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(6.dp))
                entries.forEach { entry ->
                    TransactionRow(expense = entry, onClick = { onTransactionClick(entry) })
                }
                if (!person.isSettled) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = onSettle, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Rounded.Handshake, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Settle up")
                    }
                }
            }
        }
    }
}
