package com.yugentech.mynt.ui.main.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yugentech.mynt.ui.shared.components.orb.OrbEnergy
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.material.icons.rounded.DeleteOutline

// Pill sizing, as on Amatyaa: oval ends, a fixed cell per tab so the indicator math stays exact.
private val PillHeight = 72.dp
private val TabCell = 60.dp
private val PillHPadding = 10.dp
private val IndicatorHeight = 54.dp

/** Frosted glass over whatever scrolls underneath: theme-tinted so it reads in light and dark. */
@Composable
fun glassHazeStyle(): HazeStyle {
    val surface = MaterialTheme.colorScheme.surfaceContainer
    return HazeStyle(
        backgroundColor = MaterialTheme.colorScheme.background,
        tint = HazeTint(surface.copy(alpha = 0.55f)),
        blurRadius = 22.dp,
        noiseFactor = 0.04f,
        // Below Android 12 there's no blur; a near-solid surface keeps the icons legible.
        fallbackTint = HazeTint(surface.copy(alpha = 0.94f))
    )
}

/**
 * Amatyaa's floating bar, carried over to Mynt: a frosted tab pill with a gliding
 * indicator, and the living orb beside it for voice entry.
 *
 * Tapping History again once it's open turns its icon into "+" and starts a new entry,
 * the way Amatyaa's Clients and Files tabs do.
 *
 * In [splitMode] the pill is painted over and becomes one button labelled [splitLabel];
 * the orb never moves.
 */
@Composable
fun FloatingBottomBar(
    currentTab: MyntTab,
    onTabSelected: (MyntTab) -> Unit,
    onAddClick: () -> Unit,
    onVoiceClick: () -> Unit,
    hazeState: HazeState?,
    modifier: Modifier = Modifier,
    splitMode: Boolean = false,
    splitLabel: String = "Split",
    onSplitClick: () -> Unit = {},
    deleteMode: Boolean = false,
    onDeleteClick: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TabsPill(
                currentTab = currentTab,
                onTabSelected = onTabSelected,
                onAddClick = onAddClick,
                hazeState = hazeState,
                splitMode = splitMode,
                splitLabel = splitLabel,
                onSplitClick = onSplitClick
            )

            VoiceOrbButton(
                onClick = onVoiceClick,
                deleteMode = deleteMode,
                onDeleteClick = onDeleteClick
            )
        }
    }
}

@Composable
private fun TabsPill(
    currentTab: MyntTab,
    onTabSelected: (MyntTab) -> Unit,
    onAddClick: () -> Unit,
    hazeState: HazeState?,
    splitMode: Boolean,
    splitLabel: String,
    onSplitClick: () -> Unit
) {
    val tabs = MyntTab.entries
    val selectedIndex = tabs.indexOf(currentTab).coerceAtLeast(0)

    val indicatorOffset by animateDpAsState(
        targetValue = TabCell * selectedIndex,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow),
        label = "indicatorOffset"
    )

    // The "paint": a block of colour sweeps across the pill from left to right.
    val paint by animateFloatAsState(
        targetValue = if (splitMode) 1f else 0f,
        animationSpec = tween(420, easing = FastOutSlowInEasing),
        label = "splitPaint"
    )
    val labelAlpha = ((paint - 0.45f) / 0.55f).coerceIn(0f, 1f)

    val pillShape = RoundedCornerShape(50)

    Surface(
        modifier = Modifier
            .height(PillHeight)
            .clip(pillShape)
            .then(if (hazeState != null) Modifier.hazeEffect(state = hazeState, style = glassHazeStyle()) else Modifier),
        shape = pillShape,
        color = if (hazeState != null) Color.Transparent else MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp
    ) {
        Box {
            // The four tabs, fading out as the paint covers them.
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(horizontal = PillHPadding)
                    .graphicsLayer { alpha = 1f - paint },
                contentAlignment = Alignment.CenterStart
            ) {
                Box(
                    modifier = Modifier
                        .offset(x = indicatorOffset)
                        .width(TabCell)
                        .height(IndicatorHeight)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.28f))
                )

                Row(
                    modifier = Modifier.fillMaxHeight(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tabs.forEach { tab ->
                        val isSelected = currentTab == tab
                        TabIcon(
                            tab = tab,
                            isSelected = isSelected,
                            modifier = Modifier.width(TabCell),
                            onClick = {
                                when {
                                    !isSelected -> onTabSelected(tab)
                                    tab == MyntTab.HISTORY -> onAddClick()
                                }
                            }
                        )
                    }
                }
            }

            // The paint itself, growing from the left edge.
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        scaleX = paint
                        transformOrigin = TransformOrigin(0f, 0.5f)
                    }
                    .background(MaterialTheme.colorScheme.primary)
            )

            // The button label, fading in once the paint has mostly covered the pill.
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer { alpha = labelAlpha },
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = splitLabel,
                    transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
                    label = "splitLabel"
                ) { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            // Catches taps only while the pill is a button, so the tabs work normally otherwise.
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .then(if (splitMode) Modifier.clickable(onClick = onSplitClick) else Modifier)
            )
        }
    }
}

@Composable
private fun TabIcon(
    tab: MyntTab,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(if (isSelected) 1.06f else 1f, label = "tabScale")
    val contentColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.onSurface
        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
        label = "tabContentColor"
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .semantics {
                role = Role.Tab
                selected = isSelected
                contentDescription = if (isSelected && tab == MyntTab.HISTORY) "Add transaction" else tab.title
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null, // the sliding indicator is the feedback
                onClick = onClick
            )
            .scale(scale),
        contentAlignment = Alignment.Center
    ) {
        if (tab == MyntTab.HISTORY) {
            // Slides up into "+" once selected, back down when leaving.
            AnimatedContent(
                targetState = isSelected,
                transitionSpec = {
                    val entering = targetState
                    val enter = slideInVertically(tween(260, easing = FastOutSlowInEasing)) { h -> if (entering) h else -h } +
                            fadeIn(tween(200))
                    val exit = slideOutVertically(tween(260, easing = FastOutSlowInEasing)) { h -> if (entering) -h else h } +
                            fadeOut(tween(200))
                    enter togetherWith exit using SizeTransform(clip = false)
                },
                label = "historyAddSwap"
            ) { selected ->
                Icon(
                    imageVector = if (selected) Icons.Rounded.Add else tab.unselectedIcon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(26.dp)
                )
            }
        } else {
            Crossfade(targetState = isSelected, label = "iconToggle") { selected ->
                Icon(
                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}

/**
 * The orb, laid out at the pill's height so it never pushes the bar around.
 * In [deleteMode] it turns into a red delete button.
 */
@Composable
private fun VoiceOrbButton(
    onClick: () -> Unit,
    deleteMode: Boolean,
    onDeleteClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "orbPress"
    )

    Box(
        modifier = Modifier
            .size(PillHeight)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = if (deleteMode) "Delete selected settle-up" else "Add by voice"
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = if (deleteMode) onDeleteClick else onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = deleteMode,
            transitionSpec = {
                (fadeIn(tween(220)) + scaleIn(tween(260, easing = FastOutSlowInEasing), initialScale = 0.6f)) togetherWith
                        (fadeOut(tween(160)) + scaleOut(tween(200), targetScale = 0.6f)) using
                        SizeTransform(clip = false)
            },
            contentAlignment = Alignment.Center,
            label = "orbOrDelete"
        ) { isDelete ->
            if (isDelete) {
                Box(
                    modifier = Modifier
                        .size(PillHeight - 8.dp)
                        .scale(pressScale)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onError,
                        modifier = Modifier.size(28.dp)
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(PillHeight + 60.dp)
                        .scale(pressScale),
                    contentAlignment = Alignment.Center
                ) {
                    OrbEnergy(modifier = Modifier.fillMaxSize())
                    Icon(
                        imageVector = Icons.Rounded.Mic,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.92f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}