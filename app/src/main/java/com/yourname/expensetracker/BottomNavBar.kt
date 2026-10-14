package com.yourname.expensetracker

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yourname.expensetracker.ui.theme.ExpenseTrackerTheme

/**
 * Tab items for navigation bar.
 */
enum class NavTab {
    HOME,
    ADD,
    STATS,
    ME
}

// Gold Accent Color constant
val GoldAccent = Color(0xFFFFB800)

/**
 * Custom Floating Navigation Bar with translucent glassmorphic surface raised higher above the screen bottom.
 */
@Composable
fun BottomNavBar(
    selectedTab: NavTab,
    onTabSelected: (NavTab) -> Unit,
    onMicClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 20.dp, end = 20.dp, bottom = 20.dp, top = 8.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Floating Translucent Pill Container
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp),
            shape = RoundedCornerShape(34.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.68f),
            tonalElevation = 6.dp,
            shadowElevation = 10.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Home Tab
                NavItem(
                    icon = Icons.Default.Home,
                    label = "Home",
                    isSelected = selectedTab == NavTab.HOME,
                    onClick = { onTabSelected(NavTab.HOME) },
                    modifier = Modifier.weight(1f)
                )

                // 2. Add Tab
                NavItem(
                    icon = Icons.Default.Add,
                    label = "Add",
                    isSelected = selectedTab == NavTab.ADD,
                    onClick = { onTabSelected(NavTab.ADD) },
                    modifier = Modifier.weight(1f)
                )

                // Space in the center for the raised Mic button
                Spacer(modifier = Modifier.weight(1f))

                // 4. Stats Tab
                NavItem(
                    icon = Icons.Default.BarChart,
                    label = "Stats",
                    isSelected = selectedTab == NavTab.STATS,
                    onClick = { onTabSelected(NavTab.STATS) },
                    modifier = Modifier.weight(1f)
                )

                // 5. Me Tab
                NavItem(
                    icon = Icons.Default.Person,
                    label = "Me",
                    isSelected = selectedTab == NavTab.ME,
                    onClick = { onTabSelected(NavTab.ME) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 3. Raised Center Mic Button (Sits raised above the pill)
        CenterMicButton(
            onClick = onMicClick,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-16).dp)
        )
    }
}

/**
 * Individual Navigation Item for Home, Add, Stats, and Me with springy circular reveal hover effect.
 */
@Composable
private fun NavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    // Spring animation for icon scale bounce on selection
    val iconScale by animateFloatAsState(
        targetValue = if (isSelected) 1.15f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "navItemScale"
    )

    // Bouncy spring animation for circular reveal on hover
    val hoverProgress by animateFloatAsState(
        targetValue = if (isHovered && !isSelected) 1.0f else 0.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "hoverProgress"
    )

    Column(
        modifier = modifier
            .clip(CircleShape)
            .hoverable(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon container with circular selection highlight and expanding hover reveal
        Box(
            contentAlignment = Alignment.Center
        ) {
            // Subtle semi-transparent surface overlay that expands as a circle from center on hover
            if (hoverProgress > 0.001f && !isSelected) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .graphicsLayer {
                            scaleX = hoverProgress
                            scaleY = hoverProgress
                            alpha = hoverProgress.coerceIn(0f, 1f)
                        }
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                )
            }

            // Selected gold highlight circle
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) GoldAccent else Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    modifier = Modifier
                        .size(22.dp)
                        .scale(iconScale),
                    tint = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Label string
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
            color = if (isSelected) GoldAccent else MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Raised Gold Mic Button sitting above the center of the navigation pill.
 */
@Composable
private fun CenterMicButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Press scale spring animation
    val buttonScale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "micButtonScale"
    )

    Box(
        modifier = modifier
            .scale(buttonScale)
            .size(56.dp)
            .clip(CircleShape)
            .background(GoldAccent)
            .border(2.dp, MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = "Voice Input",
            tint = Color.Black,
            modifier = Modifier.size(28.dp)
        )
    }
}

@Preview(showBackground = true, name = "Bottom Navigation Bar Preview")
@Composable
fun BottomNavBarPreview() {
    ExpenseTrackerTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(MaterialTheme.colorScheme.background)
        ) {
            BottomNavBar(
                selectedTab = NavTab.HOME,
                onTabSelected = {},
                onMicClick = {}
            )
        }
    }
}
