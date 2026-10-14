package com.yourname.expensetracker

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale

/**
 * Speech recognition lifecycle and state transitions.
 */
enum class SpeechState {
    IDLE,
    LISTENING,
    PROCESSING,
    SUCCESS,
    ERROR
}

/**
 * Helper class wrapping Android's SpeechRecognizer API for live speech-to-text.
 */
class SpeechRecognizerHelper(private val context: Context) {

    var speechState by mutableStateOf(SpeechState.IDLE)
        private set

    var spokenText by mutableStateOf("")
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    private var speechRecognizer: SpeechRecognizer? = null

    init {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        speechState = SpeechState.LISTENING
                        errorMessage = null
                    }

                    override fun onBeginningOfSpeech() {
                        speechState = SpeechState.LISTENING
                    }

                    override fun onRmsChanged(rmsdB: Float) {}

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        speechState = SpeechState.PROCESSING
                    }

                    override fun onError(error: Int) {
                        speechState = SpeechState.ERROR
                        errorMessage = getErrorMessage(error)
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            spokenText = matches[0]
                            speechState = SpeechState.SUCCESS
                        } else {
                            speechState = SpeechState.ERROR
                            errorMessage = "No speech detected. Please try again."
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            spokenText = matches[0]
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }
        } else {
            speechState = SpeechState.ERROR
            errorMessage = "Speech recognizer is unavailable on this device."
        }
    }

    /**
     * Starts listening for voice input.
     */
    fun startListening() {
        if (speechRecognizer == null) {
            speechState = SpeechState.ERROR
            errorMessage = "Speech recognizer is unavailable on this device."
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...")
        }

        spokenText = ""
        errorMessage = null
        speechState = SpeechState.LISTENING
        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            speechState = SpeechState.ERROR
            errorMessage = e.localizedMessage ?: "Failed to start speech recognition."
        }
    }

    /**
     * Stops listening for speech input.
     */
    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) {
            // Ignore stop errors
        }
    }

    /**
     * Resets the speech helper state back to IDLE.
     */
    fun reset() {
        spokenText = ""
        errorMessage = null
        speechState = SpeechState.IDLE
    }

    /**
     * Releases system SpeechRecognizer resources.
     */
    fun destroy() {
        try {
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            // Ignore cleanup errors
        }
        speechRecognizer = null
    }

    private fun getErrorMessage(errorCode: Int): String {
        return when (errorCode) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error. Please check your mic."
            SpeechRecognizer.ERROR_CLIENT -> "Speech recognizer client error."
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is required."
            SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network connection error. Please check your internet connection."
            SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized. Please speak clearly and try again."
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy. Please wait a moment and try again."
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input detected. Please tap mic to try again."
            else -> "Speech recognition error occurred ($errorCode). Please try again."
        }
    }
}
