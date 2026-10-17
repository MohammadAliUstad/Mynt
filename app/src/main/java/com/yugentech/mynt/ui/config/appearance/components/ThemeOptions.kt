package com.yugentech.mynt.ui.config.appearance.components

import androidx.compose.ui.graphics.Color
import com.yugentech.theme.AppColorSchemes
import com.yugentech.theme.models.ColorTheme

data class ThemeOption(
    val colorTheme: ColorTheme,
    val displayName: String,
    val primaryColor: Color
)

fun themeOptions(currentPrimary: Color): List<ThemeOption> = listOf(
    ThemeOption(ColorTheme.DYNAMIC, "Dynamic", currentPrimary),
    ThemeOption(ColorTheme.MYNT, "Mynt", AppColorSchemes.MyntLightColorScheme.primary),
    ThemeOption(ColorTheme.LAGOON, "Lagoon", AppColorSchemes.LagoonLightColorScheme.primary),
    ThemeOption(ColorTheme.CANYON, "Canyon", AppColorSchemes.CanyonLightColorScheme.primary),
    ThemeOption(ColorTheme.HARVEST, "Harvest", AppColorSchemes.HarvestLightColorScheme.primary),
    ThemeOption(ColorTheme.SAKURA, "Sakura", AppColorSchemes.SakuraLightColorScheme.primary),
    ThemeOption(ColorTheme.ALPINE, "Alpine", AppColorSchemes.AlpineLightColorScheme.primary),
    ThemeOption(ColorTheme.TWILIGHT, "Twilight", AppColorSchemes.TwilightLightColorScheme.primary)
)
