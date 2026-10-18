package com.yugentech.mynt.navigation.navgraph

import android.app.Activity
import android.content.Context
import androidx.compose.material3.SnackbarHostState
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.yugentech.mynt.auth.viewmodel.AuthViewModel
import com.yugentech.mynt.database.model.Expense
import com.yugentech.mynt.navigation.screen.AppScreen
import com.yugentech.mynt.transactions.viewmodel.TransactionsViewModel
import com.yugentech.mynt.ui.main.parent.MainScreen
import com.yugentech.mynt.ui.tabs.profileScreen.viewmodel.SettingsViewModel
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally

fun NavGraphBuilder.mainGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    transactionsViewModel: TransactionsViewModel,
    settingsViewModel: SettingsViewModel,
    snackbarHostState: SnackbarHostState,
    onDeleteWithUndo: (Expense) -> Unit,
    onShowMessage: (String) -> Unit,
    context: Context
) {
    composable(
        route = AppScreen.Main.route,
        // Going to Split bill fades instead of sliding, so the bottom bar doesn't appear to move.
        exitTransition = {
            if (targetState.destination.route == AppScreen.Split.route) fadeOut(tween(250))
            else slideOutHorizontally(tween(250)) { -1000 } + fadeOut(tween(250))
        },
        popEnterTransition = {
            if (initialState.destination.route == AppScreen.Split.route) fadeIn(tween(250))
            else slideInHorizontally(tween(250)) { -1000 } + fadeIn(tween(250))
        }
    ) {
        MainScreen(
            authViewModel = authViewModel,
            transactionsViewModel = transactionsViewModel,
            settingsViewModel = settingsViewModel,
            snackbarHostState = snackbarHostState,
            onAddTransaction = { type ->
                navController.navigate(AppScreen.Transaction.add(type)) { launchSingleTop = true }
            },
            onEditTransaction = { expense ->
                navController.navigate(AppScreen.Transaction.edit(expense.id)) { launchSingleTop = true }
            },
            onDeleteTransaction = onDeleteWithUndo,
            onLentBorrowedClick = {
                navController.navigate(AppScreen.LentBorrowed.route) { launchSingleTop = true }
            },
            onVoiceClick = {
                navController.navigate(AppScreen.Voice.route) { launchSingleTop = true }
            },
            onAppearanceClick = {
                navController.navigate(AppScreen.Appearance.route) { launchSingleTop = true }
            },
            onShowMessage = onShowMessage,
            onExitApp = {
                (context as? Activity)?.finish()
            },
            onSignOut = {
                authViewModel.signOut()
                // Sign-in only opens the app for now, so nothing in the auth state changes to send us back.
                navController.navigate(AppScreen.SignIn.route) {
                    popUpTo(0) { inclusive = true }
                    launchSingleTop = true
                }
            }
        )
    }
}
