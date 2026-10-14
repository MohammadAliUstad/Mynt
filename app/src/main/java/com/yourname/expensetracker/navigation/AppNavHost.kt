package com.yourname.expensetracker.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.yourname.expensetracker.AuthManager
import com.yourname.expensetracker.ExpenseTrackerMainContent
import com.yourname.expensetracker.SignInScreen
import com.yourname.expensetracker.SplashScreen

/**
 * Navigation Host for ExpenseTracker app (adapted from Quill's AppNavHost).
 * Handles top-level transitions between Splash, Sign-In, and Main App screens.
 */
@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: String = AppScreen.Splash.route
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
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
            // 1. Splash Screen Route
            composable(AppScreen.Splash.route) {
                SplashScreen(
                    onSplashFinished = {
                        val nextRoute = if (AuthManager.isUserSignedIn) {
                            AppScreen.Main.route
                        } else {
                            AppScreen.SignIn.route
                        }
                        navController.navigate(nextRoute) {
                            popUpTo(AppScreen.Splash.route) { inclusive = true }
                        }
                    }
                )
            }

            // 2. Sign-In Screen Route
            composable(AppScreen.SignIn.route) {
                SignInScreen(
                    onSignInSuccess = {
                        navController.navigate(AppScreen.Main.route) {
                            popUpTo(AppScreen.SignIn.route) { inclusive = true }
                        }
                    }
                )
            }

            // 3. Main App Screen Route
            composable(AppScreen.Main.route) {
                ExpenseTrackerMainContent(
                    onSignOut = {
                        navController.navigate(AppScreen.SignIn.route) {
                            popUpTo(AppScreen.Main.route) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}
