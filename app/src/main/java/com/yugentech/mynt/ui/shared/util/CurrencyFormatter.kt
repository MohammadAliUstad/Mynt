package com.yugentech.mynt.ui.shared.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import com.yugentech.theme.tokens.AppConstants
import java.util.Locale
import kotlin.math.abs

// Provided once in MainActivity from the user's saved settings.
val LocalCurrencySymbol = compositionLocalOf { AppConstants.DEFAULT_CURRENCY_SYMBOL }

fun formatAmount(amount: Double, currencySymbol: String): String {
    return String.format(Locale.getDefault(), "%s%,.2f", currencySymbol, amount)
}

/** Whole amounts without the trailing ".00", for headline numbers where cents are noise. */
fun formatAmountShort(amount: Double, currencySymbol: String): String {
    val isWhole = abs(amount - Math.round(amount)) < 0.005
    return if (isWhole) {
        String.format(Locale.getDefault(), "%s%,d", currencySymbol, Math.round(amount))
    } else {
        formatAmount(amount, currencySymbol)
    }
}

/** 1.2K, 3.4L style figures for chart axes and tight labels. Lakh/crore for rupees. */
fun formatAmountCompact(amount: Double, currencySymbol: String): String {
    val value = abs(amount)
    val sign = if (amount < 0) "-" else ""
    val indian = currencySymbol == "₹"
    val (scaled, suffix) = when {
        indian && value >= 1_00_00_000 -> value / 1_00_00_000 to "Cr"
        indian && value >= 1_00_000 -> value / 1_00_000 to "L"
        !indian && value >= 1_000_000_000 -> value / 1_000_000_000 to "B"
        !indian && value >= 1_000_000 -> value / 1_000_000 to "M"
        value >= 1_000 -> value / 1_000 to "K"
        else -> return "$sign$currencySymbol${Math.round(value)}"
    }
    val text = if (scaled >= 100) String.format(Locale.getDefault(), "%.0f", scaled)
    else String.format(Locale.getDefault(), "%.1f", scaled).removeSuffix(".0").removeSuffix(",0")
    return "$sign$currencySymbol$text$suffix"
}

@Composable
@ReadOnlyComposable
fun formatAmount(amount: Double): String = formatAmount(amount, LocalCurrencySymbol.current)

@Composable
@ReadOnlyComposable
fun formatAmountShort(amount: Double): String = formatAmountShort(amount, LocalCurrencySymbol.current)

@Composable
@ReadOnlyComposable
fun formatAmountCompact(amount: Double): String = formatAmountCompact(amount, LocalCurrencySymbol.current)
