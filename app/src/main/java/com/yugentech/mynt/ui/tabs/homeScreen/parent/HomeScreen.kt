package com.yugentech.mynt.ui.tabs.homeScreen.parent

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Handshake
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yugentech.mynt.database.model.Expense
import com.yugentech.mynt.database.model.TransactionType
import com.yugentech.mynt.transactions.insights.BudgetStatus
import com.yugentech.mynt.transactions.insights.FinanceInsights
import com.yugentech.mynt.transactions.insights.Insight
import com.yugentech.mynt.transactions.insights.MonthSummary
import com.yugentech.mynt.transactions.insights.sortedByDateDescending
import com.yugentech.mynt.ui.shared.components.AnimatedAmountText
import com.yugentech.mynt.ui.shared.components.BudgetDialog
import com.yugentech.mynt.ui.shared.components.EmptyState
import com.yugentech.mynt.ui.shared.components.MonthSwitcher
import com.yugentech.mynt.ui.shared.components.MyntCard
import com.yugentech.mynt.ui.shared.components.Pill
import com.yugentech.mynt.ui.shared.components.SectionHeader
import com.yugentech.mynt.ui.shared.components.SwipeableTransactionRow
import com.yugentech.mynt.ui.shared.components.charts.BudgetProgressBar
import com.yugentech.mynt.ui.shared.components.monthTitle
import com.yugentech.mynt.ui.shared.util.FinanceColors
import com.yugentech.mynt.ui.shared.util.LocalCurrencySymbol
import com.yugentech.mynt.ui.shared.util.formatAmount
import com.yugentech.mynt.ui.shared.util.formatAmountShort
import java.time.LocalTime
import java.time.YearMonth
import androidx.compose.material.icons.rounded.Groups

private const val RECENT_COUNT = 5

/**
 * Home: where the month stands at a glance. Net balance up top, the budget and how much is
 * safe to spend each day, one-tap ways to log, a few observations, and the latest entries.
 */
@Composable
fun HomeScreen(
    expenses: List<Expense>,
    userName: String,
    monthlyBudget: Double,
    bottomPadding: Dp,
    onSetBudget: (Double) -> Unit,
    onAddTransaction: (TransactionType) -> Unit,
    onVoiceClick: () -> Unit,
    onLentBorrowedClick: () -> Unit,
    onSplitClick: () -> Unit,
    onTransactionClick: (Expense) -> Unit,
    onDeleteTransaction: (Expense) -> Unit,
    onSeeAllClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    var month by rememberSaveable { mutableStateOf(YearMonth.now()) }
    var showBudgetDialog by remember { mutableStateOf(false) }
    val currency = LocalCurrencySymbol.current

    val summary = remember(expenses, month) { FinanceInsights.summary(expenses, month) }
    val budget = remember(expenses, month, monthlyBudget) {
        FinanceInsights.budgetStatus(expenses, month, monthlyBudget)
    }
    val streak = remember(expenses) { FinanceInsights.loggingStreak(expenses) }
    val insights = remember(expenses, month, currency) {
        FinanceInsights.insights(expenses, month) { formatAmountShort(it, currency) }
    }
    val recent = remember(expenses, month) {
        FinanceInsights.inMonth(expenses, month).sortedByDateDescending().take(RECENT_COUNT)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
            .padding(bottom = bottomPadding)
    ) {
        Spacer(Modifier.height(12.dp))

        HomeHeader(userName = userName, streak = streak, onProfileClick = onProfileClick)

        Spacer(Modifier.height(20.dp))

        BalanceHeroCard(
            summary = summary,
            month = month,
            onMonthChange = { month = it }
        )

        Spacer(Modifier.height(14.dp))

        BudgetCard(
            status = budget,
            month = month,
            onEditBudget = { showBudgetDialog = true }
        )

        Spacer(Modifier.height(20.dp))

        QuickActions(
            onExpense = { onAddTransaction(TransactionType.EXPENSE) },
            onIncome = { onAddTransaction(TransactionType.INCOME) },
            onVoice = onVoiceClick,
            onLentBorrowed = onLentBorrowedClick,
            onSplit = onSplitClick
        )

        AnimatedVisibility(visible = insights.isNotEmpty(), enter = fadeIn(), exit = fadeOut()) {
            Column {
                Spacer(Modifier.height(24.dp))
                SectionHeader(title = "Insights")
                Spacer(Modifier.height(8.dp))
                InsightsRow(insights = insights.take(3))
            }
        }

        Spacer(Modifier.height(20.dp))

        SectionHeader(
            title = if (month == YearMonth.now()) "Recent activity" else "Activity in ${monthTitle(month)}",
            actionLabel = if (recent.isNotEmpty()) "See all" else null,
            onAction = onSeeAllClick
        )

        Spacer(Modifier.height(6.dp))

        if (recent.isEmpty()) {
            MyntCard(modifier = Modifier.fillMaxWidth()) {
                EmptyState(
                    icon = Icons.Rounded.Mic,
                    title = "Nothing logged yet",
                    message = "Tap the orb and say something like \"Spent 250 on lunch\", or add it by hand.",
                    actionLabel = "Add manually",
                    onAction = { onAddTransaction(TransactionType.EXPENSE) }
                )
            }
        } else {
            Column(
                modifier = Modifier.animateContentSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                recent.forEach { expense ->
                    key(expense.id) {
                        SwipeableTransactionRow(
                            expense = expense,
                            onClick = { onTransactionClick(expense) },
                            onDelete = { onDeleteTransaction(expense) }
                        )
                    }
                }
            }
        }
    }

    if (showBudgetDialog) {
        BudgetDialog(
            currentBudget = monthlyBudget,
            onSave = {
                onSetBudget(it)
                showBudgetDialog = false
            },
            onDismiss = { showBudgetDialog = false }
        )
    }
}

@Composable
private fun HomeHeader(userName: String, streak: Int, onProfileClick: () -> Unit) {
    val greeting = remember {
        when (LocalTime.now().hour) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..21 -> "Good evening"
            else -> "Hello, night owl"
        }
    }
    val firstName = userName.trim().substringBefore(' ')

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .clickable(onClick = onProfileClick),
            contentAlignment = Alignment.Center
        ) {
            if (firstName.isNotEmpty()) {
                Text(
                    text = firstName.first().uppercase(),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            } else {
                Icon(
                    Icons.Rounded.Person,
                    contentDescription = "Profile",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (firstName.isNotEmpty()) greeting else "Welcome to Mynt",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = if (firstName.isNotEmpty()) firstName else greeting,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (streak > 0) {
            Pill(
                text = if (streak == 1) "1 day" else "$streak days",
                color = FinanceColors.lent,
                icon = Icons.Rounded.LocalFireDepartment
            )
        }
    }
}

@Composable
private fun BalanceHeroCard(
    summary: MonthSummary,
    month: YearMonth,
    onMonthChange: (YearMonth) -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    val gradient = Brush.linearGradient(
        listOf(primary, lerp(primary, Color.Black, 0.28f))
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(30.dp))
            .background(gradient)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Net balance",
                    style = MaterialTheme.typography.titleSmall,
                    color = onPrimary.copy(alpha = 0.8f),
                    modifier = Modifier.weight(1f)
                )
                Surface(shape = CircleShape, color = onPrimary.copy(alpha = 0.14f)) {
                    CompositionLocalProvider(LocalContentColor provides onPrimary) {
                        MonthSwitcherOnColor(month = month, onMonthChange = onMonthChange)
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            AnimatedAmountText(
                amount = kotlin.math.abs(summary.net),
                prefix = if (summary.net < 0) "−" else "",
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                color = onPrimary
            )

            summary.savingsRate?.let { rate ->
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (rate >= 0) "You've kept ${(rate * 100).toInt()}% of what came in"
                    else "Spending is ahead of income this month",
                    style = MaterialTheme.typography.bodyMedium,
                    color = onPrimary.copy(alpha = 0.8f)
                )
            }

            Spacer(Modifier.height(18.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HeroStat(
                    label = "Income",
                    amount = summary.income,
                    icon = Icons.AutoMirrored.Rounded.TrendingUp,
                    modifier = Modifier.weight(1f)
                )
                HeroStat(
                    label = "Spent",
                    amount = summary.expense,
                    icon = Icons.AutoMirrored.Rounded.TrendingDown,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/** The month switcher, restyled to sit on the coloured hero card. */
@Composable
private fun MonthSwitcherOnColor(month: YearMonth, onMonthChange: (YearMonth) -> Unit) {
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            surfaceContainerHigh = Color.Transparent,
            onSurface = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        MonthSwitcher(month = month, onMonthChange = onMonthChange)
    }
}

@Composable
private fun HeroStat(label: String, amount: Double, icon: ImageVector, modifier: Modifier = Modifier) {
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(onPrimary.copy(alpha = 0.12f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(onPrimary.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = onPrimary, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelMedium, color = onPrimary.copy(alpha = 0.75f))
            AnimatedAmountText(
                amount = amount,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = onPrimary
            )
        }
    }
}

@Composable
private fun BudgetCard(
    status: BudgetStatus?,
    month: YearMonth,
    onEditBudget: () -> Unit
) {
    if (status == null) {
        MyntCard(modifier = Modifier.fillMaxWidth(), onClick = onEditBudget) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Savings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Set a monthly budget",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        "See how much you can safely spend each day.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(Icons.Rounded.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        }
        return
    }

    MyntCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (status.isOver) "Over budget by" else "Left to spend",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                AnimatedAmountText(
                    amount = kotlin.math.abs(status.remaining),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (status.isOver) FinanceColors.expense else MaterialTheme.colorScheme.onSurface
                )
            }
            IconButton(onClick = onEditBudget) {
                Icon(Icons.Rounded.Edit, contentDescription = "Edit budget", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(Modifier.height(12.dp))
        BudgetProgressBar(progress = status.progress)
        Spacer(Modifier.height(10.dp))

        Row {
            Text(
                text = "${formatAmountShort(status.spent)} of ${formatAmountShort(status.budget)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${(status.progress * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (status.isCurrentMonth && status.daysLeft > 0) {
            Spacer(Modifier.height(12.dp))
            val (icon, text, tint) = when {
                status.isOver -> Triple(
                    Icons.Rounded.WarningAmber,
                    "You've passed this month's budget. Hold off where you can.",
                    FinanceColors.expense
                )
                status.projectedSpend > status.budget -> Triple(
                    Icons.Rounded.WarningAmber,
                    "At this pace you'll reach ${formatAmountShort(status.projectedSpend)}. " +
                        "Keep to ${formatAmountShort(status.dailyAllowance)}/day to stay on track.",
                    FinanceColors.lent
                )
                else -> Triple(
                    Icons.Rounded.Savings,
                    "${formatAmountShort(status.dailyAllowance)} a day for the next ${status.daysLeft} " +
                        (if (status.daysLeft == 1) "day" else "days") + " keeps you on budget.",
                    MaterialTheme.colorScheme.primary
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(tint.copy(alpha = 0.1f))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
            }
        } else if (!status.isCurrentMonth && month.isBefore(YearMonth.now())) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (status.isOver) "Finished ${formatAmount(-status.remaining)} over budget."
                else "Finished ${formatAmount(status.remaining)} under budget.",
                style = MaterialTheme.typography.bodySmall,
                color = if (status.isOver) FinanceColors.expense else FinanceColors.income
            )
        }
    }
}

@Composable
private fun QuickActions(
    onExpense: () -> Unit,
    onIncome: () -> Unit,
    onVoice: () -> Unit,
    onLentBorrowed: () -> Unit,
    onSplit: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        QuickAction("Expense", Icons.Rounded.Remove, FinanceColors.expense, onExpense, Modifier.weight(1f))
        QuickAction("Income", Icons.Rounded.Add, FinanceColors.income, onIncome, Modifier.weight(1f))
        QuickAction("Voice", Icons.Rounded.Mic, MaterialTheme.colorScheme.primary, onVoice, Modifier.weight(1f))
        QuickAction("Lend/Owe", Icons.Rounded.Handshake, FinanceColors.lent, onLentBorrowed, Modifier.weight(1f))
        QuickAction("Split bill", Icons.Rounded.Groups, FinanceColors.borrowed, onSplit, Modifier.weight(1f))
    }
}

@Composable
private fun QuickAction(
    label: String,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(tint.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
    }
}

@Composable
private fun InsightsRow(insights: List<Insight>) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        insights.forEach { insight -> InsightCard(insight) }
    }
}

@Composable
fun InsightCard(insight: Insight, modifier: Modifier = Modifier) {
    val tint = when (insight.tone) {
        Insight.Tone.POSITIVE -> FinanceColors.income
        Insight.Tone.WARNING -> FinanceColors.lent
        Insight.Tone.NEUTRAL -> MaterialTheme.colorScheme.primary
    }
    val icon = when (insight.tone) {
        Insight.Tone.POSITIVE -> Icons.AutoMirrored.Rounded.TrendingDown
        Insight.Tone.WARNING -> Icons.Rounded.WarningAmber
        Insight.Tone.NEUTRAL -> Icons.Rounded.Lightbulb
    }
    MyntCard(modifier = modifier.width(232.dp), contentPadding = 16.dp) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(10.dp))
        Text(
            insight.title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(2.dp))
        Text(
            insight.detail,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
