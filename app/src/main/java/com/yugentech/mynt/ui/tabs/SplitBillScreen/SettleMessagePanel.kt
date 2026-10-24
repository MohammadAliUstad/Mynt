package com.yugentech.mynt.ui.split.parent

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.yugentech.mynt.split.model.SettledRecord
import com.yugentech.mynt.ui.shared.components.orb.OrbEnergy
import kotlinx.coroutines.delay
import java.time.format.DateTimeFormatter
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.roundToInt

private val PanelEase = CubicBezierEasing(0.25f, 1f, 0.5f, 1f)
private val PanelRadius = 26.dp
private val GlowThickness = 2.5.dp

/**
 * Grows out of the selected settle-up card into a half-screen panel, shows an AI
 * "thinking" animation while Gemini writes, then reveals the message.
 */
@Composable
internal fun SettleMessageOverlay(state: SplitScreenState) {
    val target = state.panelRecord
    var shown by remember { mutableStateOf<SettledRecord?>(null) }
    val progress = remember { Animatable(0f) }

    LaunchedEffect(target) {
        if (target != null) {
            shown = target
            progress.animateTo(1f, tween(520, easing = PanelEase))
        } else if (shown != null) {
            progress.animateTo(0f, tween(360, easing = FastOutSlowInEasing))
            shown = null
        }
    }

    shown?.let { record ->
        ExpandingPanel(record = record, p = progress.value, state = state)
    }
}

@Composable
private fun ExpandingPanel(record: SettledRecord, p: Float, state: SplitScreenState) {
    val density = LocalDensity.current
    val scheme = MaterialTheme.colorScheme
    var origin by remember { mutableStateOf(Offset.Zero) }
    val panelColor = lerp(scheme.surfaceContainer, scheme.surfaceContainerHigh, p)

    // Rotating AI-style glow around the panel edge while the message is being written.
    val transition = rememberInfiniteTransition(label = "panelGlow")
    val angle = transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(3600, easing = LinearEasing)),
        label = "glowAngle"
    )
    val glowTarget = when (state.messageState) {
        is MessageState.Loading -> 1f
        is MessageState.Ready -> 0.35f
        else -> 0f
    }
    val glowAlpha by animateFloatAsState(glowTarget, tween(500), label = "glowAlpha")
    val glowA = scheme.primary
    val glowB = scheme.tertiary
    val glowC = scheme.secondary

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { origin = it.positionInRoot() }
    ) {
        val containerW = constraints.maxWidth.toFloat()
        val containerH = constraints.maxHeight.toFloat()
        val margin = with(density) { 16.dp.toPx() }
        val bottomGap = WindowInsets.navigationBars.getBottom(density) + with(density) { 124.dp.toPx() }
        val topInset = WindowInsets.statusBars.getTop(density)

        val endW = containerW - margin * 2
        val endH = min(containerH * 0.52f, containerH - bottomGap - topInset - margin)
        val endL = margin
        val endT = containerH - bottomGap - endH

        val start = state.cardBounds[record.id]?.translate(-origin)
            ?: Rect(
                endL,
                endT + endH * 0.6f,
                endL + endW,
                endT + endH * 0.6f + with(density) { 96.dp.toPx() }
            )

        val left = start.left + (endL - start.left) * p
        val top = start.top + (endT - start.top) * p
        val width = start.width + (endW - start.width) * p
        val height = start.height + (endH - start.height) * p

        val contentAlpha = ((p - 0.55f) / 0.45f).coerceIn(0f, 1f)
        val contentW = with(density) { endW.toDp() } - GlowThickness * 2
        val contentH = with(density) { endH.toDp() } - GlowThickness * 2

        // Dim the screen behind; tapping it closes the panel.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = p }
                .background(Color.Black.copy(alpha = 0.5f))
                .pointerInput(Unit) { detectTapGestures { state.closePanel() } }
        )

        Box(
            modifier = Modifier
                .offset { IntOffset(left.roundToInt(), top.roundToInt()) }
                .size(with(density) { width.toDp() }, with(density) { height.toDp() })
                .graphicsLayer { alpha = (p * 8f).coerceAtMost(1f) }
                .clip(RoundedCornerShape(PanelRadius))
                .background(panelColor)
                .drawBehind {
                    if (glowAlpha > 0.01f) {
                        val brush = Brush.sweepGradient(listOf(glowA, glowB, glowC, glowA))
                        rotate(angle.value) {
                            drawCircle(
                                brush = brush,
                                radius = hypot(size.width, size.height),
                                alpha = glowAlpha
                            )
                        }
                    }
                }
                // Swallows taps so touching the panel doesn't reach the dim layer.
                .pointerInput(Unit) { detectTapGestures { } }
        ) {
            Box(
                modifier = Modifier
                    .padding(GlowThickness)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(PanelRadius - GlowThickness))
                    .background(panelColor)
            ) {
                Box(
                    modifier = Modifier
                        .wrapContentSize(Alignment.TopStart, unbounded = true)
                        .size(contentW, contentH)
                        .graphicsLayer { alpha = contentAlpha }
                ) {
                    PanelContent(record = record, state = state)
                }
            }
        }
    }
}

@Composable
private fun PanelContent(record: SettledRecord, state: SplitScreenState) {
    val ms = state.messageState
    val locale = LocalLocale.current.platformLocale
    val scheme = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SparkleBadge(thinking = ms is MessageState.Loading)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Message for your friends",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    "Settled on ${record.settledOn.format(DateTimeFormatter.ofPattern("d MMM yyyy", locale))} · ${record.people.size} people",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { state.closePanel() }) {
                Icon(Icons.Rounded.Close, contentDescription = "Close")
            }
        }

        Spacer(Modifier.height(12.dp))

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            AnimatedContent(
                targetState = ms,
                contentKey = { it::class },
                transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
                label = "messageState"
            ) { current ->
                when (current) {
                    is MessageState.Ready -> MessageView(current.text)
                    is MessageState.Failed -> FailedView(current.message, onRetry = { state.retryMessage() })
                    else -> ThinkingView()
                }
            }
        }

        if (ms is MessageState.Ready) {
            Spacer(Modifier.height(10.dp))
            ActionRow(text = ms.text, onRegenerate = { state.retryMessage() })
        }
    }
}

@Composable
private fun SparkleBadge(thinking: Boolean) {
    val transition = rememberInfiniteTransition(label = "sparkle")
    val pulse by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )
    val scale = if (thinking) pulse else 1f
    Box(
        modifier = Modifier
            .size(38.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Rounded.AutoAwesome,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun ThinkingView() {
    val steps = remember {
        listOf(
            "Reading your settle-up…",
            "Working out who pays whom…",
            "Finding the right words…",
            "Polishing the message…"
        )
    }
    var index by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1800)
            index = (index + 1) % steps.size
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OrbEnergy(modifier = Modifier.size(84.dp), speed = 1.8f, amplitude = 0.3f)
        Spacer(Modifier.height(10.dp))
        AnimatedContent(
            targetState = index,
            transitionSpec = {
                (slideInVertically(tween(300)) { it / 2 } + fadeIn(tween(300))) togetherWith
                        (slideOutVertically(tween(200)) { -it / 2 } + fadeOut(tween(200)))
            },
            label = "thinkingStep"
        ) { i ->
            Text(
                steps[i],
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.height(18.dp))
        ShimmerLines()
    }
}

@Composable
private fun ShimmerLines() {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val x = transition.animateFloat(
        initialValue = -400f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing)),
        label = "shimmerX"
    )
    val base = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val highlight = MaterialTheme.colorScheme.primary.copy(alpha = 0.30f)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        listOf(1f, 0.92f, 0.78f, 0.5f).forEach { fraction ->
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .drawBehind {
                        drawRect(
                            Brush.linearGradient(
                                colors = listOf(base, highlight, base),
                                start = Offset(x.value, 0f),
                                end = Offset(x.value + 400f, 0f)
                            )
                        )
                    }
            )
        }
    }
}

@Composable
private fun MessageView(text: String) {
    // Types the message out, like a live AI response.
    var revealed by remember(text) { mutableIntStateOf(0) }
    LaunchedEffect(text) {
        while (revealed < text.length) {
            revealed = min(text.length, revealed + 3)
            delay(16)
        }
    }
    val end = if (revealed in 1 until text.length && text[revealed - 1].isHighSurrogate()) revealed + 1 else revealed

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = text.take(end),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun FailedView(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Rounded.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(36.dp)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(14.dp))
        Button(onClick = onRetry) {
            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Try again")
        }
    }
}

@Composable
private fun ActionRow(text: String, onRegenerate: () -> Unit) {
    val context = LocalContext.current
    var copied by remember(text) { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(1500)
            copied = false
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedButton(
            onClick = {
                context.getSystemService(ClipboardManager::class.java)
                    ?.setPrimaryClip(ClipData.newPlainText("Settle-up message", text))
                copied = true
            },
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                if (copied) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(if (copied) "Copied" else "Copy")
        }

        Button(
            onClick = {
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, text)
                }
                context.startActivity(Intent.createChooser(send, "Share message"))
            },
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Share")
        }

        IconButton(onClick = onRegenerate) {
            Icon(Icons.Rounded.Refresh, contentDescription = "Write it again")
        }
    }
}