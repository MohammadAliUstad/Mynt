package com.yugentech.mynt.ui.tabs.homeScreen.parent

import kotlinx.coroutines.launch
import com.yugentech.mynt.ui.shared.util.categoryStyle
import com.yugentech.mynt.transactions.insights.localDate
import com.yugentech.mynt.transactions.insights.TransactionDates
import com.yugentech.mynt.transactions.insights.CategoryShare
import com.yugentech.mynt.domain.model.HeroCardKind
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.History
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
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
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yugentech.mynt.database.model.Expense
import com.yugentech.mynt.database.model.TransactionType
import com.yugentech.mynt.domain.model.CardAnimationStyle
import com.yugentech.mynt.ui.shared.components.hero.HeroCardBackground
import com.yugentech.mynt.ui.shared.components.hero.rememberHeroClock
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
import kotlin.math.absoluteValue
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
    onProfileClick: () -> Unit,
    cardAnimationStyle: CardAnimationStyle = CardAnimationStyle.WATER,
    heroCards: List<HeroCardKind> = HeroCardKind.entries,
    onSummaryClick: (TransactionType, YearMonth) -> Unit = { _, _ -> }
) {
    var month by rememberSaveable { mutableStateOf(YearMonth.now()) }
    var showBudgetDialog by remember { mutableStateOf(false) }
    val currency = LocalCurrencySymbol.current

    val summary = remember(expenses, month) { FinanceInsights.summary(expenses, month) }
    val budget = remember(expenses, month, monthlyBudget) {
        FinanceInsights.budgetStatus(expenses, month, monthlyBudget)
    }
    val streak = remember(expenses) { FinanceInsights.loggingStreak(expenses) }
    // Numbers behind the Today, Top spending and Vs last month cards
    val extras = remember(expenses, month) {
        val today = TransactionDates.today()
        fun spentOn(day: java.time.LocalDate) =
            expenses.filter { it.type == TransactionType.EXPENSE && it.localDate == day }.sumOf { it.amount }
        HeroExtras(
            todaySpent = spentOn(today),
            todayCount = expenses.count { it.localDate == today },
            yesterdaySpent = spentOn(today.minusDays(1)),
            dailyAverage = FinanceInsights.averageDailySpend(expenses, month),
            topCategories = FinanceInsights.categoryBreakdown(FinanceInsights.inMonth(expenses, month)).take(3),
            lastMonth = FinanceInsights.summary(expenses, month.minusMonths(1))
        )
    }
    // Outstanding balances are all-time (a loan doesn't expire at month end), so no month filter here.
    val lendOwe = remember(expenses) {
        val open = FinanceInsights.personBalances(expenses).filter { !it.isSettled }
        LendOweSummary(
            owedToYou = open.filter { it.balance > 0 }.sumOf { it.balance },
            youOwe = open.filter { it.balance < 0 }.sumOf { -it.balance },
            peopleOweYou = open.count { it.balance > 0 },
            youOwePeople = open.count { it.balance < 0 }
        )
    }
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
            cards = heroCards,
            summary = summary,
            lendOwe = lendOwe,
            extras = extras,
            month = month,
            onMonthChange = { month = it },
            animationStyle = cardAnimationStyle,
            onStatClick = onSummaryClick
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

/** What's outstanding with other people, across all time. Positive net = people owe you. */
private data class LendOweSummary(
    val owedToYou: Double,
    val youOwe: Double,
    val peopleOweYou: Int,
    val youOwePeople: Int
) {
    val net: Double get() = owedToYou - youOwe
}

/** Numbers behind the Today, Top spending and Vs last month cards. */
private data class HeroExtras(
    val todaySpent: Double,
    val todayCount: Int,
    val yesterdaySpent: Double,
    val dailyAverage: Double,
    val topCategories: List<CategoryShare>,
    val lastMonth: MonthSummary
)

private class HeroStatSpec(
    val label: String,
    val amount: Double,
    val icon: ImageVector,
    /** Tapping the tile opens History on this type. Null leaves the tile as plain information. */
    val type: TransactionType? = null,
    /** The month History opens on. Null means the month currently selected on Home. */
    val month: YearMonth? = null
)

/** Everything one card shows. Every card uses the same layout, so they are always the same height. */
private class HeroPageSpec(
    val title: String,
    /** Text shown where the month switcher normally sits. Null keeps the real switcher. */
    val trailingLabel: String?,
    /** The big number. A negative value is shown with a minus sign. */
    val amount: Double,
    val subtitle: String,
    val first: HeroStatSpec,
    val second: HeroStatSpec
)

/**
 * What each card says. To add a card: add an entry to [HeroCardKind] (in SettingsConfiguration.kt),
 * add a branch here and a gradient in [heroBrush]. The swipe, dots, animation and the on/off
 * switch in Theme settings all pick it up on their own.
 */
@Composable
private fun heroPageFor(
    kind: HeroCardKind,
    summary: MonthSummary,
    lendOwe: LendOweSummary,
    extras: HeroExtras,
    month: YearMonth
): HeroPageSpec = when (kind) {
    HeroCardKind.MONTH -> HeroPageSpec(
        title = "Net balance",
        trailingLabel = null,
        amount = summary.net,
        subtitle = summary.savingsRate?.let { rate ->
            if (rate >= 0) "You've kept ${(rate * 100).toInt()}% of what came in"
            else "Spending is ahead of income this month"
        } ?: "No income logged yet this month",
        first = HeroStatSpec("Income", summary.income, Icons.AutoMirrored.Rounded.TrendingUp, TransactionType.INCOME),
        second = HeroStatSpec("Spent", summary.expense, Icons.AutoMirrored.Rounded.TrendingDown, TransactionType.EXPENSE)
    )

    HeroCardKind.LEND_OWE -> HeroPageSpec(
        title = "Lend & owe",
        trailingLabel = "All time",
        amount = lendOwe.net,
        subtitle = lendOweSubtitle(lendOwe),
        first = HeroStatSpec("Owed to you", lendOwe.owedToYou, Icons.Rounded.ArrowDownward, TransactionType.LENT),
        second = HeroStatSpec("You owe", lendOwe.youOwe, Icons.Rounded.ArrowUpward, TransactionType.BORROWED)
    )

    HeroCardKind.TODAY -> {
        val today = TransactionDates.today()
        HeroPageSpec(
            title = "Spent today",
            trailingLabel = today.format(java.time.format.DateTimeFormatter.ofPattern("EEE d MMM", java.util.Locale.getDefault())),
            amount = extras.todaySpent,
            subtitle = when (extras.todayCount) {
                0 -> "Nothing logged yet today"
                1 -> "1 entry today"
                else -> "${extras.todayCount} entries today"
            },
            first = HeroStatSpec(
                "Yesterday", extras.yesterdaySpent, Icons.Rounded.History,
                TransactionType.EXPENSE, YearMonth.from(today.minusDays(1))
            ),
            second = HeroStatSpec("Daily average", extras.dailyAverage, Icons.Rounded.CalendarMonth)
        )
    }

    HeroCardKind.TOP_SPENDING -> {
        val top = extras.topCategories.firstOrNull()
        val placeholder = HeroStatSpec("—", 0.0, Icons.Rounded.Category)
        HeroPageSpec(
            title = "Top spending",
            trailingLabel = null,
            amount = top?.amount ?: 0.0,
            subtitle = top?.let { "${it.category} is ${(it.share * 100).toInt()}% of your spending" }
                ?: "No spending logged this month",
            first = extras.topCategories.getOrNull(1)?.let {
                HeroStatSpec(it.category, it.amount, categoryStyle(it.category, TransactionType.EXPENSE).icon, TransactionType.EXPENSE)
            } ?: placeholder,
            second = extras.topCategories.getOrNull(2)?.let {
                HeroStatSpec(it.category, it.amount, categoryStyle(it.category, TransactionType.EXPENSE).icon, TransactionType.EXPENSE)
            } ?: placeholder
        )
    }

    HeroCardKind.VS_LAST_MONTH -> {
        val last = extras.lastMonth
        val lastName = month.minusMonths(1).month.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.getDefault())
        val change = summary.expense - last.expense
        val percent = if (last.expense > 0) kotlin.math.abs(change / last.expense * 100).toInt() else 0
        HeroPageSpec(
            title = "Vs last month",
            trailingLabel = null,
            amount = summary.expense,
            subtitle = when {
                last.expense <= 0 -> "No spending logged in $lastName"
                percent == 0 -> "About the same as $lastName"
                change < 0 -> "$percent% less than $lastName"
                else -> "$percent% more than $lastName"
            },
            first = HeroStatSpec("Last month", last.expense, Icons.Rounded.History, TransactionType.EXPENSE, month.minusMonths(1)),
            second = HeroStatSpec(
                label = if (change <= 0) "Spent less by" else "Spent more by",
                amount = kotlin.math.abs(change),
                icon = if (change <= 0) Icons.Rounded.ArrowDownward else Icons.Rounded.ArrowUpward
            )
        )
    }
}

/** Each card gets its own take on the theme colour so the swipe reads as a set, not a copy. */
@Composable
private fun heroBrush(kind: HeroCardKind): Brush {
    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    return when (kind) {
        HeroCardKind.MONTH -> Brush.linearGradient(
            listOf(primary, lerp(primary, Color.Black, 0.28f))
        )
        HeroCardKind.LEND_OWE -> Brush.linearGradient(
            colors = listOf(lerp(primary, Color.Black, 0.10f), lerp(primary, Color.Black, 0.42f)),
            start = Offset(Float.POSITIVE_INFINITY, 0f),
            end = Offset(0f, Float.POSITIVE_INFINITY)
        )
        HeroCardKind.TODAY -> {
            val base = lerp(primary, tertiary, 0.30f)
            Brush.linearGradient(listOf(base, lerp(base, Color.Black, 0.30f)))
        }
        HeroCardKind.TOP_SPENDING -> Brush.verticalGradient(
            listOf(lerp(primary, Color.Black, 0.06f), lerp(primary, Color.Black, 0.38f))
        )
        HeroCardKind.VS_LAST_MONTH -> {
            val base = lerp(primary, tertiary, 0.15f)
            Brush.linearGradient(
                colors = listOf(lerp(base, Color.Black, 0.08f), lerp(base, Color.Black, 0.40f)),
                start = Offset(Float.POSITIVE_INFINITY, 0f),
                end = Offset(0f, Float.POSITIVE_INFINITY)
            )
        }
    }
}

/**
 * The card leaves like a page being turned: it hinges on the edge nearest the next card, tilts
 * away, shrinks and fades. The card arriving does the same thing in reverse, so it swings in
 * and settles flat. Everything is read while drawing, so swiping never recomposes anything.
 */
private fun Modifier.heroCardTransition(pagerState: PagerState, page: Int): Modifier = graphicsLayer {
    // Positive: this card is to the left (leaving). Negative: to the right (arriving).
    val position = pagerState.currentPage - page + pagerState.currentPageOffsetFraction
    val distance = position.absoluteValue.coerceIn(0f, 1f)
    val eased = 1f - (1f - distance) * (1f - distance)
    cameraDistance = 14f * density
    transformOrigin = TransformOrigin(if (position > 0f) 1f else 0f, 0.5f)
    rotationY = position.coerceIn(-1f, 1f) * 14f
    val scale = 1f - 0.08f * eased
    scaleX = scale
    scaleY = scale
    alpha = 1f - 0.55f * eased
}

/** The text and tiles drift a little faster than the card and fade out sooner, which gives it depth. */
private fun Modifier.heroContentParallax(pagerState: PagerState, page: Int): Modifier = graphicsLayer {
    val position = pagerState.currentPage - page + pagerState.currentPageOffsetFraction
    translationX = -position * size.width * 0.12f
    alpha = (1f - position.absoluteValue * 1.3f).coerceIn(0f, 1f)
}

/**
 * A row of full cards you swipe between: the whole card slides away and the next one slides in
 * from the right. Tapping a tile opens History filtered to that type.
 */
@Composable
private fun BalanceHeroCard(
    cards: List<HeroCardKind>,
    summary: MonthSummary,
    lendOwe: LendOweSummary,
    extras: HeroExtras,
    month: YearMonth,
    onMonthChange: (YearMonth) -> Unit,
    animationStyle: CardAnimationStyle,
    onStatClick: (TransactionType, YearMonth) -> Unit
) {
    val visible by rememberUpdatedState(cards.ifEmpty { listOf(HeroCardKind.MONTH) })
    val pagerState = rememberPagerState(pageCount = { visible.size })
    val scope = rememberCoroutineScope()
    // One clock shared by every card, so the animation stays in step while swiping
    val clock = if (animationStyle != CardAnimationStyle.NONE) rememberHeroClock() else null

    Column {
        HorizontalPager(
            state = pagerState,
            pageSpacing = 16.dp,
            beyondViewportPageCount = 1,
            userScrollEnabled = visible.size > 1,
            key = { index -> visible.getOrNull(index)?.name ?: index },
            flingBehavior = PagerDefaults.flingBehavior(
                state = pagerState,
                snapAnimationSpec = spring(dampingRatio = 0.8f, stiffness = 420f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            val kind = visible.getOrElse(page) { HeroCardKind.MONTH }
            HeroCard(
                kind = kind,
                spec = heroPageFor(kind, summary, lendOwe, extras, month),
                month = month,
                onMonthChange = onMonthChange,
                animationStyle = animationStyle,
                clock = clock,
                onStatClick = { stat -> stat.type?.let { onStatClick(it, stat.month ?: month) } },
                modifier = Modifier.heroCardTransition(pagerState, page),
                contentModifier = Modifier.heroContentParallax(pagerState, page)
            )
        }

        if (visible.size > 1) {
            Spacer(Modifier.height(4.dp))
            HeroPageDots(
                pagerState = pagerState,
                onDotClick = { index ->
                    scope.launch {
                        pagerState.animateScrollToPage(
                            page = index,
                            animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f)
                        )
                    }
                }
            )
        }
    }
}

/** One full card: its own gradient, animated background and content. */
@Composable
private fun HeroCard(
    kind: HeroCardKind,
    spec: HeroPageSpec,
    month: YearMonth,
    onMonthChange: (YearMonth) -> Unit,
    animationStyle: CardAnimationStyle,
    clock: State<Float>?,
    onStatClick: (HeroStatSpec) -> Unit,
    modifier: Modifier = Modifier,
    contentModifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(30.dp))
            .background(heroBrush(kind))
    ) {
        HeroCardBackground(
            style = animationStyle,
            clock = clock,
            modifier = Modifier.matchParentSize()
        )

        Column(modifier = contentModifier.padding(20.dp)) {
            HeroHeader(
                title = spec.title,
                trailingLabel = spec.trailingLabel,
                month = month,
                onMonthChange = onMonthChange
            )
            Spacer(Modifier.height(10.dp))
            HeroPageContent(spec = spec, onStatClick = onStatClick)
        }
    }
}

private fun lendOweSubtitle(lendOwe: LendOweSummary): String {
    val parts = buildList {
        if (lendOwe.peopleOweYou > 0) {
            add(if (lendOwe.peopleOweYou == 1) "1 person owes you" else "${lendOwe.peopleOweYou} people owe you")
        }
        if (lendOwe.youOwePeople > 0) {
            add(if (lendOwe.youOwePeople == 1) "you owe 1 person" else "you owe ${lendOwe.youOwePeople} people")
        }
    }
    return if (parts.isEmpty()) "Nothing outstanding right now" else parts.joinToString(" · ")
}

/**
 * Title on the left, month switcher on the right. On cards that aren't about the selected month
 * the switcher stays in place, invisible, so every card keeps exactly the same height, and
 * [trailingLabel] sits over it.
 */
@Composable
private fun HeroHeader(
    title: String,
    trailingLabel: String?,
    month: YearMonth,
    onMonthChange: (YearMonth) -> Unit
) {
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = onPrimary.copy(alpha = 0.8f),
            modifier = Modifier.weight(1f)
        )
        Box(contentAlignment = Alignment.Center) {
            Surface(
                shape = CircleShape,
                color = onPrimary.copy(alpha = 0.14f),
                modifier = Modifier.graphicsLayer { alpha = if (trailingLabel == null) 1f else 0f }
            ) {
                CompositionLocalProvider(LocalContentColor provides onPrimary) {
                    MonthSwitcherOnColor(month = month, onMonthChange = onMonthChange)
                }
            }
            if (trailingLabel != null) {
                Surface(
                    shape = CircleShape,
                    color = onPrimary.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = trailingLabel,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = onPrimary,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
                // Swallows taps so the invisible month switcher underneath can't be hit by accident.
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        )
                )
            }
        }
    }
}

/** The big number, one line of context and two tiles. Tiles with a type open History when tapped. */
@Composable
private fun HeroPageContent(
    spec: HeroPageSpec,
    onStatClick: (HeroStatSpec) -> Unit
) {
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    Column {
        AnimatedAmountText(
            amount = kotlin.math.abs(spec.amount),
            prefix = if (spec.amount < 0) "−" else "",
            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
            color = onPrimary
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = spec.subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = onPrimary.copy(alpha = 0.8f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(Modifier.height(18.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HeroStat(
                label = spec.first.label,
                amount = spec.first.amount,
                icon = spec.first.icon,
                modifier = Modifier.weight(1f),
                onClick = spec.first.type?.let { { onStatClick(spec.first) } }
            )
            HeroStat(
                label = spec.second.label,
                amount = spec.second.amount,
                icon = spec.second.icon,
                modifier = Modifier.weight(1f),
                onClick = spec.second.type?.let { { onStatClick(spec.second) } }
            )
        }
    }
}

/** Page dots under the cards. The active one stretches into a pill and follows your finger; tap one to jump. */
@Composable
private fun HeroPageDots(pagerState: PagerState, onDotClick: (Int) -> Unit) {
    val dotColor = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pagerState.pageCount) { index ->
            val closeness = (1f - (pagerState.currentPage + pagerState.currentPageOffsetFraction - index).absoluteValue)
                .coerceIn(0f, 1f)
            // The padding makes a bigger tap target than the 6dp dot itself.
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onDotClick(index) }
                    )
                    .padding(horizontal = 3.dp, vertical = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .height(6.dp)
                        .width((6f + 12f * closeness).dp)
                        .clip(CircleShape)
                        .background(dotColor.copy(alpha = 0.30f + 0.55f * closeness))
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
private fun HeroStat(
    label: String,
    amount: Double,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
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