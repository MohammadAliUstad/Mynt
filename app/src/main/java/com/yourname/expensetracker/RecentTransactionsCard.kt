package com.yourname.expensetracker

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yourname.expensetracker.ui.theme.ExpenseTrackerTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Compact Card containing recent transactions for the selected month,
 * with downward expansion to show Full Transaction history on toggle,
 * and symmetrical upward shrink animation on collapse.
 *
 * Note: statement import now lives in the Import Documents card on the Home screen.
 * [onImportFileSelected] is kept (unused) only so older callers still compile.
 */
@Composable
fun RecentTransactionsCard(
    transactions: List<Expense>,
    modifier: Modifier = Modifier,
    onImportFileSelected: (String) -> Unit = {},
    isExpanded: Boolean = false,
    onToggleHistory: () -> Unit = {},
    onTransactionClick: (Expense) -> Unit = {}
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            if (transactions.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp, horizontal = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🧾", fontSize = 28.sp)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "No transactions yet",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Import your statement to get started instantly or add transactions manually.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                val topItems = transactions.take(3)
                val extraItems = transactions.drop(3)

                // Top 3 items (Always visible)
                topItems.forEachIndexed { index, transaction ->
                    TransactionItem(
                        transaction = transaction,
                        onClick = { onTransactionClick(transaction) }
                    )

                    if (index < topItems.size - 1 || extraItems.isNotEmpty()) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 4.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        )
                    }
                }

                // Extra items (Symmetrical vertical expand/shrink animation!)
                if (extraItems.isNotEmpty()) {
                    AnimatedVisibility(
                        visible = isExpanded,
                        enter = expandVertically(
                            animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
                            expandFrom = Alignment.Top
                        ) + fadeIn(animationSpec = tween(350)),
                        exit = shrinkVertically(
                            animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
                            shrinkTowards = Alignment.Top
                        ) + fadeOut(animationSpec = tween(350))
                    ) {
                        Column {
                            extraItems.forEachIndexed { index, transaction ->
                                TransactionItem(
                                    transaction = transaction,
                                    onClick = { onTransactionClick(transaction) }
                                )

                                if (index < extraItems.size - 1) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 4.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                    )
                                }
                            }
                        }
                    }

                    // Full History / Show Less Toggle Button
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 4.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    TextButton(
                        onClick = onToggleHistory,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isExpanded) "Show Less" else "Full History",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

/**
 * Individual Transaction Item.
 */
@Composable
private fun TransactionItem(
    transaction: Expense,
    onClick: () -> Unit
) {
    val (categoryIcon, categoryBgColor) = getCategoryIconAndColor(transaction.category, transaction.type)
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = MaterialTheme.colorScheme.onSurfaceVariant),
                onClick = {
                    scope.launch {
                        delay(150)
                        focusManager.clearFocus(force = true)
                        onClick()
                    }
                }
            )
            .padding(vertical = 10.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon Badge
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(categoryBgColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = categoryIcon,
                fontSize = 18.sp,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Details Column
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = transaction.title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "${transaction.category} • ${transaction.date}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Amount Column
        val formattedAmount = SettingsManager.formatAmount(transaction.amount)
        val amountColor = if (transaction.type == TransactionType.INCOME)
            Color(0xFF2E7D32)
        else
            MaterialTheme.colorScheme.onSurface

        Text(
            text = if (transaction.type == TransactionType.INCOME) "+$formattedAmount" else formattedAmount,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            color = amountColor
        )
    }
}

@Preview(showBackground = true, name = "Recent Transactions Card Preview")
@Composable
fun RecentTransactionsCardPreview() {
    ExpenseTrackerTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            RecentTransactionsCard(
                transactions = emptyList()
            )
        }
    }
}