package com.yourname.expensetracker.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.yourname.expensetracker.SettingsManager
import com.yourname.expensetracker.ThemeMode

@Composable
fun ExpenseTrackerTheme(
    darkTheme: Boolean = when (SettingsManager.themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    },
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val accentPrimary = Color(SettingsManager.accentTheme.primaryHex)

    val baseColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> darkColorScheme(
            primary = accentPrimary,
            secondary = PurpleGrey80,
            tertiary = Pink80
        )

        else -> lightColorScheme(
            primary = accentPrimary,
            secondary = PurpleGrey40,
            tertiary = Pink40
        )
    }

    // If AMOLED mode is enabled in dark theme, set background and surface to pure black
    val colorScheme = if (darkTheme && SettingsManager.isAmoledMode) {
        baseColorScheme.copy(
            background = Color.Black,
            surface = Color.Black,
            surfaceVariant = Color(0xFF121212)
        )
    } else {
        baseColorScheme
    }

    // Dynamic Typography based on selected font family
    val currentFontFamily = SettingsManager.fontOption.fontFamily
    val appTypography = Typography(
        headlineLarge = Typography.headlineLarge.copy(fontFamily = currentFontFamily),
        headlineMedium = Typography.headlineMedium.copy(fontFamily = currentFontFamily),
        headlineSmall = Typography.headlineSmall.copy(fontFamily = currentFontFamily),
        titleLarge = Typography.titleLarge.copy(fontFamily = currentFontFamily),
        titleMedium = Typography.titleMedium.copy(fontFamily = currentFontFamily),
        titleSmall = Typography.titleSmall.copy(fontFamily = currentFontFamily),
        bodyLarge = Typography.bodyLarge.copy(fontFamily = currentFontFamily),
        bodyMedium = Typography.bodyMedium.copy(fontFamily = currentFontFamily),
        bodySmall = Typography.bodySmall.copy(fontFamily = currentFontFamily),
        labelLarge = Typography.labelLarge.copy(fontFamily = currentFontFamily),
        labelMedium = Typography.labelMedium.copy(fontFamily = currentFontFamily),
        labelSmall = Typography.labelSmall.copy(fontFamily = currentFontFamily)
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !darkTheme
                insetsController.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = appTypography,
        content = content
    )
}
