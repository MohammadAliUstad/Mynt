package com.yugentech.mynt.navigation.navgraph

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.yugentech.mynt.navigation.screen.AppScreen
import com.yugentech.mynt.theme.viewmodel.ThemeViewModel
import com.yugentech.mynt.ui.config.appearance.parent.ThemeSettingsScreen

fun NavGraphBuilder.settingsGraph(
    navController: NavHostController,
    themeViewModel: ThemeViewModel
) {
    composable(AppScreen.Appearance.route) {
        ThemeSettingsScreen(
            themeViewModel = themeViewModel,
            onBackClick = { navController.popBackStack() }
        )
    }
}
