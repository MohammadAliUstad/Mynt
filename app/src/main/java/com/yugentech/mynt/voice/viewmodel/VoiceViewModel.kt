package com.yugentech.mynt.voice.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yugentech.mynt.database.model.Expense
import com.yugentech.mynt.voice.model.ParsedVoiceResult
import com.yugentech.mynt.voice.recorder.AndroidAudioRecorder
import com.yugentech.mynt.voice.repository.VoiceRepository
import com.yugentech.mynt.voice.service.ClarificationException
import com.yugentech.mynt.voice.state.VoiceUiState
import com.yugentech.mynt.voice.tts.TextToSpeechHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import kotlin.math.sqrt

private const val MAX_RECORDING_SECONDS = 60

class VoiceViewModel(
    private val voiceRepository: VoiceRepository,
    private val audioRecorder: AndroidAudioRecorder,
    private val textToSpeechHelper: TextToSpeechHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow(VoiceUiState())
    val uiState: StateFlow<VoiceUiState> = _uiState.asStateFlow()

    private val _amplitude = MutableStateFlow(0f)
    val amplitude: StateFlow<Float> = _amplitude.asStateFlow()

    private val audioBuffer = ByteArrayOutputStream()
    private var recordingJob: Job? = null
    private var timerJob: Job? = null
    private var hasAutoStarted = false
    private var existingExpenses: List<Expense> = emptyList()
    private var activeSplitPeople: List<String> = emptyList()
    private var pendingTranscript: String = ""

    fun onScreenOpened(hasMicPermission: Boolean) {
        if (hasAutoStarted || !hasMicPermission) return
        hasAutoStarted = true
        startRecording()
    }

    fun startRecording(clearBuffer: Boolean = true) {
        hasAutoStarted = true
        if (clearBuffer) {
            audioBuffer.reset()
            _uiState.update { it.copy(elapsedSeconds = 0) }
        }
        _uiState.update { it.copy(isRecording = true, error = null) }

        recordingJob?.cancel()
        recordingJob = viewModelScope.launch {
            audioRecorder.startRecording()
                .catch { e ->
                    _amplitude.value = 0f
                    _uiState.update {
                        it.copy(isRecording = false, error = e.localizedMessage ?: "Audio recording error")
                    }
                }
                .collect { chunk ->
                    audioBuffer.write(chunk)
                    _amplitude.value = loudness(chunk)
                }
        }

        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            var seconds = _uiState.value.elapsedSeconds
            while (isActive && _uiState.value.isRecording) {
                delay(1000)
                seconds++
                _uiState.update { it.copy(elapsedSeconds = seconds) }
                if (seconds >= MAX_RECORDING_SECONDS) {
                    stopAndTranscribe(existingExpenses)
                    break
                }
            }
        }
    }

    fun pauseRecording() {
        if (!_uiState.value.isRecording) return
        recordingJob?.cancel()
        timerJob?.cancel()
        audioRecorder.stopRecording()
        _amplitude.value = 0f
        _uiState.update { it.copy(isRecording = false) }
    }

    fun setContext(expenses: List<Expense>, splitPeople: List<String>) {
        existingExpenses = expenses
        activeSplitPeople = splitPeople
    }

    fun stopAndTranscribe(existingExpenses: List<Expense>) {
        if (!_uiState.value.isRecording) return
        recordingJob?.cancel()
        timerJob?.cancel()
        audioRecorder.stopRecording()
        _amplitude.value = 0f

        val capturedPcmBytes = audioBuffer.toByteArray()
        if (capturedPcmBytes.isEmpty()) {
            _uiState.update {
                it.copy(isRecording = false, error = "No speech audio captured. Please try again.")
            }
            return
        }

        _uiState.update { it.copy(isRecording = false, isProcessing = true) }
        viewModelScope.launch {
            val result = voiceRepository.transcribeAudio(capturedPcmBytes)
            if (result.isSuccess) {
                val newText = result.getOrNull().orEmpty()
                val fullText = if (pendingTranscript.isNotEmpty() && newText.isNotBlank()) {
                    "$pendingTranscript. $newText"
                } else if (newText.isNotBlank()) {
                    newText
                } else {
                    pendingTranscript
                }

                _uiState.update { it.copy(transcript = fullText) }
                processText(fullText, existingExpenses)
            } else {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        error = result.exceptionOrNull()?.localizedMessage ?: "Transcription failed."
                    )
                }
            }
        }
    }

    private fun processText(fullText: String, existingExpenses: List<Expense>) {
        viewModelScope.launch {
            val parseResult = voiceRepository.parseTransactions(fullText, existingExpenses, activeSplitPeople)
            val exception = parseResult.exceptionOrNull()

            if (exception is ClarificationException) {
                pendingTranscript = fullText
                val prompt = exception.prompt

                // Matches the question text generated in VoiceService and provides tappable buttons
                val options = if (prompt.contains("split bill", ignoreCase = true) && prompt.contains("regular expense", ignoreCase = true)) {
                    listOf("Split bill", "Regular expense")
                } else if (prompt.contains("give the name", ignoreCase = true)) {
                    listOf("No, save without name")
                } else {
                    emptyList()
                }

                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        clarificationPrompt = prompt,
                        clarificationOptions = options
                    )
                }

                textToSpeechHelper.speak(prompt) {
                    if (_uiState.value.clarificationPrompt != null && !_uiState.value.isRecording) {
                        startRecording()
                    }
                }
            } else if (parseResult.isSuccess) {
                pendingTranscript = ""
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        clarificationPrompt = null,
                        clarificationOptions = emptyList(),
                        parsedResults = parseResult.getOrNull().orEmpty()
                    )
                }
            } else {
                _uiState.update {
                    it.copy(isProcessing = false, error = exception?.localizedMessage ?: "Parsing failed")
                }
            }
        }
    }

    fun submitClarification(answer: String, existingExpenses: List<Expense>) {
        textToSpeechHelper.stop()
        recordingJob?.cancel()
        timerJob?.cancel()
        audioRecorder.stopRecording()

        if (answer.isBlank()) {
            _uiState.update { it.copy(clarificationPrompt = null, clarificationOptions = emptyList()) }
            return
        }

        _uiState.update {
            it.copy(
                clarificationPrompt = null,
                clarificationOptions = emptyList(),
                isRecording = false,
                isProcessing = true
            )
        }
        val fullText = if (pendingTranscript.isNotEmpty()) "$pendingTranscript. $answer" else answer

        _uiState.update { it.copy(transcript = fullText) }
        processText(fullText, existingExpenses)
    }

    fun cancelClarification() {
        textToSpeechHelper.stop()
        cancelRecording()
        pendingTranscript = ""
        _uiState.update { it.copy(clarificationPrompt = null, clarificationOptions = emptyList()) }
    }

    fun cancelRecording() {
        // Stop a question that is still being read aloud, otherwise its "finished speaking"
        // callback could switch the mic back on after you've walked away.
        textToSpeechHelper.stop()
        recordingJob?.cancel()
        timerJob?.cancel()
        audioRecorder.stopRecording()
        audioBuffer.reset()
        _amplitude.value = 0f
        // The half-finished request goes too, so the next recording doesn't get glued onto it.
        pendingTranscript = ""
        _uiState.value = VoiceUiState()
    }

    fun removeResult(index: Int) {
        _uiState.update { state ->
            state.copy(parsedResults = state.parsedResults.filterIndexed { i, _ -> i != index })
        }
    }

    fun updateResult(index: Int, updated: ParsedVoiceResult) {
        _uiState.update { state ->
            if (index !in state.parsedResults.indices) {
                state
            } else {
                state.copy(
                    parsedResults = state.parsedResults.toMutableList().also { it[index] = updated }
                )
            }
        }
    }

    override fun onCleared() {
        textToSpeechHelper.destroy()
        recordingJob?.cancel()
        timerJob?.cancel()
        audioRecorder.stopRecording()
        super.onCleared()
    }

    private fun loudness(chunk: ByteArray): Float {
        if (chunk.size < 2) return 0f
        var sum = 0.0
        var count = 0
        var i = 0
        while (i + 1 < chunk.size) {
            val sample = (chunk[i].toInt() and 0xFF) or (chunk[i + 1].toInt() shl 8)
            val value = sample.toShort().toDouble()
            sum += value * value
            count++
            i += 2
        }
        val rms = sqrt(sum / count) / Short.MAX_VALUE
        return (sqrt(rms) * 1.8).toFloat().coerceIn(0f, 1f)
    }
}