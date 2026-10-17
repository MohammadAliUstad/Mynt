package com.yugentech.mynt.navigation.navgraph

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.yugentech.mynt.database.model.Expense
import com.yugentech.mynt.database.model.TransactionType
import com.yugentech.mynt.navigation.screen.AppScreen
import com.yugentech.mynt.transactions.insights.FinanceInsights
import com.yugentech.mynt.transactions.viewmodel.TransactionsViewModel
import com.yugentech.mynt.ui.tabs.profileScreen.viewmodel.SettingsViewModel
import com.yugentech.mynt.ui.transactions.addExpense.parent.AddExpenseScreen
import com.yugentech.mynt.ui.transactions.lentBorrowed.parent.LentBorrowedScreen
import com.yugentech.mynt.ui.voice.parent.VoiceScreen
import com.yugentech.mynt.voice.viewmodel.VoiceViewModel
import org.koin.androidx.compose.koinViewModel

fun NavGraphBuilder.transactionGraph(
    navController: NavHostController,
    transactionsViewModel: TransactionsViewModel,
    settingsViewModel: SettingsViewModel,
    onDeleteWithUndo: (Expense) -> Unit,
    onShowMessage: (String) -> Unit
) {
    // The editor rises from the bottom like a sheet
    composable(
        route = AppScreen.Transaction.route,
        arguments = listOf(
            navArgument(AppScreen.ARG_ID) { type = NavType.StringType; nullable = true; defaultValue = null },
            navArgument(AppScreen.ARG_TYPE) { type = NavType.StringType; nullable = true; defaultValue = null }
        ),
        enterTransition = { slideInVertically(tween(320)) { it / 3 } + fadeIn(tween(260)) },
        exitTransition = { fadeOut(tween(200)) },
        popEnterTransition = { fadeIn(tween(200)) },
        popExitTransition = { slideOutVertically(tween(280)) { it / 3 } + fadeOut(tween(220)) }
    ) { entry ->
        val expenses by transactionsViewModel.transactions.collectAsStateWithLifecycle()
        val id = entry.arguments?.getString(AppScreen.ARG_ID)
        val initialType = entry.arguments?.getString(AppScreen.ARG_TYPE)
            ?.let { runCatching { TransactionType.valueOf(it) }.getOrNull() }
            ?: TransactionType.EXPENSE
        val existing = remember(id, expenses) { id?.let { wanted -> expenses.firstOrNull { it.id == wanted } } }
        val people = remember(expenses) { FinanceInsights.personBalances(expenses).map { it.name } }

        // Wait for the list before filling the form, or an edit would open blank.
        if (id != null && existing == null) return@composable

        AddExpenseScreen(
            existing = existing,
            initialType = initialType,
            knownPeople = people,
            onBackClick = { navController.popBackStack() },
            onSave = { saved ->
                transactionsViewModel.saveTransaction(saved)
                settingsViewModel.confirm()
                onShowMessage(if (existing != null) "Changes saved" else "Added")
                navController.popBackStack()
            },
            onDelete = { deleted ->
                navController.popBackStack()
                onDeleteWithUndo(deleted)
            }
        )
    }

    composable(AppScreen.LentBorrowed.route) {
        val expenses by transactionsViewModel.transactions.collectAsStateWithLifecycle()

        LentBorrowedScreen(
            expenses = expenses,
            onBackClick = { navController.popBackStack() },
            onAddClick = { type ->
                navController.navigate(AppScreen.Transaction.add(type)) { launchSingleTop = true }
            },
            onTransactionClick = { tx ->
                navController.navigate(AppScreen.Transaction.edit(tx.id)) { launchSingleTop = true }
            },
            onSettle = { settlement ->
                transactionsViewModel.saveTransaction(settlement)
                settingsViewModel.confirm()
            }
        )
    }

    // Voice input slides up from the bottom like a sheet
    composable(
        route = AppScreen.Voice.route,
        enterTransition = { slideInVertically(tween(300)) { it } + fadeIn(tween(300)) },
        exitTransition = { slideOutVertically(tween(300)) { it } + fadeOut(tween(300)) },
        popEnterTransition = { slideInVertically(tween(300)) { it } + fadeIn(tween(300)) },
        popExitTransition = { slideOutVertically(tween(300)) { it } + fadeOut(tween(300)) }
    ) {
        val voiceViewModel: VoiceViewModel = koinViewModel()
        val expenses by transactionsViewModel.transactions.collectAsStateWithLifecycle()

        VoiceScreen(
            viewModel = voiceViewModel,
            existingExpenses = expenses,
            onCloseClick = { navController.popBackStack() },
            onConfirmTransactions = { results ->
                // An update keeps the original's creation time, so it stays where it was in lists.
                val byId = expenses.associateBy { it.id }
                transactionsViewModel.saveTransactions(
                    results.map { result ->
                        val original = byId[result.expense.id]
                        if (result.isUpdate && original != null) result.expense.copy(timestamp = original.timestamp)
                        else result.expense
                    }
                )
                settingsViewModel.confirm()
                val updates = results.count { it.isUpdate }
                val added = results.size - updates
                onShowMessage(
                    listOfNotNull(
                        if (added > 0) "Added $added" else null,
                        if (updates > 0) "updated $updates" else null
                    ).joinToString(", ").replaceFirstChar { it.uppercase() }
                )
                navController.popBackStack()
            }
        )
    }
}
