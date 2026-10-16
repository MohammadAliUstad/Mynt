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

@Composable
fun ExpenseTrackerRoot() {
    val navController = rememberNavController()
    AppNavHost(navController = navController)
}

@Composable
fun ExpenseTrackerMainContent(
    onSignOut: () -> Unit
) {
    val expenses = remember { mutableStateListOf<Expense>() }
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

    var selectedTab by remember { mutableStateOf(NavTab.HOME) }
    var showVoiceOverlay by remember { mutableStateOf(false) }
    var showSettingsScreen by remember { mutableStateOf(false) }
    var showThemeSettingsScreen by remember { mutableStateOf(false) }
    var showLentBorrowedScreen by remember { mutableStateOf(false) }
    var showAddExpenseScreen by remember { mutableStateOf(false) }

    BackHandler(enabled = showAddExpenseScreen) {
        showAddExpenseScreen = false
    }

    BackHandler(enabled = showLentBorrowedScreen) {
        showLentBorrowedScreen = false
    }

    BackHandler(enabled = showThemeSettingsScreen) {
        showThemeSettingsScreen = false
    }

    BackHandler(enabled = showSettingsScreen && !showThemeSettingsScreen) {
        showSettingsScreen = false
    }

    BackHandler(enabled = showVoiceOverlay) {
        showVoiceOverlay = false
    }

    BackHandler(enabled = selectedTab != NavTab.HOME && !showSettingsScreen && !showThemeSettingsScreen && !showVoiceOverlay) {
        selectedTab = NavTab.HOME
    }

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
                            if (userId != null) {
                                FirestoreRepository.addTransaction(userId, updatedTransaction)
                            } else {
                                val index = expenses.indexOfFirst { it.id == updatedTransaction.id }
                                if (index != -1) expenses[index] = updatedTransaction
                            }
                        },
                        onDeleteTransaction = { deletedTransaction ->
                            if (userId != null) {
                                FirestoreRepository.deleteTransaction(userId, deletedTransaction.id)
                            } else {
                                expenses.removeIf { it.id == deletedTransaction.id }
                            }
                        }
                    )

                    NavTab.ADD -> TransactionsScreen(
                        expenses = expenses,
                        onUpdateTransaction = { updatedTransaction ->
                            if (userId != null) {
                                FirestoreRepository.addTransaction(userId, updatedTransaction)
                            } else {
                                val index = expenses.indexOfFirst { it.id == updatedTransaction.id }
                                if (index != -1) expenses[index] = updatedTransaction
                            }
                        },
                        onDeleteTransaction = { deletedTransaction ->
                            if (userId != null) {
                                FirestoreRepository.deleteTransaction(userId, deletedTransaction.id)
                            } else {
                                expenses.removeIf { it.id == deletedTransaction.id }
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

            BottomNavBar(
                selectedTab = selectedTab,
                onTabSelected = { tab -> selectedTab = tab },
                onMicClick = { showVoiceOverlay = true },
                modifier = Modifier.align(Alignment.BottomCenter)
            )

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

            AnimatedVisibility(
                visible = showThemeSettingsScreen,
                enter = slideInHorizontally(tween(300)) { width -> width } + fadeIn(tween(300)),
                exit = slideOutHorizontally(tween(300)) { width -> width } + fadeOut(tween(300))
            ) {
                ThemeSettingsScreen(
                    onBackClick = { showThemeSettingsScreen = false }
                )
            }

            AnimatedVisibility(
                visible = showVoiceOverlay,
                enter = slideInVertically(tween(300)) { height -> height } + fadeIn(tween(300)),
                exit = slideOutVertically(tween(300)) { height -> height } + fadeOut(tween(300))
            ) {
                VoiceScreen(
                    onCloseClick = { showVoiceOverlay = false },
                    existingExpenses = expenses.toList(),
                    onSaveTransaction = { newTransaction ->
                        if (userId != null) {
                            FirestoreRepository.addTransaction(userId, newTransaction)
                        } else {
                            expenses.add(0, newTransaction)
                        }
                        showVoiceOverlay = false
                    },
                    onUpdateTransaction = { updatedTransaction ->
                        if (userId != null) {
                            FirestoreRepository.addTransaction(userId, updatedTransaction)
                        } else {
                            val index = expenses.indexOfFirst { it.id == updatedTransaction.id }
                            if (index != -1) expenses[index] = updatedTransaction
                        }
                        showVoiceOverlay = false
                    }
                )
            }
        }
    }
}