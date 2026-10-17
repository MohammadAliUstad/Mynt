package com.yugentech.mynt.ui.shared.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.CallMade
import androidx.compose.material.icons.automirrored.rounded.CallReceived
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Handshake
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocalGroceryStore
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import com.yugentech.mynt.database.model.SETTLEMENT_CATEGORY
import com.yugentech.mynt.database.model.TransactionType
import java.util.Locale

data class CategoryStyle(
    val icon: ImageVector,
    /** The category's hue; use [container] behind the icon and this for the icon itself. */
    val color: Color
) {
    val container: Color get() = color.copy(alpha = 0.16f)
}

/**
 * Money colours that hold up in both themes: a little brighter on dark backgrounds,
 * a little deeper on light ones.
 */
object FinanceColors {
    val income: Color
        @Composable @ReadOnlyComposable get() = pick(light = Color(0xFF1E8E4E), dark = Color(0xFF5FD38D))
    val expense: Color
        @Composable @ReadOnlyComposable get() = pick(light = Color(0xFFD93F3F), dark = Color(0xFFFF7A70))
    val lent: Color
        @Composable @ReadOnlyComposable get() = pick(light = Color(0xFFE08600), dark = Color(0xFFFFB547))
    val borrowed: Color
        @Composable @ReadOnlyComposable get() = pick(light = Color(0xFF2F6FE0), dark = Color(0xFF7DA9FF))

    @Composable
    @ReadOnlyComposable
    fun forType(type: TransactionType): Color = when (type) {
        TransactionType.INCOME -> income
        TransactionType.EXPENSE -> expense
        TransactionType.LENT -> lent
        TransactionType.BORROWED -> borrowed
    }

    @Composable
    @ReadOnlyComposable
    private fun pick(light: Color, dark: Color): Color =
        if (MaterialTheme.colorScheme.background.luminance() < 0.5f) dark else light
}

private val categoryPalette = mapOf(
    "food" to (Icons.Rounded.Restaurant to Color(0xFFFF7043)),
    "groceries" to (Icons.Rounded.LocalGroceryStore to Color(0xFF66BB6A)),
    "travel" to (Icons.Rounded.DirectionsCar to Color(0xFF42A5F5)),
    "shopping" to (Icons.Rounded.ShoppingBag to Color(0xFFAB47BC)),
    "bills" to (Icons.AutoMirrored.Rounded.ReceiptLong to Color(0xFF26A69A)),
    "rent" to (Icons.Rounded.Home to Color(0xFF8D6E63)),
    "health" to (Icons.Rounded.LocalHospital to Color(0xFFEC407A)),
    "entertainment" to (Icons.Rounded.Movie to Color(0xFFFFA726)),
    "education" to (Icons.Rounded.School to Color(0xFF5C6BC0)),
    "emi" to (Icons.Rounded.AccountBalance to Color(0xFF29B6F6)),
    "salary" to (Icons.Rounded.Work to Color(0xFF43A047)),
    "freelance" to (Icons.Rounded.Payments to Color(0xFF00ACC1)),
    "business" to (Icons.Rounded.Storefront to Color(0xFF7CB342)),
    "gift" to (Icons.Rounded.CardGiftcard to Color(0xFFEC407A)),
    "investment" to (Icons.AutoMirrored.Rounded.TrendingUp to Color(0xFFFFB300)),
    "refund" to (Icons.Rounded.Replay to Color(0xFF26C6DA))
)

private val fallbackColor = Color(0xFF8C9399)

fun categoryStyle(category: String, type: TransactionType = TransactionType.EXPENSE): CategoryStyle {
    val key = category.lowercase(Locale.getDefault())
    return when {
        key == SETTLEMENT_CATEGORY.lowercase(Locale.getDefault()) ->
            CategoryStyle(Icons.Rounded.Handshake, Color(0xFF26A69A))
        type == TransactionType.LENT -> CategoryStyle(Icons.AutoMirrored.Rounded.CallMade, Color(0xFFFFA000))
        type == TransactionType.BORROWED -> CategoryStyle(Icons.AutoMirrored.Rounded.CallReceived, Color(0xFF448AFF))
        else -> categoryPalette[key]
            ?.let { (icon, color) -> CategoryStyle(icon, color) }
            ?: CategoryStyle(
                icon = if (type == TransactionType.INCOME) Icons.Rounded.Payments else Icons.Rounded.Category,
                color = fallbackColor
            )
    }
}

/** A stable chart colour for the nth category in a breakdown. */
fun chartColor(category: String, index: Int): Color =
    categoryPalette[category.lowercase(Locale.getDefault())]?.second
        ?: chartFallbacks[index % chartFallbacks.size]

private val chartFallbacks = listOf(
    Color(0xFF7E57C2), Color(0xFF26A69A), Color(0xFFFFCA28), Color(0xFFEF5350), Color(0xFF78909C)
)
