package com.yugentech.mynt.ui.tabs.historyScreen.parent

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yugentech.mynt.database.model.Expense
import com.yugentech.mynt.database.model.TransactionType
import com.yugentech.mynt.database.model.expenseCategories
import com.yugentech.mynt.database.model.incomeCategories
import com.yugentech.mynt.transactions.insights.FinanceInsights
import com.yugentech.mynt.transactions.insights.TransactionDates
import com.yugentech.mynt.transactions.insights.localDate
import com.yugentech.mynt.transactions.insights.sortedByDateDescending
import com.yugentech.mynt.ui.shared.components.EmptyState
import com.yugentech.mynt.ui.shared.components.MonthSwitcher
import com.yugentech.mynt.ui.shared.components.MyntCard
import com.yugentech.mynt.ui.shared.components.SwipeableTransactionRow
import com.yugentech.mynt.ui.shared.components.monthTitle
import com.yugentech.mynt.ui.shared.components.isInflow
import com.yugentech.mynt.ui.shared.util.FinanceColors
import com.yugentech.mynt.ui.shared.util.formatAmountShort
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Asks the History screen to open on a particular type and month, e.g. when you tap "Income" on
 * the Home card. [nonce] makes two identical taps in a row count as two separate requests.
 */
data class TransactionsFilterRequest(
    val type: TransactionType,
    val month: YearMonth,
    val nonce: Long = System.nanoTime()
)

private enum class TypeFilter(val label: String, val type: TransactionType?) {
    ALL("All", null),
    EXPENSE("Spent", TransactionType.EXPENSE),
    INCOME("Income", TransactionType.INCOME),
    LENT("Lent", TransactionType.LENT),
    BORROWED("Borrowed", TransactionType.BORROWED)
}

private enum class SortOrder(val label: String) {
    NEWEST("Newest first"),
    OLDEST("Oldest first"),
    HIGHEST("Highest amount")
}

/**
 * Every entry, a month at a time and grouped by day. Searching looks across all months,
 * since "when did I last pay the plumber?" rarely knows the month.
 */
@Composable
fun TransactionsScreen(
    expenses: List<Expense>,
    bottomPadding: Dp,
    onTransactionClick: (Expense) -> Unit,
    onDeleteTransaction: (Expense) -> Unit,
    onAddClick: () -> Unit,
    filterRequest: TransactionsFilterRequest? = null,
    onFilterRequestConsumed: () -> Unit = {}
) {
    val focusManager = LocalFocusManager.current

    var month by rememberSaveable { mutableStateOf(YearMonth.now()) }
    var typeFilter by rememberSaveable { mutableStateOf(TypeFilter.ALL) }
    var categoryFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var sortOrder by rememberSaveable { mutableStateOf(SortOrder.NEWEST) }
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var sortMenuOpen by remember { mutableStateOf(false) }

    // Applies a filter requested from elsewhere (the Home card), then tells the caller it's been used
    // so coming back to this tab later doesn't reset whatever you've changed since.
    LaunchedEffect(filterRequest) {
        filterRequest?.let { request ->
            typeFilter = TypeFilter.entries.first { it.type == request.type }
            categoryFilter = null
            month = request.month
            query = ""
            searchOpen = false
            onFilterRequestConsumed()
        }
    }

    val isSearching = query.isNotBlank()

    val filtered = remember(expenses, month, typeFilter, categoryFilter, sortOrder, query) {
        val base = if (isSearching) expenses else FinanceInsights.inMonth(expenses, month)
        val matches = base.filter { tx ->
            (typeFilter.type == null || tx.type == typeFilter.type) &&
                    (categoryFilter == null || tx.category.equals(categoryFilter, ignoreCase = true)) &&
                    (!isSearching || tx.matches(query))
        }
        when (sortOrder) {
            SortOrder.NEWEST -> matches.sortedByDateDescending()
            SortOrder.OLDEST -> matches.sortedByDateDescending().reversed()
            SortOrder.HIGHEST -> matches.sortedByDescending { it.amount }
        }
    }

    val inflow = filtered.filter { it.type.isInflow() }.sumOf { it.amount }
    val outflow = filtered.filterNot { it.type.isInflow() }.sumOf { it.amount }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Spacer(Modifier.height(12.dp))

        // Title row, or the search field in its place
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (searchOpen) {
                SearchField(
                    query = query,
                    onQueryChange = { query = it },
                    onClose = {
                        query = ""
                        searchOpen = false
                        focusManager.clearFocus(force = true)
                    },
                    modifier = Modifier.weight(1f)
                )
            } else {
                Text(
                    text = "History",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { searchOpen = true }) {
                    Icon(Icons.Rounded.Search, contentDescription = "Search")
                }
            }
            Box {
                IconButton(onClick = { sortMenuOpen = true }) {
                    Icon(Icons.AutoMirrored.Rounded.Sort, contentDescription = "Sort")
                }
                DropdownMenu(
                    expanded = sortMenuOpen,
                    onDismissRequest = { sortMenuOpen = false },
                    shape = RoundedCornerShape(18.dp)
                ) {
                    SortOrder.entries.forEach { order ->
                        DropdownMenuItem(
                            text = { Text(order.label) },
                            trailingIcon = {
                                if (order == sortOrder) Icon(Icons.Rounded.Check, contentDescription = null)
                            },
                            onClick = {
                                sortOrder = order
                                sortMenuOpen = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        AnimatedVisibility(
            visible = !isSearching,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MonthSwitcher(month = month, onMonthChange = { month = it })
            }
        }

        Spacer(Modifier.height(12.dp))

        // Type filters, then the categories that belong to the chosen type
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(TypeFilter.entries) { filter ->
                FilterChip(
                    selected = typeFilter == filter,
                    onClick = {
                        typeFilter = filter
                        categoryFilter = null
                    },
                    label = { Text(filter.label) },
                    shape = CircleShape,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }

        val categoryOptions = when (typeFilter) {
            TypeFilter.EXPENSE -> expenseCategories
            TypeFilter.INCOME -> incomeCategories
            else -> emptyList()
        }
        AnimatedVisibility(
            visible = categoryOptions.isNotEmpty(),
            enter = expandVertically(tween(220, easing = FastOutSlowInEasing)) + fadeIn(),
            exit = shrinkVertically(tween(220, easing = FastOutSlowInEasing)) + fadeOut()
        ) {
            LazyRow(
                modifier = Modifier.padding(top = 8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categoryOptions) { category ->
                    FilterChip(
                        selected = categoryFilter == category,
                        onClick = { categoryFilter = if (categoryFilter == category) null else category },
                        label = { Text(category) },
                        shape = CircleShape
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // In / out for whatever is currently listed
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TotalChip("Money in", "+" + formatAmountShort(inflow), FinanceColors.income, Modifier.weight(1f))
            TotalChip("Money out", "−" + formatAmountShort(outflow), FinanceColors.expense, Modifier.weight(1f))
        }

        Spacer(Modifier.height(8.dp))

        if (filtered.isEmpty()) {
            EmptyState(
                icon = if (isSearching) Icons.Rounded.SearchOff else Icons.Rounded.Search,
                title = if (isSearching) "No matches for \"$query\"" else "Nothing here yet",
                message = if (isSearching) "Try a name, a category or an amount."
                else "Entries for ${monthTitle(month)} will show up here.",
                actionLabel = if (isSearching) null else "Add an entry",
                onAction = if (isSearching) null else onAddClick
            )
        } else {
            val grouped = if (sortOrder == SortOrder.HIGHEST) null else filtered.groupBy { it.localDate }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = bottomPadding),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (grouped == null) {
                    items(filtered, key = { it.id }) { tx ->
                        SwipeableTransactionRow(
                            expense = tx,
                            onClick = { onTransactionClick(tx) },
                            onDelete = { onDeleteTransaction(tx) },
                            modifier = Modifier.animateItem()
                        )
                    }
                } else {
                    grouped.forEach { (day, entries) ->
                        item(key = "header-$day") {
                            DayHeader(day = day, entries = entries, modifier = Modifier.animateItem())
                        }
                        items(entries, key = { it.id }) { tx ->
                            SwipeableTransactionRow(
                                expense = tx,
                                onClick = { onTransactionClick(tx) },
                                onDelete = { onDeleteTransaction(tx) },
                                showDate = isSearching && day.year != LocalDate.now().year,
                                modifier = Modifier.animateItem()
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun Expense.matches(query: String): Boolean {
    val q = query.trim()
    if (q.isEmpty()) return true
    // Also searches inside the hidden AI note (e.g. "office", "colleague", "online")!
    val textMatch = title.contains(q, ignoreCase = true) ||
            category.contains(q, ignoreCase = true) ||
            personName.contains(q, ignoreCase = true) ||
            note.contains(q, ignoreCase = true)
    // "250" also finds 250.00 and 1,250: handy when you remember the amount, not the name.
    val digits = q.filter { it.isDigit() }
    val amountMatch = digits.isNotEmpty() && amount.toLong().toString().contains(digits)
    return textMatch || amountMatch
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    TextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Search all entries") },
        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
        trailingIcon = {
            IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, contentDescription = "Close search") }
        },
        singleLine = true,
        shape = CircleShape,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ),
        modifier = modifier.focusRequester(focusRequester)
    )
}

@Composable
private fun TotalChip(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    MyntCard(modifier = modifier, contentPadding = 14.dp, shape = RoundedCornerShape(20.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = color)
    }
}

@Composable
private fun DayHeader(day: LocalDate, entries: List<Expense>, modifier: Modifier = Modifier) {
    val today = TransactionDates.today()
    val label = when (day) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> day.format(
            DateTimeFormatter.ofPattern(
                if (day.year == today.year) "EEEE, d MMM" else "EEE, d MMM yyyy",
                Locale.getDefault()
            )
        )
    }
    val spent = entries.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 2.dp, start = 4.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        if (spent > 0) {
            Text(
                text = "Spent ${formatAmountShort(spent)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(2.dp))
    }
}