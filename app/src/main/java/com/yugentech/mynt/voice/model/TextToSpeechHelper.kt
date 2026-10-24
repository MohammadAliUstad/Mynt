// TextToSpeechHelper.kt
package com.yugentech.mynt.voice.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import java.util.UUID

class TextToSpeechHelper(context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var onDoneCallback: (() -> Unit)? = null

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.getDefault()
            isInitialized = true
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}

                override fun onDone(utteranceId: String?) {
                    onDoneCallback?.invoke()
                    onDoneCallback = null
                }

                override fun onError(utteranceId: String?) {
                    onDoneCallback?.invoke()
                    onDoneCallback = null
                }
            })
        }
    }

    fun speak(text: String, onDone: () -> Unit) {
        if (isInitialized) {
            onDoneCallback = onDone
            val utteranceId = UUID.randomUUID().toString()
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        } else {
            onDone()
        }
    }

    fun stop() {
        tts?.stop()
        onDoneCallback = null
    }

    fun destroy() {
        tts?.stop()
        tts?.shutdown()
    }
}