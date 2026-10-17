package com.yugentech.mynt.voice.state

import com.yugentech.mynt.voice.model.ParsedVoiceResult

data class VoiceUiState(
    val isRecording: Boolean = false,
    val isProcessing: Boolean = false,
    val transcript: String = "",
    val error: String? = null,
    val parsedResults: List<ParsedVoiceResult> = emptyList(),
    /** Seconds since recording started, for the timer under the orb. */
    val elapsedSeconds: Int = 0
) {
    /** Transcribed but nothing usable came out of it. */
    val heardNothingUseful: Boolean
        get() = !isRecording && !isProcessing && transcript.isNotBlank() && parsedResults.isEmpty()
}
