package com.yourname.expensetracker.navigation

/**
 * Screen routes used by AppNavHost.
 */
sealed class AppScreen(val route: String) {
    object Splash : AppScreen("splash")
    object SignIn : AppScreen("sign_in")
    object Main : AppScreen("main")
}
