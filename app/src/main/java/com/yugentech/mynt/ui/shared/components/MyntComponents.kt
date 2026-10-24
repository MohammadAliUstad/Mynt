package com.yugentech.mynt.ui.shared.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yugentech.mynt.database.model.Expense
import com.yugentech.mynt.database.model.TransactionType
import com.yugentech.mynt.ui.shared.util.CategoryStyle
import com.yugentech.mynt.ui.shared.util.FinanceColors
import com.yugentech.mynt.ui.shared.util.LocalCurrencySymbol
import com.yugentech.mynt.ui.shared.util.categoryStyle
import com.yugentech.mynt.ui.shared.util.formatAmount
import com.yugentech.mynt.ui.shared.util.formatAmountShort

/** The rounded container every card in Mynt sits on. */
@Composable
fun MyntCard(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surfaceContainer,
    shape: Shape = RoundedCornerShape(26.dp),
    onClick: (() -> Unit)? = null,
    contentPadding: Dp = 18.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val inner: @Composable () -> Unit = {
        Column(modifier = Modifier.padding(contentPadding), content = content)
    }
    if (onClick != null) {
        Surface(onClick = onClick, modifier = modifier, shape = shape, color = color, content = inner)
    } else {
        Surface(modifier = modifier, shape = shape, color = color, content = inner)
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) {
                Text(actionLabel, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun CategoryBadge(
    style: CategoryStyle,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    shape: Shape = RoundedCornerShape(14.dp)
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(style.container),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = style.icon,
            contentDescription = null,
            tint = style.color,
            modifier = Modifier.size(size * 0.5f)
        )
    }
}

/**
 * Money in shows "+", money out shows "−": income and borrowing bring cash in;
 * spending and lending send it out.
 */
fun TransactionType.isInflow(): Boolean = this == TransactionType.INCOME || this == TransactionType.BORROWED

@Composable
fun signedAmount(expense: Expense): String {
    val sign = if (expense.type.isInflow()) "+" else "−"
    return sign + formatAmountShort(expense.amount)
}

/** Who the entry is about: the person for lent/borrowed, otherwise its title. */
val Expense.displayTitle: String
    get() = if ((type == TransactionType.LENT || type == TransactionType.BORROWED) && personName.isNotBlank()) {
        personName
    } else {
        title
    }

@Composable
fun TransactionRow(
    expense: Expense,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showDate: Boolean = true
) {
    val style = categoryStyle(expense.category, expense.type)
    val subtitle = buildString {
        append(
            when (expense.type) {
                TransactionType.LENT -> if (expense.category == "Settlement") "Settled up" else "Lent"
                TransactionType.BORROWED -> if (expense.category == "Settlement") "Settled up" else "Borrowed"
                else -> expense.category
            }
        )
        if (expense.type == TransactionType.LENT || expense.type == TransactionType.BORROWED) {
            if (expense.title.isNotBlank() && !expense.title.equals(expense.personName, ignoreCase = true)) {
                append(" · ").append(expense.title)
            }
        }
        if (showDate) append(" · ").append(expense.date)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CategoryBadge(style = style)

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = expense.displayTitle,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.width(8.dp))

        Text(
            text = signedAmount(expense),
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = if (expense.type == TransactionType.EXPENSE) MaterialTheme.colorScheme.onSurface
            else FinanceColors.forType(expense.type),
            maxLines = 1
        )
    }
}

/** A [TransactionRow] that deletes on a swipe to the left. */
@Composable
fun SwipeableTransactionRow(
    expense: Expense,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    showDate: Boolean = true
) {
    val state = rememberSwipeToDismissBoxState()

    LaunchedEffect(expense.id) {
        if (state.currentValue != SwipeToDismissBoxValue.Settled) {
            state.snapTo(SwipeToDismissBoxValue.Settled)
        }
    }

    SwipeToDismissBox(
        state = state,
        modifier = modifier,
        enableDismissFromStartToEnd = false,
        onDismiss = { value -> if (value == SwipeToDismissBoxValue.EndToStart) onDelete() },
        backgroundContent = {
            val active = state.targetValue == SwipeToDismissBoxValue.EndToStart
            val background by animateColorAsState(
                if (active) MaterialTheme.colorScheme.errorContainer else Color.Transparent,
                label = "swipeBackground"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(18.dp))
                    .background(background)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = "Delete",
                    tint = if (active) MaterialTheme.colorScheme.onErrorContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    ) {
        Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = RoundedCornerShape(18.dp)) {
            TransactionRow(expense = expense, onClick = onClick, showDate = showDate)
        }
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 28.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp))
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(16.dp))
            FilledTonalButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

/** Counts up (or down) to [amount] whenever it changes, so totals feel alive. */
@Composable
fun AnimatedAmountText(
    amount: Double,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    prefix: String = "",
    showDecimals: Boolean = false
) {
    val animatable = remember { Animatable(amount.toFloat()) }
    LaunchedEffect(amount) {
        animatable.animateTo(amount.toFloat(), tween(durationMillis = 650, easing = FastOutSlowInEasing))
    }
    val symbol = LocalCurrencySymbol.current
    val value = animatable.value.toDouble()
    // Settle on the exact figure once the animation lands, so rounding never shows.
    val shown = if (animatable.isRunning) value else amount
    Text(
        text = prefix + if (showDecimals) formatAmount(shown, symbol) else formatAmountShort(shown, symbol),
        style = style,
        color = color,
        modifier = modifier,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

/** A small rounded label, like "−12%" or "Over budget". */
@Composable
fun Pill(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.16f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
        Text(text = text, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = color)
    }
}