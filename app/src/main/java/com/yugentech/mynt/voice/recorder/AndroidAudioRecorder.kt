package com.yugentech.mynt.voice.recorder

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Android AudioRecorder copied directly from Amatyaa's STT engine.
 * Records 16kHz MONO PCM 16-bit audio via MediaRecorder.AudioSource.VOICE_RECOGNITION.
 */
class AndroidAudioRecorder(
    private val context: Context
) {

    private var audioRecord: AudioRecord? = null
    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    @SuppressLint("MissingPermission")
    fun startRecording(): Flow<ByteArray> = callbackFlow {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            close(SecurityException("Microphone permission not granted."))
            return@callbackFlow
        }

        val minBufSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        val bufferSize = (minBufSize * 2).coerceAtLeast(3200)

        val recorder = try {
            AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )
        } catch (e: Exception) {
            close(e)
            return@callbackFlow
        }

        audioRecord = recorder

        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            close(IllegalStateException("AudioRecord initialization failed."))
            return@callbackFlow
        }

        try {
            recorder.startRecording()
        } catch (e: Exception) {
            close(e)
            return@callbackFlow
        }

        val job = launch(Dispatchers.IO) {
            val audioBuffer = ByteArray(bufferSize / 2)
            while (isActive && recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                val read = recorder.read(audioBuffer, 0, audioBuffer.size)
                if (read > 0) {
                    val chunk = audioBuffer.copyOf(read)
                    trySend(chunk)
                }
            }
        }

        awaitClose {
            job.cancel()
            stopRecording()
        }
    }

    fun stopRecording() {
        audioRecord?.let {
            try {
                if (it.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    it.stop()
                }
            } catch (e: Exception) {
                // Ignore stop error
            } finally {
                it.release()
            }
        }
        audioRecord = null
    }
}
