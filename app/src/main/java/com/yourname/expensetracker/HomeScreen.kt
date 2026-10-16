package com.yourname.expensetracker

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yourname.expensetracker.ui.theme.ExpenseTrackerTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Home Screen: month selector + bell, greeting, Overview card (donut with Net),
 * 2x2 quick action tiles, Import Documents card, and Recent / Full Transactions list.
 */
@Composable
fun HomeScreen(
    expenses: List<Expense> = emptyList(),
    userName: String = "User",
    onAddTransactionClick: () -> Unit = {},
    onLentBorrowedClick: () -> Unit = {},
    onUpdateTransaction: (Expense) -> Unit = {},
    onDeleteTransaction: (Expense) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current

    // Selected Calendar month & year state (defaults to current month)
    var selectedCalendar by remember { mutableStateOf(Calendar.getInstance()) }

    // Dialog state for Month/Year Picker
    var showMonthPicker by remember { mutableStateOf(false) }

    // State for History downward expansion
    var isHistoryExpanded by remember { mutableStateOf(false) }

    // Back Handler to collapse when expanded
    BackHandler(enabled = isHistoryExpanded) {
        isHistoryExpanded = false
    }

    // Currently edited transaction state
    var editingTransaction by remember { mutableStateOf<Expense?>(null) }

    // Date formatter for matching monthYear string (e.g., "October 2026")
    val monthYearFormatter = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
    val currentMonthYear = monthYearFormatter.format(selectedCalendar.time)

    // Calculate totals and filtered transactions for the selected month
    val monthExpenses = expenses.filter {
        it.monthYear.equals(currentMonthYear, ignoreCase = true) && it.type == TransactionType.EXPENSE
    }
    val monthIncomes = expenses.filter {
        it.monthYear.equals(currentMonthYear, ignoreCase = true) && it.type == TransactionType.INCOME
    }

    val totalSpent = monthExpenses.sumOf { it.amount }
    val totalEarned = monthIncomes.sumOf { it.amount }

    // Combined recent transactions for selected month (newest first)
    val monthTransactions = expenses.filter {
        it.monthYear.equals(currentMonthYear, ignoreCase = true)
    }

    // Greeting based on time of day
    val greeting = remember {
        when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 0..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            else -> "Good Evening"
        }
    }

    fun showComingSoon(message: String) {
        coroutineScope.launch { snackbarHostState.showSnackbar(message) }
    }

    // File picker for Import Documents (PDF / CSV statements)
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = getFileNameFromUri(context, uri) ?: "Selected statement"
            showComingSoon("$fileName: Import is coming soon")
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    actionColor = MaterialTheme.colorScheme.primary,
                    dismissActionContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 110.dp) // Ensures last item is not hidden by floating bottom nav bar
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // 1. TOP ROW: Month/Year selector (left) + Notification bell (right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MonthYearSelectorButton(
                    selectedCalendar = selectedCalendar,
                    onClick = { showMonthPicker = true }
                )

                IconButton(
                    onClick = { showComingSoon("Notifications are coming soon") },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notifications",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 2. GREETING
            Text(
                text = "$greeting,",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f)
            )
            Text(
                text = "$userName 👋",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Stay on top of your finances",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 3. OVERVIEW CARD (Income / Expenses + donut with Net inside)
            HomeOverviewCard(
                totalIncome = totalEarned,
                totalExpense = totalSpent
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 4. QUICK ACTION TILES (2 x 2)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionTile(
                    emoji = "➕",
                    label = "Add Transaction",
                    tint = Color(0xFF7E57C2),
                    onClick = onAddTransactionClick,
                    modifier = Modifier.weight(1f)
                )
                // Lent & Borrowed action tile
                QuickActionTile(
                    emoji = "🤝",
                    label = "Lent & Borrowed",
                    tint = Color(0xFFFF9800),
                    onClick = onLentBorrowedClick,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionTile(
                    emoji = "🧮",
                    label = "Calculator",
                    tint = Color(0xFF42A5F5),
                    onClick = { showComingSoon("Calculator is coming soon") },
                    modifier = Modifier.weight(1f)
                )
                QuickActionTile(
                    emoji = "📝",
                    label = "Manual Entry",
                    tint = Color(0xFF26A69A),
                    onClick = onAddTransactionClick,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 5. IMPORT DOCUMENTS CARD (opens the file picker)
            ImportDocumentsCard(
                onClick = {
                    filePickerLauncher.launch(
                        arrayOf("application/pdf", "text/csv", "text/comma-separated-values")
                    )
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 6. TRANSACTIONS SECTION TITLE WITH SWIFT TEXT ANIMATION
            AnimatedContent(
                targetState = isHistoryExpanded,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                },
                label = "sectionTitleAnimation"
            ) { expanded ->
                Text(
                    text = if (expanded) "Full Transaction" else "Recent Transactions",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 7. RECENT / FULL TRANSACTIONS CARD WITH DOWNWARD EXPANSION
            RecentTransactionsCard(
                transactions = monthTransactions,
                isExpanded = isHistoryExpanded,
                onToggleHistory = {
                    isHistoryExpanded = !isHistoryExpanded
                },
                onTransactionClick = { transaction ->
                    editingTransaction = transaction
                }
            )
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
        if (editingTransaction != null) {
            EditTransactionDialog(
                transaction = editingTransaction!!,
                onDismissRequest = {
                    editingTransaction = null
                    focusManager.clearFocus(force = true)
                },
                onSaveTransaction = { updatedTransaction ->
                    editingTransaction = null
                    focusManager.clearFocus(force = true)
                    onUpdateTransaction(updatedTransaction)
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Transaction updated!")
                    }
                },
                onDeleteTransaction = { deletedTransaction ->
                    editingTransaction = null
                    focusManager.clearFocus(force = true)
                    onDeleteTransaction(deletedTransaction)
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Transaction deleted!")
                    }
                }
            )
        }
    }
}

/**
 * Small rounded tile with a colored emoji badge, label and chevron.
 */
@Composable
private fun QuickActionTile(
    emoji: String,
    label: String,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(18.dp)

    Card(
        modifier = modifier
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
                .padding(horizontal = 12.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(tint.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = emoji, fontSize = 20.sp)
            }

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = "›",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

/**
 * Full-width Import Documents card.
 */
@Composable
private fun ImportDocumentsCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(18.dp)

    Card(
        modifier = modifier
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
                .padding(horizontal = 14.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "☁️", fontSize = 22.sp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Import Documents",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Upload bank statements, bills, receipts and more",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            Text(
                text = "›",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

/**
 * Extracts human-readable file name from SAF content Uri.
 */
private fun getFileNameFromUri(context: Context, uri: Uri): String? {
    var result: String? = null
    if (uri.scheme == "content") {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    result = cursor.getString(index)
                }
            }
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/') ?: -1
        if (cut != -1) {
            result = result?.substring(cut + 1)
        }
    }
    return result
}

@Preview(showBackground = true, name = "Home Screen Redesign Preview")
@Composable
fun HomeScreenPreview() {
    ExpenseTrackerTheme {
        HomeScreen(expenses = emptyList())
    }
}