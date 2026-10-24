package com.yugentech.mynt.navigation.navgraph

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
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
import com.yugentech.mynt.split.model.SplitBill
import com.yugentech.mynt.transactions.insights.FinanceInsights
import com.yugentech.mynt.transactions.viewmodel.TransactionsViewModel
import com.yugentech.mynt.ui.tabs.profileScreen.viewmodel.SettingsViewModel
import com.yugentech.mynt.ui.transactions.addExpense.parent.AddExpenseScreen
import com.yugentech.mynt.ui.transactions.lentBorrowed.parent.LentBorrowedScreen
import com.yugentech.mynt.ui.voice.parent.VoiceScreen
import com.yugentech.mynt.voice.viewmodel.VoiceViewModel
import org.koin.androidx.compose.koinViewModel
import com.yugentech.mynt.ui.split.parent.SplitSession

fun NavGraphBuilder.transactionGraph(
    navController: NavHostController,
    transactionsViewModel: TransactionsViewModel,
    settingsViewModel: SettingsViewModel,
    onDeleteWithUndo: (Expense) -> Unit,
    onShowMessage: (String) -> Unit
) {
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

    composable(
        route = AppScreen.LentBorrowed.route,
        enterTransition = { slideInHorizontally(tween(300)) { it } + fadeIn(tween(300)) },
        exitTransition = { slideOutHorizontally(tween(300)) { -it / 4 } + fadeOut(tween(300)) },
        popEnterTransition = { slideInHorizontally(tween(300)) { -it / 4 } + fadeIn(tween(300)) },
        popExitTransition = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)) }
    ) {
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
                val splits = results.filter { it.splitBill != null }
                val entries = results.filter { it.expense != null }

                if (entries.isNotEmpty()) {
                    val byId = expenses.associateBy { it.id }
                    val baseTime = System.currentTimeMillis()
                    transactionsViewModel.saveTransactions(
                        entries.mapIndexed { index, result ->
                            val expense = result.expense!!
                            val original = byId[expense.id]
                            if (result.isUpdate && original != null) {
                                expense.copy(timestamp = original.timestamp)
                            } else {
                                val ts = if (expense.timestamp > 0) expense.timestamp else (baseTime + (index * 10L))
                                expense.copy(timestamp = ts)
                            }
                        }
                    )
                    settingsViewModel.confirm()
                }

                // Split bills go into the shared Split session; Home opens it on the Bill tab.
                splits.forEach { result ->
                    SplitSession.addVoiceBill(result.participants, result.splitBill!!)
                }

                val updates = entries.count { it.isUpdate }
                val added = entries.size - updates
                onShowMessage(
                    listOfNotNull(
                        if (added > 0) "Added $added" else null,
                        if (updates > 0) "updated $updates" else null,
                        if (splits.isNotEmpty()) "split bill added" else null
                    ).joinToString(", ").replaceFirstChar { it.uppercase() }
                )
                navController.popBackStack()
            }
        )
    }
}