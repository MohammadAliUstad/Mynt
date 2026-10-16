package com.yourname.expensetracker

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.yourname.expensetracker.navigation.AppNavHost

/**
 * Root Composable invoking AppNavHost for top-level navigation.
 */
@Composable
fun ExpenseTrackerRoot() {
    val navController = rememberNavController()
    AppNavHost(navController = navController)
}

/**
 * Main application content with real-time Cloud Firestore database sync,
 * system back navigation handling, floating bottom navigation bar,
 * Settings screen slide/fade transition, dedicated Theme settings transition, and full-screen voice overlay.
 */
@Composable
fun ExpenseTrackerMainContent(
    onSignOut: () -> Unit
) {
    // Shared in-memory and cloud-synced list of expenses & incomes
    val expenses = remember { mutableStateListOf<Expense>() }

    // Logged-in user ID
    val userId = AuthManager.currentUser?.uid

    // Synchronize transactions with Cloud Firestore in real-time
    DisposableEffect(userId) {
        if (userId != null) {
            val listener = FirestoreRepository.listenToUserTransactions(userId) { cloudList ->
                expenses.clear()
                expenses.addAll(cloudList)
            }
            onDispose {
                listener.remove()
            }
        } else {
            onDispose { }
        }
    }

    // Navigation tab state
    var selectedTab by remember { mutableStateOf(NavTab.HOME) }

    // Voice Screen overlay visibility
    var showVoiceOverlay by remember { mutableStateOf(false) }

    // Settings Screen visibility
    var showSettingsScreen by remember { mutableStateOf(false) }

    // Dedicated Theme Settings Screen visibility
    var showThemeSettingsScreen by remember { mutableStateOf(false) }

    // Lent & Borrowed Screen visibility
    var showLentBorrowedScreen by remember { mutableStateOf(false) }

    // Add Expense (manual entry) Screen visibility
    var showAddExpenseScreen by remember { mutableStateOf(false) }

    // Dedicated Back Handlers for Device System Back Button
    // 00. Close Add Expense Screen overlay
    BackHandler(enabled = showAddExpenseScreen) {
        showAddExpenseScreen = false
    }

    // 0. Close Lent & Borrowed Screen overlay
    BackHandler(enabled = showLentBorrowedScreen) {
        showLentBorrowedScreen = false
    }

    // 1. Close Theme Settings Screen overlay
    BackHandler(enabled = showThemeSettingsScreen) {
        showThemeSettingsScreen = false
    }

    // 2. Close Settings Screen overlay
    BackHandler(enabled = showSettingsScreen && !showThemeSettingsScreen) {
        showSettingsScreen = false
    }

    // 3. Close Voice Screen overlay
    BackHandler(enabled = showVoiceOverlay) {
        showVoiceOverlay = false
    }

    // 4. Return to Home Tab if on Add, Stats, or Me tabs
    BackHandler(enabled = selectedTab != NavTab.HOME && !showSettingsScreen && !showThemeSettingsScreen && !showVoiceOverlay) {
        selectedTab = NavTab.HOME
    }

    // Index mapping for tab animation direction
    val tabIndices = mapOf(
        NavTab.HOME to 0,
        NavTab.ADD to 1,
        NavTab.STATS to 2,
        NavTab.ME to 3
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Tab Content with direction-aware slide & fade animations
            AnimatedContent(
                targetState = selectedTab,
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                transitionSpec = {
                    val initialIndex = tabIndices[initialState] ?: 0
                    val targetIndex = tabIndices[targetState] ?: 0

                    if (targetIndex > initialIndex) {
                        slideInHorizontally(tween(300)) { width -> width } + fadeIn(tween(300)) togetherWith
                                slideOutHorizontally(tween(300)) { width -> -width } + fadeOut(tween(300))
                    } else {
                        slideInHorizontally(tween(300)) { width -> -width } + fadeIn(tween(300)) togetherWith
                                slideOutHorizontally(tween(300)) { width -> width } + fadeOut(tween(300))
                    }
                },
                label = "tabContentAnimation"
            ) { targetTab ->
                when (targetTab) {
                    NavTab.HOME -> HomeScreen(
                        expenses = expenses,
                        userName = AuthManager.userDisplayName,
                        onAddTransactionClick = { showAddExpenseScreen = true },
                        onLentBorrowedClick = { showLentBorrowedScreen = true },
                        onUpdateTransaction = { updatedTransaction ->
                            val index = expenses.indexOfFirst { it.id == updatedTransaction.id }
                            if (index != -1) {
                                expenses[index] = updatedTransaction
                            }
                            if (userId != null) {
                                FirestoreRepository.addTransaction(userId, updatedTransaction)
                            }
                        },
                        onDeleteTransaction = { deletedTransaction ->
                            expenses.removeIf { it.id == deletedTransaction.id }
                            if (userId != null) {
                                FirestoreRepository.deleteTransaction(userId, deletedTransaction.id)
                            }
                        }
                    )

                    NavTab.ADD -> TransactionsScreen(
                        expenses = expenses,
                        onUpdateTransaction = { updatedTransaction ->
                            val index = expenses.indexOfFirst { it.id == updatedTransaction.id }
                            if (index != -1) {
                                expenses[index] = updatedTransaction
                            }
                            if (userId != null) {
                                FirestoreRepository.addTransaction(userId, updatedTransaction)
                            }
                        },
                        onDeleteTransaction = { deletedTransaction ->
                            expenses.removeIf { it.id == deletedTransaction.id }
                            if (userId != null) {
                                FirestoreRepository.deleteTransaction(userId, deletedTransaction.id)
                            }
                        }
                    )

                    NavTab.STATS -> StatsScreen(expenses = expenses)
                    NavTab.ME -> ProfileScreen(
                        onSignOutClick = onSignOut,
                        onNavigateToSettings = { showSettingsScreen = true },
                        onNavigateToInsights = { selectedTab = NavTab.STATS }
                    )
                }
            }

            // Custom Floating Bottom Navigation Bar
            BottomNavBar(
                selectedTab = selectedTab,
                onTabSelected = { tab -> selectedTab = tab },
                onMicClick = { showVoiceOverlay = true },
                modifier = Modifier.align(Alignment.BottomCenter)
            )

            // Add Expense (manual entry) Screen Overlay with slide from/to right + fade animation
            AnimatedVisibility(
                visible = showAddExpenseScreen,
                enter = slideInHorizontally(tween(300)) { width -> width } + fadeIn(tween(300)),
                exit = slideOutHorizontally(tween(300)) { width -> width } + fadeOut(tween(300))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    AddExpenseScreen(
                        onBackClick = { showAddExpenseScreen = false },
                        onSaveExpense = { newTransaction ->
                            if (userId != null) {
                                FirestoreRepository.addTransaction(userId, newTransaction)
                            } else {
                                expenses.add(0, newTransaction)
                            }
                            showAddExpenseScreen = false
                        }
                    )
                }
            }

            // Lent & Borrowed Screen Overlay with slide from/to right + fade animation
            AnimatedVisibility(
                visible = showLentBorrowedScreen,
                enter = slideInHorizontally(tween(300)) { width -> width } + fadeIn(tween(300)),
                exit = slideOutHorizontally(tween(300)) { width -> width } + fadeOut(tween(300))
            ) {
                LentBorrowedScreen(
                    expenses = expenses,
                    onBackClick = { showLentBorrowedScreen = false },
                    onTransactionClick = { }
                )
            }

            // Settings Screen Overlay with slide from/to right + fade animation
            AnimatedVisibility(
                visible = showSettingsScreen,
                enter = slideInHorizontally(tween(300)) { width -> width } + fadeIn(tween(300)),
                exit = slideOutHorizontally(tween(300)) { width -> width } + fadeOut(tween(300))
            ) {
                SettingsScreen(
                    onBackClick = { showSettingsScreen = false },
                    onNavigateToThemeSettings = { showThemeSettingsScreen = true }
                )
            }

            // Dedicated Theme Settings Screen Overlay
            AnimatedVisibility(
                visible = showThemeSettingsScreen,
                enter = slideInHorizontally(tween(300)) { width -> width } + fadeIn(tween(300)),
                exit = slideOutHorizontally(tween(300)) { width -> width } + fadeOut(tween(300))
            ) {
                ThemeSettingsScreen(
                    onBackClick = { showThemeSettingsScreen = false }
                )
            }

            // Full-screen Voice Overlay with slide up/down animation
            AnimatedVisibility(
                visible = showVoiceOverlay,
                enter = slideInVertically(tween(300)) { height -> height } + fadeIn(tween(300)),
                exit = slideOutVertically(tween(300)) { height -> height } + fadeOut(tween(300))
            ) {
                VoiceScreen(
                    onCloseClick = { showVoiceOverlay = false },
                    onSaveTransaction = { newTransaction ->
                        if (userId != null) {
                            FirestoreRepository.addTransaction(userId, newTransaction)
                        } else {
                            expenses.add(0, newTransaction)
                        }
                        showVoiceOverlay = false
                    }
                )
            }
        }
    }
}