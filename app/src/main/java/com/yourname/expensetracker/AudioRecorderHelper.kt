package com.yourname.expensetracker

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

/**
 * Helper class to record microphone audio to an AAC/M4A temp file for Soniox Cloud AI.
 */
class AudioRecorderHelper(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var outputFile: File? = null
    var isRecording = false
        private set

    /**
     * Starts recording audio from the microphone.
     */
    fun startRecording(): Boolean {
        return try {
            cancelRecording() // Clean up any previous recording

            val tempFile = File.createTempFile("speech_soniox_", ".m4a", context.cacheDir)
            outputFile = tempFile

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(16000)
                setAudioEncodingBitRate(32000)
                setOutputFile(tempFile.absolutePath)
                prepare()
                start()
            }

            mediaRecorder = recorder
            isRecording = true
            true
        } catch (e: Exception) {
            e.printStackTrace()
            isRecording = false
            mediaRecorder?.release()
            mediaRecorder = null
            false
        }
    }

    /**
     * Stops recording and returns the recorded audio bytes.
     */
    fun stopRecording(): ByteArray? {
        if (!isRecording) return null
        return try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            isRecording = false

            val bytes = outputFile?.readBytes()
            outputFile?.delete()
            bytes
        } catch (e: Exception) {
            e.printStackTrace()
            mediaRecorder?.release()
            mediaRecorder = null
            isRecording = false
            outputFile?.delete()
            null
        }
    }

    /**
     * Cancels recording and deletes temporary audio file.
     */
    fun cancelRecording() {
        try {
            if (isRecording) {
                mediaRecorder?.stop()
            }
            mediaRecorder?.release()
            mediaRecorder = null
            isRecording = false
            outputFile?.delete()
        } catch (e: Exception) {
            mediaRecorder?.release()
            mediaRecorder = null
            isRecording = false
            outputFile?.delete()
        }
    }
}
