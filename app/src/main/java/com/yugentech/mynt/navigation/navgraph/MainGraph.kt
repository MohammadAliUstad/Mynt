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
    composable(AppScreen.Main.route) {
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
