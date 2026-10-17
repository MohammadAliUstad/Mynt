package com.yugentech.mynt.ui.shared.components.orb

import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val DEG = (PI / 180.0).toFloat()
private const val TWO_PI = (2.0 * PI).toFloat()

/**
 * Ambient home-screen orb. Mono-primary, no Modifier.blur — softness is gradient-only,
 * brushes built once via drawWithCache, only the draw phase runs per frame.
 */
@Composable
fun AnimatingOrb(
    modifier: Modifier = Modifier,
    amplitude: Float = 0f,
    orbSize: Dp = 260.dp,
    onClick: () -> Unit = {}
) {
    val t = rememberInfiniteTransition(label = "Orb")

    // Additional smoothing at the UI level for ultra-fluid response
    val smoothedAmplitude by animateFloatAsState(
        targetValue = amplitude,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow),
        label = "smoothedAmplitude"
    )

    // Counter-rotating drivers + breathing + core flicker + organic drift.
    val swirlOuter by t.animateFloat(
        0f, 360f, infiniteRepeatable(tween(11000, easing = LinearEasing)), label = "swirlOuter"
    )
    val swirlInner by t.animateFloat(
        360f, 0f, infiniteRepeatable(tween(7000, easing = LinearEasing)), label = "swirlInner"
    )
    val breatheBase by t.animateFloat(
        0.92f, 1.10f, // Wider base range for more "life" in neutral state
        infiniteRepeatable(tween(3200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breathe"
    )
    
    // Louder = Larger swell (Dramatic response from 0.0 base)
    val reactiveBreathe = breatheBase + (smoothedAmplitude * 0.85f)

    val corePulseBase by t.animateFloat(
        0.78f, 1f,
        infiniteRepeatable(tween(1900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "corePulse"
    )
    // Louder = Brighter, more energized core
    val reactiveCorePulse = (corePulseBase + smoothedAmplitude * 0.9f).coerceAtMost(2.0f)

    val driftX by t.animateFloat(
        0f, TWO_PI, infiniteRepeatable(tween(8000, easing = LinearEasing)), label = "driftX"
    )
    val driftY by t.animateFloat(
        0f, TWO_PI, infiniteRepeatable(tween(5300, easing = LinearEasing)), label = "driftY"
    )
    
    // Free-running phases for sparks
    val phaseA by t.animateFloat(
        0f, TWO_PI, infiniteRepeatable(tween(4300, easing = LinearEasing)), label = "phaseA"
    )
    val phaseB by t.animateFloat(
        0f, TWO_PI, infiniteRepeatable(tween(6100, easing = LinearEasing)), label = "phaseB"
    )

    val primary = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier.size(orbSize),
        contentAlignment = Alignment.Center
    )  {
        Box(
            Modifier
                .fillMaxSize()
                .drawWithCache {
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
                        center = Offset.Zero, radius = r * 0.80f
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
                            translate(c.x, c.y) { drawCircle(body, r * 0.80f, Offset.Zero) }

                            // Orbiting fields expand with amplitude
                            val orbitBoost = 1f + smoothedAmplitude * 0.15f
                            drawBlob(blob(primary, r * 0.68f), c, orbit = r * 0.30f * orbitBoost, angleDeg = swirlOuter, radius = r * 0.68f)
                            drawBlob(blob(light, r * 0.62f), c, orbit = r * 0.34f * orbitBoost, angleDeg = swirlInner + 140f, radius = r * 0.62f)
                            drawBlob(blob(deep, r * 0.62f), c, orbit = r * 0.28f * orbitBoost, angleDeg = swirlOuter + 250f, radius = r * 0.62f)
                            drawBlob(blob(primary, r * 0.56f), c, orbit = r * 0.16f * orbitBoost, angleDeg = swirlInner + 60f, radius = r * 0.56f)

                            val coreC = c + Offset(cos(driftX) * r * 0.08f, sin(driftY) * r * 0.08f)
                            translate(coreC.x, coreC.y) {
                                drawCircle(core, r * 0.46f, Offset.Zero, alpha = reactiveCorePulse)
                            }

                            // Sparks brighten and push outward
                            val sparkOpacityBoost = 1f + smoothedAmplitude * 0.8f
                            val sparkSpread = 1f + smoothedAmplitude * 0.25f
                            
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

        // Tap target covers only the visible body, not the faded aura or the
        // transparent corners of the 260dp canvas — keeps nearby UI tappable.
        Box(
            Modifier
                .size(orbSize * 0.65f)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                )
        )
    }
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