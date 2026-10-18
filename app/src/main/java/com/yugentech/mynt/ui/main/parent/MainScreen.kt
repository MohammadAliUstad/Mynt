package com.yugentech.mynt.ui.main.parent

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yugentech.mynt.auth.viewmodel.AuthViewModel
import com.yugentech.mynt.database.model.Expense
import com.yugentech.mynt.database.model.TransactionType
import com.yugentech.mynt.transactions.viewmodel.TransactionsViewModel
import com.yugentech.mynt.ui.main.components.FloatingBottomBar
import com.yugentech.mynt.ui.main.components.MyntTab
import com.yugentech.mynt.ui.split.parent.SplitBillScreen
import com.yugentech.mynt.ui.split.parent.SplitScreenState
import com.yugentech.mynt.ui.tabs.historyScreen.parent.TransactionsScreen
import com.yugentech.mynt.ui.tabs.homeScreen.parent.HomeScreen
import com.yugentech.mynt.ui.tabs.profileScreen.parent.ProfileScreen
import com.yugentech.mynt.ui.tabs.profileScreen.viewmodel.SettingsViewModel
import com.yugentech.mynt.ui.tabs.statsScreen.parent.StatsScreen
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

@Composable
fun MainScreen(
    authViewModel: AuthViewModel,
    transactionsViewModel: TransactionsViewModel,
    settingsViewModel: SettingsViewModel,
    snackbarHostState: SnackbarHostState,
    onAddTransaction: (TransactionType) -> Unit,
    onEditTransaction: (Expense) -> Unit,
    onDeleteTransaction: (Expense) -> Unit,
    onLentBorrowedClick: () -> Unit,
    onVoiceClick: () -> Unit,
    onAppearanceClick: () -> Unit,
    onShowMessage: (String) -> Unit,
    onExitApp: () -> Unit,
    onSignOut: () -> Unit
) {
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val expenses by transactionsViewModel.transactions.collectAsStateWithLifecycle()
    val settings by settingsViewModel.settingsConfiguration.collectAsStateWithLifecycle()

    var selectedTab by rememberSaveable { mutableStateOf(MyntTab.HOME) }
    val saveableStateHolder = rememberSaveableStateHolder()
    val hazeState = rememberHazeState()

    // Split bill opens inside this screen, so the bottom bar never leaves and nothing flickers.
    var showSplit by remember { mutableStateOf(false) }
    val splitState = remember { SplitScreenState() }

    // Room for the floating bar under the last row of every tab.
    val navBarInset = with(LocalDensity.current) { WindowInsets.navigationBars.getBottom(this).toDp() }
    val bottomPadding = navBarInset + 104.dp

    // Blank until Firebase sign-in is wired up; screens fall back to a neutral greeting.
    val userName = authState.userData?.name.orEmpty()
    val userEmail = authState.userData?.email ?: "Signed in on this device"

    BackHandler(enabled = selectedTab != MyntTab.HOME) {
        selectedTab = MyntTab.HOME
    }
    BackHandler(enabled = showSplit) {
        showSplit = false
    }

    fun selectTab(tab: MyntTab) {
        if (tab != selectedTab) settingsViewModel.tick()
        selectedTab = tab
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Everything the bar floats over is the haze source, so it frosts as content scrolls by.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(hazeState)
                .background(MaterialTheme.colorScheme.background)
        ) {
            AnimatedContent(
                targetState = showSplit,
                modifier = Modifier.fillMaxSize(),
                transitionSpec = {
                    if (targetState) {
                        (slideInHorizontally(tween(320)) { w -> w / 4 } + fadeIn(tween(320))) togetherWith
                                (slideOutHorizontally(tween(320)) { w -> -w / 4 } + fadeOut(tween(200)))
                    } else {
                        (slideInHorizontally(tween(320)) { w -> -w / 4 } + fadeIn(tween(320))) togetherWith
                                (slideOutHorizontally(tween(320)) { w -> w / 4 } + fadeOut(tween(200)))
                    }
                },
                label = "splitOverlay"
            ) { splitOpen ->
                if (splitOpen) {
                    SplitBillScreen(
                        state = splitState,
                        onBackClick = { showSplit = false }
                    )
                } else {
                    AnimatedContent(
                        targetState = selectedTab,
                        transitionSpec = {
                            val forward = targetState.ordinal > initialState.ordinal
                            val spec = tween<androidx.compose.ui.unit.IntOffset>(320)
                            (slideInHorizontally(spec) { w -> if (forward) w / 4 else -w / 4 } + fadeIn(tween(320))) togetherWith
                                    (slideOutHorizontally(spec) { w -> if (forward) -w / 4 else w / 4 } + fadeOut(tween(200)))
                        },
                        label = "tabContent"
                    ) { tab ->
                        saveableStateHolder.SaveableStateProvider(tab) {
                            when (tab) {
                                MyntTab.HOME -> HomeScreen(
                                    expenses = expenses,
                                    userName = userName,
                                    monthlyBudget = settings.monthlyBudget,
                                    bottomPadding = bottomPadding,
                                    onSetBudget = settingsViewModel::setMonthlyBudget,
                                    onAddTransaction = onAddTransaction,
                                    onVoiceClick = onVoiceClick,
                                    onLentBorrowedClick = onLentBorrowedClick,
                                    onSplitClick = { showSplit = true },
                                    onTransactionClick = onEditTransaction,
                                    onDeleteTransaction = onDeleteTransaction,
                                    onSeeAllClick = { selectTab(MyntTab.HISTORY) },
                                    onProfileClick = { selectTab(MyntTab.ME) }
                                )

                                MyntTab.HISTORY -> TransactionsScreen(
                                    expenses = expenses,
                                    bottomPadding = bottomPadding,
                                    onTransactionClick = onEditTransaction,
                                    onDeleteTransaction = onDeleteTransaction,
                                    onAddClick = { onAddTransaction(TransactionType.EXPENSE) }
                                )

                                MyntTab.STATS -> StatsScreen(
                                    expenses = expenses,
                                    bottomPadding = bottomPadding
                                )

                                MyntTab.ME -> ProfileScreen(
                                    userName = userName,
                                    userEmail = userEmail,
                                    expenses = expenses,
                                    settingsViewModel = settingsViewModel,
                                    bottomPadding = bottomPadding,
                                    onShowMessage = onShowMessage,
                                    onSignOutClick = onSignOut,
                                    onExitClick = onExitApp,
                                    onAppearanceClick = onAppearanceClick,
                                    onLentBorrowedClick = onLentBorrowedClick
                                )
                            }
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = bottomPadding - 8.dp)
        )

        FloatingBottomBar(
            currentTab = selectedTab,
            onTabSelected = ::selectTab,
            onAddClick = { onAddTransaction(TransactionType.EXPENSE) },
            onVoiceClick = {
                if (showSplit) onShowMessage("Voice for splits comes in the next step")
                else onVoiceClick()
            },
            hazeState = hazeState,
            // Lifts with the keyboard on the Split screen, so the button is never hidden.
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .then(if (showSplit) Modifier.imePadding() else Modifier),
            splitMode = showSplit,
            splitLabel = splitState.barLabel,
            onSplitClick = splitState::onBarButton
        )
    }
}