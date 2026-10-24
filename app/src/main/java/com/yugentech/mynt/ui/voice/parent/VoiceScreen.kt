package com.yugentech.mynt.ui.voice.parent

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yugentech.mynt.database.model.Expense
import com.yugentech.mynt.split.model.SplitBill
import com.yugentech.mynt.ui.shared.components.CategoryBadge
import com.yugentech.mynt.ui.shared.components.MyntCard
import com.yugentech.mynt.ui.shared.components.Pill
import com.yugentech.mynt.ui.shared.components.displayTitle
import com.yugentech.mynt.ui.shared.components.orb.AnimatingOrb
import com.yugentech.mynt.ui.shared.components.signedAmount
import com.yugentech.mynt.ui.shared.util.FinanceColors
import com.yugentech.mynt.ui.shared.util.LocalCurrencySymbol
import com.yugentech.mynt.ui.shared.util.categoryStyle
import com.yugentech.mynt.ui.shared.util.formatAmount
import com.yugentech.mynt.ui.split.parent.SplitSession
import com.yugentech.mynt.voice.model.ParsedVoiceResult
import com.yugentech.mynt.voice.viewmodel.VoiceViewModel
import kotlinx.coroutines.delay
import java.time.format.DateTimeFormatter

private val examplePhrases = listOf(
    "\"Lunch was 490, 5 of us: me, Julez, Arbaz, Shahbaz, Altamash. Arbaz paid\"",
    "\"Spent 450 on groceries\"",
    "\"Got my salary of 52,000 yesterday\"",
    "\"Lent 2000 to Rahul for rent\""
)

@Composable
fun VoiceScreen(
    viewModel: VoiceViewModel,
    existingExpenses: List<Expense>,
    onCloseClick: () -> Unit,
    onConfirmTransactions: (List<ParsedVoiceResult>) -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val amplitude by viewModel.amplitude.collectAsStateWithLifecycle()

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }
    var permissionDenied by remember { mutableStateOf(false) }
    var editingIndex by remember { mutableStateOf<Int?>(null) }
    var wasRecordingBeforePause by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasMicPermission = isGranted
        permissionDenied = !isGranted
        if (isGranted) viewModel.startRecording()
    }

    val activePeople = SplitSession.state.people.toList()
    LaunchedEffect(existingExpenses, activePeople) {
        viewModel.setContext(existingExpenses, activePeople)
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                wasRecordingBeforePause = viewModel.uiState.value.isRecording
                if (wasRecordingBeforePause) {
                    viewModel.pauseRecording()
                }
            } else if (event == Lifecycle.Event.ON_RESUME) {
                if (wasRecordingBeforePause && hasMicPermission) {
                    viewModel.startRecording(clearBuffer = false)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        if (hasMicPermission) viewModel.onScreenOpened(hasMicPermission = true)
        else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    fun onOrbTap() {
        when {
            !hasMicPermission -> permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            uiState.isRecording -> viewModel.stopAndTranscribe(existingExpenses)
            !uiState.isProcessing -> viewModel.startRecording()
        }
    }

    BackHandler(enabled = uiState.isRecording) { viewModel.cancelRecording() }

    val hasResults = uiState.parsedResults.isNotEmpty() && !uiState.isRecording && !uiState.isProcessing
    val firstSplit = uiState.parsedResults.firstOrNull { it.isSplit }?.splitBill
    val knownPeople = remember(existingExpenses) {
        existingExpenses.map { it.personName }.filter { it.isNotBlank() }.distinct()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(if (hasResults) 48.dp else 96.dp))

            AnimatingOrb(
                amplitude = if (uiState.isRecording) amplitude else 0f,
                orbSize = if (hasResults) 180.dp else 260.dp,
                onClick = ::onOrbTap,
                modifier = Modifier.animateContentSize()
            )

            Spacer(Modifier.height(12.dp))

            StatusText(
                isRecording = uiState.isRecording,
                isProcessing = uiState.isProcessing,
                elapsedSeconds = uiState.elapsedSeconds,
                error = uiState.error,
                hasMicPermission = hasMicPermission,
                permissionDenied = permissionDenied,
                heardNothingUseful = uiState.heardNothingUseful,
                hasResults = hasResults,
                clarificationPrompt = uiState.clarificationPrompt,
                clarificationOptions = uiState.clarificationOptions,
                onRetry = { viewModel.startRecording() },
                onOpenSettings = { openAppSettings(context) },
                onSubmitClarification = { answer -> viewModel.submitClarification(answer, existingExpenses) }
            )

            if (uiState.transcript.isNotBlank() && !uiState.isProcessing && !uiState.isRecording) {
                Spacer(Modifier.height(20.dp))
                Text(
                    text = "“${uiState.transcript}”",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            AnimatedVisibility(
                visible = hasResults,
                enter = fadeIn() + slideInVertically { it / 4 },
                exit = fadeOut()
            ) {
                Column(
                    modifier = Modifier.padding(top = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (uiState.parsedResults.any { !it.isSplit && it.expense != null }) {
                        Hint("Tap an entry to edit it")
                    }
                    uiState.parsedResults.forEachIndexed { index, result ->
                        if (result.isSplit && result.splitBill != null) {
                            SplitResultCard(
                                split = result.splitBill,
                                people = result.participants,
                                onRemove = { viewModel.removeResult(index) }
                            )
                        } else if (result.expense != null) {
                            ParsedResultCard(
                                result = result,
                                onClick = { editingIndex = index },
                                onRemove = { viewModel.removeResult(index) }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(160.dp))
        }

        IconButton(
            onClick = {
                if (uiState.isRecording || uiState.clarificationPrompt != null) viewModel.cancelRecording()
                onCloseClick()
            },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
        ) {
            Icon(Icons.Rounded.Close, contentDescription = "Close")
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            when {
                uiState.isRecording -> Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalIconButton(
                        onClick = { viewModel.cancelRecording() },
                        modifier = Modifier.size(56.dp)
                    ) { Icon(Icons.Rounded.Close, contentDescription = "Discard") }
                    FilledIconButton(
                        onClick = { viewModel.stopAndTranscribe(existingExpenses) },
                        modifier = Modifier.size(72.dp),
                        colors = IconButtonDefaults.filledIconButtonColors()
                    ) { Icon(Icons.Rounded.Check, contentDescription = "Done", modifier = Modifier.size(32.dp)) }
                }

                hasResults -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { viewModel.startRecording() },
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = CircleShape
                    ) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Redo")
                    }
                    Button(
                        onClick = { onConfirmTransactions(uiState.parsedResults) },
                        modifier = Modifier.weight(1.4f).height(56.dp),
                        shape = CircleShape
                    ) {
                        Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (firstSplit != null) "Add bill" else if (uiState.parsedResults.size > 1) "Save ${uiState.parsedResults.size} entries" else "Save entry",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        val editingResult = editingIndex?.let { uiState.parsedResults.getOrNull(it) }
        val editingExpense = editingResult?.expense
        if (hasResults && editingIndex != null && editingResult != null && !editingResult.isSplit && editingExpense != null) {
            EditVoiceResultSheet(
                expense = editingExpense,
                knownPeople = knownPeople,
                onDismiss = { editingIndex = null },
                onSave = { updatedExpense ->
                    viewModel.updateResult(editingIndex!!, editingResult.copy(expense = updatedExpense))
                    editingIndex = null
                }
            )
        }
    }
}

@Composable
private fun StatusText(
    isRecording: Boolean,
    isProcessing: Boolean,
    elapsedSeconds: Int,
    error: String?,
    hasMicPermission: Boolean,
    permissionDenied: Boolean,
    heardNothingUseful: Boolean,
    hasResults: Boolean,
    clarificationPrompt: String?,
    clarificationOptions: List<String>,
    onRetry: () -> Unit,
    onOpenSettings: () -> Unit,
    onSubmitClarification: (String) -> Unit
) {
    var exampleIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(isRecording) {
        while (isRecording) {
            delay(3200)
            exampleIndex = (exampleIndex + 1) % examplePhrases.size
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        when {
            !hasMicPermission -> {
                Icon(Icons.Rounded.MicOff, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(8.dp))
                Headline("Mynt needs the microphone")
                Hint("Voice entries are transcribed securely and never stored as audio.")
                if (permissionDenied) {
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onOpenSettings, shape = CircleShape) { Text("Open settings") }
                }
            }
            isProcessing -> {
                Headline("Working it out…")
                Hint("Turning what you said into entries")
            }
            clarificationPrompt != null && isRecording -> {
                Headline(clarificationPrompt)

                if (clarificationOptions.isNotEmpty()) {
                    Spacer(Modifier.height(24.dp))
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth(0.85f)
                    ) {
                        clarificationOptions.forEachIndexed { index, option ->
                            // Creates a staggered slide-in animation for each card
                            var visible by remember { mutableStateOf(false) }
                            LaunchedEffect(Unit) {
                                delay(index * 150L) // Stagger delay
                                visible = true
                            }

                            AnimatedVisibility(
                                visible = visible,
                                enter = fadeIn() + slideInVertically { it / 3 }
                            ) {
                                MyntCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = { onSubmitClarification(option) },
                                    contentPadding = 18.dp,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(24.dp)
                                ) {
                                    Text(
                                        text = option,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Hint("Listening for your answer...")
                }
            }
            isRecording -> {
                Headline("Listening  ${elapsedSeconds / 60}:${"%02d".format(elapsedSeconds % 60)}")
                Spacer(Modifier.height(6.dp))
                AnimatedContent(targetState = exampleIndex, label = "examplePhrase") { index ->
                    Hint("Try ${examplePhrases[index]}")
                }
            }
            error != null -> {
                Headline("Couldn't catch that")
                Hint(error)
                Spacer(Modifier.height(16.dp))
                Button(onClick = onRetry, shape = CircleShape) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Try again")
                }
            }
            heardNothingUseful -> {
                Headline("No amount found")
                Hint("Mention how much, like \"lunch was 490\".")
                Spacer(Modifier.height(16.dp))
                Button(onClick = onRetry, shape = CircleShape) { Text("Try again") }
            }
            hasResults -> Headline("Here's what I got")
            else -> {
                Headline("Tap the orb and speak")
                Hint("Say a transaction or a split bill")
            }
        }
    }
}

@Composable
private fun Headline(text: String) {
    Text(text, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center)
}

@Composable
private fun Hint(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
}

@Composable
private fun SplitResultCard(split: SplitBill, people: List<String>, onRemove: () -> Unit) {
    val currency = LocalCurrencySymbol.current
    val count = maxOf(people.size, split.shares.size)
    MyntCard(modifier = Modifier.fillMaxWidth(), contentPadding = 14.dp, shape = RoundedCornerShape(22.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.ReceiptLong, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        split.title,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(Modifier.width(8.dp))
                    Pill(text = "Split bill", color = FinanceColors.borrowed)
                }
                Text(
                    "$count people · paid by ${split.paidBy}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val notSharing = people.filter { p -> split.shares.keys.none { it.equals(p, ignoreCase = true) } }
                if (notSharing.isNotEmpty()) {
                    Text(
                        "Not sharing: ${notSharing.joinToString(", ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    split.date.format(DateTimeFormatter.ofPattern("d MMM yyyy")),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                formatAmount(split.amount, currency),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            IconButton(onClick = onRemove) {
                Icon(Icons.Rounded.Close, contentDescription = "Remove", modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun ParsedResultCard(result: ParsedVoiceResult, onClick: () -> Unit, onRemove: () -> Unit) {
    val expense = result.expense ?: return
    val previous = result.previous
    MyntCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick),
        contentPadding = 14.dp,
        shape = RoundedCornerShape(22.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CategoryBadge(style = categoryStyle(expense.category, expense.type))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        expense.displayTitle,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (result.isUpdate) {
                        Spacer(Modifier.width(8.dp))
                        Pill(text = "Update", color = FinanceColors.lent)
                    }
                }
                Text(
                    "${expense.category} · ${expense.date}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                signedAmount(expense),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = FinanceColors.forType(expense.type)
            )
            IconButton(onClick = onRemove) {
                Icon(Icons.Rounded.Close, contentDescription = "Remove", modifier = Modifier.size(18.dp))
            }
        }
    }
}

private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
    }
    context.startActivity(intent)
}