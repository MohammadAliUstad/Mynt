package com.yourname.expensetracker

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Type filter options shown as round icon chips at the top of the Transactions screen.
 */
private enum class TypeFilter {
    ALL, INCOME, EXPENSE, LENT, BORROWED
}

/**
 * Transactions Screen: view-only history of everything entered by voice or manual entry.
 * Type chips, category chips, month selector, search and a tap-to-edit list.
 */
@Composable
fun TransactionsScreen(
    expenses: List<Expense>,
    onUpdateTransaction: (Expense) -> Unit = {},
    onDeleteTransaction: (Expense) -> Unit = {}
) {
    val focusManager = LocalFocusManager.current

    var selectedCalendar by remember { mutableStateOf(Calendar.getInstance()) }
    var showMonthPicker by remember { mutableStateOf(false) }
    var typeFilter by remember { mutableStateOf(TypeFilter.ALL) }
    var categoryFilter by remember { mutableStateOf<String?>(null) }
    var searchOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var menuOpen by remember { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<Expense?>(null) }

    val monthYearFormatter = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
    val currentMonthYear = monthYearFormatter.format(selectedCalendar.time)

    val filtered = expenses.filter { tx ->
        val matchesMonth = tx.monthYear.equals(currentMonthYear, ignoreCase = true)
        val matchesType = when (typeFilter) {
            TypeFilter.ALL -> true
            TypeFilter.INCOME -> tx.type == TransactionType.INCOME
            TypeFilter.EXPENSE -> tx.type == TransactionType.EXPENSE
            TypeFilter.LENT -> tx.type == TransactionType.LENT
            TypeFilter.BORROWED -> tx.type == TransactionType.BORROWED
        }
        val matchesCategory = categoryFilter == null || tx.category.equals(categoryFilter, ignoreCase = true)
        val matchesSearch = searchQuery.isBlank() ||
                tx.title.contains(searchQuery, ignoreCase = true) ||
                tx.category.contains(searchQuery, ignoreCase = true) ||
                tx.personName.contains(searchQuery, ignoreCase = true)

        matchesMonth && matchesType && matchesCategory && matchesSearch
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // 1. TOP ROW: Title (left) + Search and menu (right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Transactions",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = {
                        searchOpen = !searchOpen
                        if (!searchOpen) {
                            searchQuery = ""
                            focusManager.clearFocus(force = true)
                        }
                    }) {
                        Icon(
                            imageVector = if (searchOpen) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        DropdownMenu(
                            expanded = menuOpen,
                            onDismissRequest = { menuOpen = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Clear filters") },
                                onClick = {
                                    typeFilter = TypeFilter.ALL
                                    categoryFilter = null
                                    searchQuery = ""
                                    searchOpen = false
                                    menuOpen = false
                                }
                            )
                        }
                    }
                }
            }

            // 2. SEARCH FIELD (only when search is open)
            if (searchOpen) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search transactions") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. TYPE FILTER CHIPS: All / Income / Expenses / Lent / Borrowed
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TypeFilterItem(
                    label = "All",
                    icon = Icons.Default.GridView,
                    tint = MaterialTheme.colorScheme.primary,
                    selected = typeFilter == TypeFilter.ALL,
                    onClick = { typeFilter = TypeFilter.ALL }
                )
                TypeFilterItem(
                    label = "Income",
                    icon = Icons.Default.ArrowDownward,
                    tint = Color(0xFF43A047),
                    selected = typeFilter == TypeFilter.INCOME,
                    onClick = { typeFilter = TypeFilter.INCOME }
                )
                TypeFilterItem(
                    label = "Expenses",
                    icon = Icons.Default.ArrowUpward,
                    tint = Color(0xFFE53935),
                    selected = typeFilter == TypeFilter.EXPENSE,
                    onClick = { typeFilter = TypeFilter.EXPENSE }
                )
                TypeFilterItem(
                    label = "Lent",
                    icon = Icons.AutoMirrored.Filled.ArrowForward,
                    tint = Color(0xFFFF9800),
                    selected = typeFilter == TypeFilter.LENT,
                    onClick = { typeFilter = TypeFilter.LENT }
                )
                TypeFilterItem(
                    label = "Borrowed",
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    tint = Color(0xFF42A5F5),
                    selected = typeFilter == TypeFilter.BORROWED,
                    onClick = { typeFilter = TypeFilter.BORROWED }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 4. CATEGORY CHIPS (tap again to clear)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                expenseCategories.forEach { category ->
                    CategoryFilterItem(
                        category = category,
                        selected = categoryFilter.equals(category, ignoreCase = true),
                        onClick = {
                            categoryFilter = if (categoryFilter.equals(category, ignoreCase = true)) null else category
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 5. MONTH SELECTOR
            MonthYearSelectorButton(
                selectedCalendar = selectedCalendar,
                onClick = { showMonthPicker = true }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 6. TRANSACTION LIST
            if (filtered.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Text(
                        text = "No transactions found",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 120.dp) // Keeps last row clear of the floating bottom nav bar
                ) {
                    items(filtered, key = { it.id }) { tx ->
                        TransactionsListRow(
                            transaction = tx,
                            onClick = {
                                // Edit dialog only offers expense/income categories
                                if (tx.type == TransactionType.EXPENSE || tx.type == TransactionType.INCOME) {
                                    editingTransaction = tx
                                }
                            }
                        )
                    }
                }
            }
        }

        // MONTH/YEAR PICKER DIALOG
        if (showMonthPicker) {
            MonthYearPickerDialog(
                selectedCalendar = selectedCalendar,
                onMonthYearSelected = { newCalendar ->
                    selectedCalendar = newCalendar
                    showMonthPicker = false
                },
                onDismissRequest = { showMonthPicker = false }
            )
        }

        // EDIT / DELETE TRANSACTION DIALOG
        editingTransaction?.let { tx ->
            EditTransactionDialog(
                transaction = tx,
                onDismissRequest = {
                    editingTransaction = null
                    focusManager.clearFocus(force = true)
                },
                onSaveTransaction = { updatedTransaction ->
                    editingTransaction = null
                    focusManager.clearFocus(force = true)
                    onUpdateTransaction(updatedTransaction)
                },
                onDeleteTransaction = { deletedTransaction ->
                    editingTransaction = null
                    focusManager.clearFocus(force = true)
                    onDeleteTransaction(deletedTransaction)
                }
            )
        }
    }
}

/**
 * Round icon chip with a label underneath. Filled with its tint when selected.
 */
@Composable
private fun TypeFilterItem(
    label: String,
    icon: ImageVector,
    tint: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(if (selected) tint else tint.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) Color.White else tint,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

/**
 * Small round category chip (emoji + label). Outlined when selected.
 */
@Composable
private fun CategoryFilterItem(
    category: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val (emoji, bgColor) = getCategoryIconAndColor(category, TransactionType.EXPENSE)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(bgColor)
                .border(
                    width = if (selected) 2.dp else 0.dp,
                    color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(text = emoji, fontSize = 20.sp)
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = category,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

/**
 * One transaction row: category icon, title, subtitle and signed amount.
 */
@Composable
private fun TransactionsListRow(
    transaction: Expense,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    val (emoji, iconBg) = getCategoryIconAndColor(transaction.category, transaction.type)

    val isPositive = transaction.type == TransactionType.INCOME || transaction.type == TransactionType.LENT
    val amountColor = if (isPositive) Color(0xFF43A047) else Color(0xFFE53935)
    val sign = if (isPositive) "+" else "-"

    val title = if (
        (transaction.type == TransactionType.LENT || transaction.type == TransactionType.BORROWED) &&
        transaction.personName.isNotBlank()
    ) transaction.personName else transaction.title

    val subtitle = if (transaction.type == TransactionType.EXPENSE) {
        transaction.date
    } else {
        "${transaction.category} • ${transaction.date}"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable { onClick() },
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Text(text = emoji, fontSize = 20.sp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "$sign${SettingsManager.formatAmount(transaction.amount)}",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = amountColor
            )
        }
    }
}