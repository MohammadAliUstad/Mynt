package com.yugentech.mynt.ui.shared.components.hero

import com.yugentech.mynt.domain.model.HeroCardKind
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.yugentech.mynt.domain.model.CardAnimationStyle
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private const val CLOCK_PERIOD_MS = 24_000
private val TAU = (2 * PI).toFloat()

/**
 * One shared clock (0f..1f, repeating every 24s) for every card on screen, so the water on
 * the next card is already in step with the water on this one while you swipe.
 *
 * Every motion below uses a whole number of cycles per clock loop, which is why the
 * animation never visibly "jumps" when the clock wraps around.
 */
@Composable
fun rememberHeroClock(): State<Float> {
    val transition = rememberInfiniteTransition(label = "heroClock")
    return transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = CLOCK_PERIOD_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "heroClockValue"
    )
}

/**
 * The moving layer behind a hero card's text. Pass a null [clock] (or [CardAnimationStyle.NONE])
 * and nothing is drawn. The clock is only read while drawing, so the animation never causes
 * the card's contents to recompose.
 */
@Composable
fun HeroCardBackground(
    style: CardAnimationStyle,
    clock: State<Float>?,
    modifier: Modifier = Modifier
) {
    if (clock == null) return
    when (style) {
        CardAnimationStyle.WATER -> WaterTank(clock, modifier)
        CardAnimationStyle.AURORA -> AuroraGlow(clock, modifier)
        CardAnimationStyle.SHIMMER -> ShimmerSweep(clock, modifier)
        CardAnimationStyle.NONE -> Unit
    }
}

// ───────────────────────────── Water tank ─────────────────────────────

@Composable
private fun WaterTank(clock: State<Float>, modifier: Modifier) {
    Box(modifier = modifier) {
        // Soft layer: light shafts and a glow, blurred (blur needs Android 12+, otherwise it stays soft-edged by design).
        Canvas(modifier = Modifier.fillMaxSize().blur(18.dp)) {
            drawLightShafts(clock.value)
        }
        // Crisp layer: the water itself, the shiny surface line and the bubbles.
        Canvas(modifier = Modifier.fillMaxSize()) {
            val t = clock.value
            drawWater(t)
            drawBubbles(t)
        }
    }
}

private fun DrawScope.drawLightShafts(t: Float) {
    val w = size.width
    val h = size.height
    for (i in 0 until 3) {
        val sway = sin(t * TAU * 2 + i * 1.9f)
        val cx = w * (0.16f + 0.34f * i) + sway * w * 0.05f
        val topWidth = w * 0.10f
        val bottomWidth = w * 0.24f
        val slant = w * 0.14f
        val shaft = Path().apply {
            moveTo(cx - topWidth / 2, 0f)
            lineTo(cx + topWidth / 2, 0f)
            lineTo(cx + bottomWidth / 2 - slant, h)
            lineTo(cx - bottomWidth / 2 - slant, h)
            close()
        }
        drawPath(
            path = shaft,
            brush = Brush.verticalGradient(
                colors = listOf(Color.White.copy(alpha = 0.26f), Color.Transparent),
                startY = 0f,
                endY = h
            )
        )
    }
    // A broad glow drifting near the surface
    val glowCenter = Offset(w * (0.7f + 0.12f * cos(t * TAU)), h * 0.22f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White.copy(alpha = 0.22f), Color.Transparent),
            center = glowCenter,
            radius = w * 0.45f
        ),
        radius = w * 0.45f,
        center = glowCenter
    )
}

private fun DrawScope.wavePath(
    level: Float,
    amplitude: Float,
    cycles: Float,
    speed: Int,
    phase: Float,
    t: Float,
    closed: Boolean
): Path {
    val w = size.width
    val steps = 56
    val path = Path()
    for (i in 0..steps) {
        val x = w * i / steps
        val y = level + amplitude * sin(cycles * TAU * (x / w) + t * TAU * speed + phase)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    if (closed) {
        path.lineTo(w, size.height)
        path.lineTo(0f, size.height)
        path.close()
    }
    return path
}

private fun DrawScope.drawWater(t: Float) {
    val h = size.height
    val surface = h * 0.17f
    val amp = 4.dp.toPx()

    // Back wave: lighter, slower, sits behind
    drawPath(
        path = wavePath(surface - 3.dp.toPx(), amp * 1.2f, 1.3f, 2, 0.8f, t, closed = true),
        brush = Brush.verticalGradient(
            colors = listOf(Color.White.copy(alpha = 0.18f), Color.White.copy(alpha = 0.03f)),
            startY = surface - amp,
            endY = h
        )
    )

    // Front wave: the main body of water, lighter at the surface and deeper toward the bottom
    drawPath(
        path = wavePath(surface, amp, 1.8f, -3, 0f, t, closed = true),
        brush = Brush.verticalGradient(
            colors = listOf(Color.White.copy(alpha = 0.22f), Color.Black.copy(alpha = 0.20f)),
            startY = surface - amp,
            endY = h
        )
    )

    // Shiny surface line: a wide soft glow under a thin bright edge
    val line = wavePath(surface, amp, 1.8f, -3, 0f, t, closed = false)
    drawPath(
        path = line,
        color = Color.White.copy(alpha = 0.16f),
        style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round)
    )
    drawPath(
        path = line,
        color = Color.White.copy(alpha = 0.60f),
        style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
    )
}

private fun DrawScope.drawBubbles(t: Float) {
    val w = size.width
    val h = size.height
    val surface = h * 0.17f
    val count = 9
    for (i in 0 until count) {
        val x0 = ((i * 0.618034f) % 1f) * 0.84f + 0.08f
        val loops = 1 + (i % 3) // whole numbers, so the loop is seamless
        val progress = (t * loops + i / count.toFloat()) % 1f
        val travel = h - surface - 8.dp.toPx()
        val y = h - progress * travel
        val x = w * x0 + sin((progress * 2f + i) * TAU * 0.5f) * 7.dp.toPx()
        val radius = (2.5f + (i % 4) * 1.5f).dp.toPx()
        val alpha = min(progress * 5f, 1f) * (1f - progress * 0.6f)
        val center = Offset(x, y)

        drawCircle(Color.White.copy(alpha = 0.07f * alpha), radius, center)
        drawCircle(
            color = Color.White.copy(alpha = 0.40f * alpha),
            radius = radius,
            center = center,
            style = Stroke(width = 1.dp.toPx())
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.75f * alpha),
            radius = radius * 0.25f,
            center = center + Offset(-radius * 0.35f, -radius * 0.35f)
        )
    }
}

// ───────────────────────────── Aurora glow ─────────────────────────────

@Composable
private fun AuroraGlow(clock: State<Float>, modifier: Modifier) {
    Canvas(modifier = modifier.blur(36.dp)) {
        val t = clock.value
        val w = size.width
        val h = size.height
        // Only white and black washes over the card's own colour, so it always matches the theme.
        val tints = listOf(
            Color.White.copy(alpha = 0.34f),
            Color.Black.copy(alpha = 0.26f),
            Color.White.copy(alpha = 0.22f)
        )
        tints.forEachIndexed { i, tint ->
            val center = Offset(
                x = w * (0.5f + 0.38f * sin(t * TAU + i * 2.1f)),
                y = h * (0.5f + 0.34f * cos(t * TAU + i * 1.3f))
            )
            val radius = w * (0.34f + 0.05f * i)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(tint, Color.Transparent),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )
        }
    }
}

// ───────────────────────────── Shimmer sweep ─────────────────────────────

@Composable
private fun ShimmerSweep(clock: State<Float>, modifier: Modifier) {
    Canvas(modifier = modifier.blur(6.dp)) {
        val w = size.width
        val h = size.height
        // Four sweeps per clock loop; each one uses the first half of its slot and rests for the second.
        val slot = (clock.value * 4f) % 1f
        val sweep = (slot / 0.55f).coerceIn(0f, 1f)
        val x = -w * 0.4f + sweep * w * 1.8f
        val band = w * 0.22f
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.30f), Color.Transparent),
                start = Offset(x - band, 0f),
                end = Offset(x + band, h * 0.7f)
            )
        )
    }
}

/**
 * The Home-card choices from Settings, handed down through the composition so the Theme settings
 * screen can reach them without every screen in between passing them along.
 */
class HomeCardPreferences(
    val animationStyle: CardAnimationStyle,
    val onAnimationStyleChange: (CardAnimationStyle) -> Unit,
    val hiddenCards: Set<HeroCardKind>,
    val onCardVisibleChange: (HeroCardKind, Boolean) -> Unit
)

val LocalHomeCardPreferences = compositionLocalOf<HomeCardPreferences?> { null }