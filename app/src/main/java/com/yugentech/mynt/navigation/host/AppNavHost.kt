package com.yugentech.mynt.navigation.host

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.yugentech.mynt.auth.viewmodel.AuthViewModel
import com.yugentech.mynt.database.model.Expense
import com.yugentech.mynt.ui.shared.components.displayTitle
import kotlinx.coroutines.launch
import com.yugentech.mynt.navigation.navgraph.authGraph
import com.yugentech.mynt.navigation.navgraph.mainGraph
import com.yugentech.mynt.navigation.navgraph.settingsGraph
import com.yugentech.mynt.navigation.navgraph.transactionGraph
import com.yugentech.mynt.navigation.screen.AppScreen
import com.yugentech.mynt.theme.viewmodel.ThemeViewModel
import com.yugentech.mynt.transactions.viewmodel.TransactionsViewModel
import com.yugentech.mynt.ui.tabs.profileScreen.viewmodel.SettingsViewModel
import org.koin.androidx.compose.koinViewModel
import timber.log.Timber

@Composable
fun AppNavHost(
    navController: NavHostController,
    webClientId: String,
    authViewModel: AuthViewModel,
    themeViewModel: ThemeViewModel,
    settingsViewModel: SettingsViewModel
) {
    val context = LocalContext.current
    val authState by authViewModel.authState.collectAsStateWithLifecycle()

    // Activity-scoped so every destination shares one transactions stream
    val transactionsViewModel: TransactionsViewModel = koinViewModel()

    // One snackbar for the whole app, so a delete made in the editor can still be undone
    // after it closes.
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun showMessage(message: String) {
        if (message.isBlank()) return
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
        }
    }

    fun deleteWithUndo(expense: Expense) {
        transactionsViewModel.deleteTransaction(expense)
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(
                message = "Deleted \"${expense.displayTitle}\"",
                actionLabel = "Undo",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) transactionsViewModel.restoreTransaction(expense)
        }
    }

    val startDestination = remember {
        if (authState.isUserLoggedIn && authState.userId != null) {
            AppScreen.Main.route
        } else {
            AppScreen.SignIn.route
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
        onResult = { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                Timber.d("Google Sign-In Activity Result OK")
                authViewModel.handleGoogleSignInResult(result.data)
            } else {
                Timber.w("Google Sign-In Activity Result Cancelled or Failed")
                authViewModel.onGoogleSignInCancelled()
            }
        }
    )

    LaunchedEffect(authState.intent) {
        authState.intent?.let {
            Timber.d("Launching Google Sign-In Intent")
            launcher.launch(IntentSenderRequest.Builder(it).build())
        }
    }

    LaunchedEffect(authState.isUserLoggedIn, authState.userId) {
        if (!authState.isLoading && !authState.isInitializing) {
            val currentRoute = navController.currentDestination?.route

            if (authState.isUserLoggedIn && authState.userId != null) {
                if (currentRoute != AppScreen.Main.route) {
                    navController.navigate(AppScreen.Main.route) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            } else if (currentRoute != AppScreen.SignIn.route) {
                navController.navigate(AppScreen.SignIn.route) {
                    popUpTo(0) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = {
            slideInHorizontally(
                animationSpec = tween(250),
                initialOffsetX = { 1000 }
            ) + fadeIn(animationSpec = tween(250))
        },
        exitTransition = {
            slideOutHorizontally(
                animationSpec = tween(250),
                targetOffsetX = { -1000 }
            ) + fadeOut(animationSpec = tween(250))
        },
        popEnterTransition = {
            slideInHorizontally(
                animationSpec = tween(250),
                initialOffsetX = { -1000 }
            ) + fadeIn(animationSpec = tween(250))
        },
        popExitTransition = {
            slideOutHorizontally(
                animationSpec = tween(250),
                targetOffsetX = { 1000 }
            ) + fadeOut(animationSpec = tween(250))
        }
    ) {
        authGraph(
            navController = navController,
            authViewModel = authViewModel,
            context = context
        )
        mainGraph(
            navController = navController,
            authViewModel = authViewModel,
            transactionsViewModel = transactionsViewModel,
            settingsViewModel = settingsViewModel,
            snackbarHostState = snackbarHostState,
            onDeleteWithUndo = ::deleteWithUndo,
            onShowMessage = ::showMessage,
            context = context
        )
        transactionGraph(
            navController = navController,
            transactionsViewModel = transactionsViewModel,
            settingsViewModel = settingsViewModel,
            onDeleteWithUndo = ::deleteWithUndo,
            onShowMessage = ::showMessage
        )
        settingsGraph(navController, themeViewModel)
    }
}
