package com.yourname.expensetracker

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
 * Main application content with floating bottom navigation bar, Settings screen slide/fade transition,
 * dedicated Theme settings transition, and full-screen voice overlay.
 */
@Composable
fun ExpenseTrackerMainContent(
    onSignOut: () -> Unit
) {
    // Shared in-memory list of expenses & incomes
    val expenses = remember { mutableStateListOf<Expense>() }

    // Navigation tab state
    var selectedTab by remember { mutableStateOf(NavTab.HOME) }

    // Voice Screen overlay visibility
    var showVoiceOverlay by remember { mutableStateOf(false) }

    // Settings Screen visibility
    var showSettingsScreen by remember { mutableStateOf(false) }

    // Dedicated Theme Settings Screen visibility
    var showThemeSettingsScreen by remember { mutableStateOf(false) }

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
                    NavTab.HOME -> HomeScreen(expenses = expenses)
                    NavTab.ADD -> AddExpenseScreen(
                        onBackClick = { selectedTab = NavTab.HOME },
                        onSaveExpense = { newTransaction ->
                            expenses.add(0, newTransaction)
                            selectedTab = NavTab.HOME
                        }
                    )

                    NavTab.STATS -> StatsScreen(expenses = expenses)
                    NavTab.ME -> ProfileScreen(
                        onSignOutClick = onSignOut,
                        onNavigateToSettings = { showSettingsScreen = true }
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
                        expenses.add(0, newTransaction)
                        showVoiceOverlay = false
                    }
                )
            }
        }
    }
}
