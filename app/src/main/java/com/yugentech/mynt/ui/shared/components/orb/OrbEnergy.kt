package com.yugentech.mynt.ui.shared.components.orb

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val DEG = (PI / 180.0).toFloat()
private const val TWO_PI = (2.0 * PI).toFloat()

/**
 * Lightweight living orb (button version).
 */
@Composable
fun OrbEnergy(
    modifier: Modifier = Modifier,
    speed: Float = 1f,
    amplitude: Float = 0f
) {
    val transition = rememberInfiniteTransition(label = "Orb")
    fun dur(base: Int) = (base / speed).toInt().coerceAtLeast(120)

    // UI-level smoothing
    val smoothedAmplitude by animateFloatAsState(
        targetValue = amplitude,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "smoothedAmplitude"
    )

    // Counter-rotating drivers + breathing + core flicker + organic drift.
    val spinCw by transition.animateFloat(
        0f, 360f, infiniteRepeatable(tween(dur(14000), easing = LinearEasing)), label = "cw"
    )
    val spinCcw by transition.animateFloat(
        360f, 0f, infiniteRepeatable(tween(dur(9000), easing = LinearEasing)), label = "ccw"
    )
    val breatheBase by transition.animateFloat(
        0.92f, 1.08f, // Wider neutral breathing
        infiniteRepeatable(tween(dur(2800), easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breathe"
    )
    // Louder = Larger swell (Dramatic response from 0.0)
    val reactiveBreathe = breatheBase + (smoothedAmplitude * 0.65f)

    val pulseBase by transition.animateFloat(
        0.70f, 1f,
        infiniteRepeatable(tween(dur(1500), easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )
    // Louder = Brighter core
    val reactivePulse = (pulseBase + smoothedAmplitude * 0.9f).coerceAtMost(2.0f)

    val driftX by transition.animateFloat(
        0f, TWO_PI, infiniteRepeatable(tween(dur(7000), easing = LinearEasing)), label = "dx"
    )
    val driftY by transition.animateFloat(
        0f, TWO_PI, infiniteRepeatable(tween(dur(5300), easing = LinearEasing)), label = "dy"
    )
    
    // Free-running phases for sparks
    val phaseA by transition.animateFloat(
        0f, TWO_PI, infiniteRepeatable(tween(dur(4300), easing = LinearEasing)), label = "phaseA"
    )
    val phaseB by transition.animateFloat(
        0f, TWO_PI, infiniteRepeatable(tween(dur(6100), easing = LinearEasing)), label = "phaseB"
    )

    val primary = MaterialTheme.colorScheme.primary

    Box(
        modifier.drawWithCache {
            val r = size.minDimension / 2f
            val light = lerp(primary, Color.White, 0.40f)
            val deep = lerp(primary, Color.Black, 0.22f)
            val coreMid = lerp(primary, Color.White, 0.25f)

            fun blob(tint: Color, radius: Float) = Brush.radialGradient(
                0.0f to tint.copy(alpha = 0.42f),
                0.45f to tint.copy(alpha = 0.14f),
                0.75f to tint.copy(alpha = 0.04f),
                1.0f to Color.Transparent,
                center = Offset.Zero, radius = radius
            )

            val aura = Brush.radialGradient(
                0.0f to primary.copy(alpha = 0.22f),
                0.6f to primary.copy(alpha = 0.06f),
                1.0f to Color.Transparent,
                center = Offset.Zero, radius = r
            )

            val body = Brush.radialGradient(
                0.0f to primary.copy(alpha = 0.62f),
                0.45f to primary.copy(alpha = 0.40f),
                0.72f to primary.copy(alpha = 0.12f),
                0.88f to primary.copy(alpha = 0.03f),
                1.0f to Color.Transparent,
                center = Offset.Zero, radius = r * 0.75f
            )
            
            val core = Brush.radialGradient(
                0.0f to Color.White.copy(alpha = 0.85f),
                0.4f to coreMid.copy(alpha = 0.45f),
                0.8f to coreMid.copy(alpha = 0.10f),
                1.0f to Color.Transparent,
                center = Offset.Zero, radius = r * 0.46f
            )
            
            val spark = Brush.radialGradient(
                0.0f to Color.White.copy(alpha = 0.55f),
                0.4f to Color.White.copy(alpha = 0.18f),
                0.75f to Color.White.copy(alpha = 0.04f),
                1.0f to Color.Transparent,
                center = Offset.Zero, radius = r * 0.24f
            )

            onDrawBehind {
                val c = center

                scale(reactiveBreathe, reactiveBreathe, pivot = c) {
                    translate(c.x, c.y) { drawCircle(aura, r, Offset.Zero) }
                    translate(c.x, c.y) { drawCircle(body, r * 0.75f, Offset.Zero) }

                    val orbitBoost = 1f + smoothedAmplitude * 0.1f
                    drawBlob(blob(primary, r * 0.68f), c, orbit = r * 0.30f * orbitBoost, angleDeg = spinCw, radius = r * 0.68f)
                    drawBlob(blob(light, r * 0.62f), c, orbit = r * 0.34f * orbitBoost, angleDeg = spinCcw + 140f, radius = r * 0.62f)
                    drawBlob(blob(deep, r * 0.62f), c, orbit = r * 0.28f * orbitBoost, angleDeg = spinCw + 250f, radius = r * 0.62f)
                    drawBlob(blob(primary, r * 0.56f), c, orbit = r * 0.16f * orbitBoost, angleDeg = spinCcw + 60f, radius = r * 0.56f)

                    val coreC = c + Offset(cos(driftX) * r * 0.08f, sin(driftY) * r * 0.08f)
                    translate(coreC.x, coreC.y) {
                        drawCircle(core, r * 0.46f, Offset.Zero, alpha = reactivePulse)
                    }

                    val sparkOpacityBoost = 1f + smoothedAmplitude * 0.6f
                    val sparkSpread = 1f + smoothedAmplitude * 0.15f
                    val fadeA = (0.5f + 0.5f * sin(phaseA)) * sparkOpacityBoost
                    val fadeB = (0.5f + 0.5f * sin(phaseB)) * sparkOpacityBoost
                    
                    drawSpark(spark, c, rx = r * 0.34f * sparkSpread, ry = r * 0.26f * sparkSpread,
                        px = phaseA, py = phaseB, dot = r * 0.24f, alpha = fadeA.coerceAtMost(1f))
                    drawSpark(spark, c, rx = r * 0.40f * sparkSpread, ry = r * 0.46f * sparkSpread,
                        px = phaseB, py = phaseA, dot = r * 0.24f, alpha = fadeB.coerceAtMost(1f))
                }
            }
        }
    )
}

/** Positions a pre-built blob brush on its orbit. Reuses the brush via translate (no realloc). */
private fun DrawScope.drawBlob(
    brush: Brush, c: Offset, orbit: Float, angleDeg: Float, radius: Float
) {
    val a = angleDeg * DEG
    val pos = c + Offset(cos(a) * orbit, sin(a) * orbit)
    translate(pos.x, pos.y) { drawCircle(brush, radius, Offset.Zero) }
}

/** Soft spark on a smooth Lissajous path; fades via `alpha`. No summed-sine beats → no jerk. */
private fun DrawScope.drawSpark(
    brush: Brush, c: Offset, rx: Float, ry: Float, px: Float, py: Float, dot: Float, alpha: Float
) {
    // Each axis is a single clean sine at a different phase → smooth, organic, non-circular.
    val pos = c + Offset(cos(px) * rx, sin(py) * ry)
    translate(pos.x, pos.y) { drawCircle(brush, dot, Offset.Zero, alpha = alpha) }
}