package com.yugentech.mynt.voice.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yugentech.mynt.database.model.Expense
import com.yugentech.mynt.voice.recorder.AndroidAudioRecorder
import com.yugentech.mynt.voice.repository.VoiceRepository
import com.yugentech.mynt.voice.state.VoiceUiState
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

// Recording stops by itself after this long, so a forgotten session can't run up an
// enormous upload.
private const val MAX_RECORDING_SECONDS = 60

class VoiceViewModel(
    private val voiceRepository: VoiceRepository,
    private val audioRecorder: AndroidAudioRecorder
) : ViewModel() {

    private val _uiState = MutableStateFlow(VoiceUiState())
    val uiState: StateFlow<VoiceUiState> = _uiState.asStateFlow()

    /** Loudness of the microphone right now, 0 to 1, for the orb to react to. */
    private val _amplitude = MutableStateFlow(0f)
    val amplitude: StateFlow<Float> = _amplitude.asStateFlow()

    private val audioBuffer = ByteArrayOutputStream()
    private var recordingJob: Job? = null
    private var timerJob: Job? = null
    private var hasAutoStarted = false
    private var existingExpenses: List<Expense> = emptyList()

    // Starts listening once when the screen first opens (not again after a configuration change)
    fun onScreenOpened(hasMicPermission: Boolean) {
        if (hasAutoStarted || !hasMicPermission) return
        hasAutoStarted = true
        startRecording()
    }

    fun startRecording() {
        hasAutoStarted = true
        audioBuffer.reset()
        _uiState.value = VoiceUiState(isRecording = true)

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
            var seconds = 0
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

    /** Remembers the entries Gemini can match updates against, for an automatic stop. */
    fun setExistingExpenses(expenses: List<Expense>) {
        existingExpenses = expenses
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
                val text = result.getOrNull().orEmpty()
                val parsed = voiceRepository.parseTransactions(text, existingExpenses)
                _uiState.update {
                    it.copy(isProcessing = false, transcript = text, parsedResults = parsed)
                }
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

    /** Stops listening and throws away what was recorded. */
    fun cancelRecording() {
        recordingJob?.cancel()
        timerJob?.cancel()
        audioRecorder.stopRecording()
        audioBuffer.reset()
        _amplitude.value = 0f
        _uiState.value = VoiceUiState()
    }

    /** Drops one parsed entry the user doesn't want to keep. */
    fun removeResult(index: Int) {
        _uiState.update { state ->
            state.copy(parsedResults = state.parsedResults.filterIndexed { i, _ -> i != index })
        }
    }

    override fun onCleared() {
        recordingJob?.cancel()
        timerJob?.cancel()
        audioRecorder.stopRecording()
        super.onCleared()
    }

    /**
     * RMS of a 16-bit little-endian PCM chunk, mapped onto 0..1 with a gentle curve so
     * ordinary speech moves the orb visibly without shouting.
     */
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
        // Speech typically sits around 0.02–0.2 RMS; stretch that range out.
        return (sqrt(rms) * 1.8).toFloat().coerceIn(0f, 1f)
    }
}
