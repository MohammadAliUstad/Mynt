package com.yourname.expensetracker

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.yourname.expensetracker.ui.theme.ExpenseTrackerTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

/**
 * Full-screen Voice Screen overlay powered by Soniox STT via Firebase Cloud Functions,
 * displaying high-accuracy transcripts and structured transaction parsed cards.
 * Transcribed text is parsed by Gemini (via Firebase) into one or more transactions.
 */
@Composable
fun VoiceScreen(
    onCloseClick: () -> Unit,
    onSaveTransaction: (Expense) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Helpers copied directly from Amatyaa STT architecture
    val androidAudioRecorder = remember { AndroidAudioRecorder(context) }
    val audioBuffer = remember { ByteArrayOutputStream() }

    var recordingJob by remember { mutableStateOf<Job?>(null) }
    var isRecording by remember { mutableStateOf(false) }
    var isSonioxProcessing by remember { mutableStateOf(false) }
    var sonioxTranscript by remember { mutableStateOf("") }
    var sonioxError by remember { mutableStateOf<String?>(null) }
    var parsedTransactions by remember { mutableStateOf<List<Expense>>(emptyList()) }

    // Release resources on screen close
    DisposableEffect(Unit) {
        onDispose {
            recordingJob?.cancel()
            androidAudioRecorder.stopRecording()
        }
    }

    // Permission state tracking
    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasMicPermission = isGranted
        if (isGranted) {
            sonioxTranscript = ""
            parsedTransactions = emptyList()
            sonioxError = null
            isSonioxProcessing = false
            isRecording = true
            audioBuffer.reset()

            recordingJob?.cancel()
            recordingJob = coroutineScope.launch {
                androidAudioRecorder.startRecording()
                    .catch { e ->
                        isRecording = false
                        sonioxError = e.localizedMessage ?: "Audio recording error"
                    }
                    .collect { chunk ->
                        audioBuffer.write(chunk)
                    }
            }
        }
    }

    fun startListeningFlow() {
        if (hasMicPermission) {
            sonioxTranscript = ""
            parsedTransactions = emptyList()
            sonioxError = null
            isSonioxProcessing = false
            isRecording = true
            audioBuffer.reset()

            recordingJob?.cancel()
            recordingJob = coroutineScope.launch {
                androidAudioRecorder.startRecording()
                    .catch { e ->
                        isRecording = false
                        sonioxError = e.localizedMessage ?: "Audio recording error"
                    }
                    .collect { chunk ->
                        audioBuffer.write(chunk)
                    }
            }
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    fun stopAndTranscribeFlow() {
        isRecording = false
        recordingJob?.cancel()
        androidAudioRecorder.stopRecording()

        val capturedPcmBytes = audioBuffer.toByteArray()

        if (capturedPcmBytes.isNotEmpty()) {
            isSonioxProcessing = true
            coroutineScope.launch {
                val result = SonioxFirebaseHelper.transcribeAudio(capturedPcmBytes)
                if (result.isSuccess) {
                    val text = result.getOrNull() ?: ""

                    // Gemini parsing (supports multiple transactions, Hindi/English).
                    // Falls back to the old local parser if Gemini fails.
                    val aiParsed = SonioxFirebaseHelper.parseTransactions(text).getOrNull()
                    parsedTransactions = if (!aiParsed.isNullOrEmpty()) {
                        aiParsed
                    } else {
                        listOfNotNull(VoiceTransactionParser.parse(text))
                    }

                    sonioxTranscript = text
                } else {
                    sonioxError = result.exceptionOrNull()?.localizedMessage ?: "Soniox transcription failed."
                }
                isSonioxProcessing = false
            }
        } else {
            sonioxError = "No speech audio captured. Please try again."
        }
    }

    // Auto-start recording on open
    LaunchedEffect(Unit) {
        startListeningFlow()
    }

    // Pulsing animation when listening
    val infiniteTransition = rememberInfiniteTransition(label = "pulseTransition")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    actionColor = MaterialTheme.colorScheme.primary,
                    dismissActionContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.96f)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(20.dp)
        ) {
            // Close Button
            IconButton(
                onClick = onCloseClick,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(28.dp)
                )
            }

            // Center Column Content
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Mic Button
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(160.dp)
                        .clickable {
                            if (!hasMicPermission) {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            } else {
                                if (isRecording) {
                                    stopAndTranscribeFlow()
                                } else {
                                    startListeningFlow()
                                }
                            }
                        }
                ) {
                    if (isRecording) {
                        Box(
                            modifier = Modifier
                                .size(140.dp)
                                .scale(pulseScale)
                                .alpha(pulseAlpha)
                                .clip(CircleShape)
                                .background(GoldAccent)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(GoldAccent),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSonioxProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(40.dp),
                                color = Color.Black,
                                strokeWidth = 3.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Voice Input",
                                tint = Color.Black,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // STATUS / INSTRUCTION TEXT
                when {
                    !hasMicPermission -> {
                        Text(
                            text = "Microphone Permission Required",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Please allow microphone access to use Soniox voice recognition.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { openAppSettings(context) },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Open App Settings")
                        }
                    }

                    isSonioxProcessing -> {
                        Text(
                            text = "Transcribing with Soniox Cloud AI...",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Securely processing audio through Firebase",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )
                    }

                    isRecording -> {
                        Text(
                            text = "Listening... (Tap mic when done)",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Try saying: \"Spent 4100 on EMI on 2nd October\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }

                    sonioxError != null -> {
                        Text(
                            text = "Voice Recognition Error",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = sonioxError!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { startListeningFlow() },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Retry")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Try Again")
                        }
                    }

                    else -> {
                        Text(
                            text = "Tap mic to speak",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

                // RECOGNIZED TEXT & PARSED TRANSACTION CARD
                if (sonioxTranscript.isNotEmpty() && !isSonioxProcessing) {
                    Spacer(modifier = Modifier.height(24.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Soniox AI Transcript:",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "\"$sonioxTranscript\"",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )

                            // PARSED TRANSACTION DETAILS (one card per detected transaction)
                            parsedTransactions.forEach { parsedTransaction ->
                                Spacer(modifier = Modifier.height(16.dp))

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                val typeLabel = if (parsedTransaction.type == TransactionType.INCOME) "INCOME" else "EXPENSE"
                                                val typeColor = if (parsedTransaction.type == TransactionType.INCOME) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error

                                                Text(
                                                    text = typeLabel,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = typeColor,
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(typeColor.copy(alpha = 0.15f))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                )

                                                Spacer(modifier = Modifier.width(8.dp))

                                                Text(
                                                    text = "${parsedTransaction.category} • ${parsedTransaction.date}",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(4.dp))

                                            Text(
                                                text = parsedTransaction.title,
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        val formatted = SettingsManager.formatAmount(parsedTransaction.amount)
                                        val amountColor = if (parsedTransaction.type == TransactionType.INCOME) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurface

                                        Text(
                                            text = if (parsedTransaction.type == TransactionType.INCOME) "+$formatted" else formatted,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = amountColor
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // ACTION BUTTONS: "Try again" & "Save Transaction"
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { startListeningFlow() },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Try again")
                                }

                                Button(
                                    onClick = {
                                        if (parsedTransactions.isNotEmpty()) {
                                            parsedTransactions.forEach { onSaveTransaction(it) }
                                        } else {
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("Could not detect amount. Please try again.")
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (parsedTransactions.size > 1) "Save all" else "Save")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Helper to open Android app settings page.
 */
private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
    }
    context.startActivity(intent)
}

@Preview(showBackground = true, name = "Voice Screen Preview")
@Composable
fun VoiceScreenPreview() {
    ExpenseTrackerTheme {
        VoiceScreen(onCloseClick = {})
    }
}