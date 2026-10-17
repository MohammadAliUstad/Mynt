package com.yugentech.mynt.navigation.navgraph

import android.app.Activity
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.yugentech.mynt.auth.viewmodel.AuthViewModel
import com.yugentech.mynt.navigation.screen.AppScreen
import com.yugentech.mynt.ui.access.signIn.SignInScreen
import com.yugentech.mynt.ui.access.signUp.SignUpScreen
import timber.log.Timber

fun NavGraphBuilder.authGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    context: Context
) {
    // TODO: Firebase isn't wired in yet, so every way in just opens the app. Swap these for
    //  authViewModel.signIn / signUp / getGoogleSignInIntent(webClientId) once it is.
    val openApp: () -> Unit = {
        navController.navigate(AppScreen.Main.route) {
            popUpTo(0) { inclusive = true }
            launchSingleTop = true
        }
    }

    composable(AppScreen.SignIn.route) {
        Timber.v("Composing SignIn Screen")
        BackHandler { (context as? Activity)?.finish() }

        SignInScreen(
            authViewModel = authViewModel,
            onSignIn = { _, _ -> openApp() },
            onGoogleSignIn = openApp,
            onNavigateToSignUp = {
                navController.navigate(AppScreen.SignUp.route) {
                    launchSingleTop = true
                }
            },
            onForgotPassword = { email ->
                authViewModel.forgotPassword(email)
            }
        )
    }

    composable(AppScreen.SignUp.route) {
        Timber.v("Composing SignUp Screen")
        BackHandler { navController.popBackStack() }

        SignUpScreen(
            authViewModel = authViewModel,
            onSignUp = { _, _, _ -> openApp() },
            onGoogleSignIn = openApp,
            onNavigateToSignIn = {
                navController.popBackStack()
            }
        )
    }
}
