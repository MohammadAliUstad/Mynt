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
    webClientId: String,
    context: Context
) {
    composable(AppScreen.SignIn.route) {
        Timber.v("Composing SignIn Screen")
        BackHandler { (context as? Activity)?.finish() }

        SignInScreen(
            authViewModel = authViewModel,
            onSignIn = { email, password ->
                authViewModel.signIn(email, password)
            },
            onGoogleSignIn = {
                authViewModel.getGoogleSignInIntent(webClientId)
            },
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
            onSignUp = { name, email, password ->
                authViewModel.signUp(name, email, password)
            },
            onGoogleSignIn = {
                authViewModel.getGoogleSignInIntent(webClientId)
            },
            onNavigateToSignIn = {
                navController.popBackStack()
            }
        )
    }
}