package com.example.service

import android.content.Context
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import com.example.data.api.GeminiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

class TtsAudioHelper(private val context: Context) {
    private var nativeTts: TextToSpeech? = null
    private var isTtsInitialized = false
    private var mediaPlayer: MediaPlayer? = null

    init {
        nativeTts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                nativeTts?.language = Locale.US
                isTtsInitialized = true
            }
        }
    }

    fun speakText(
        scope: CoroutineScope,
        text: String,
        onStart: () -> Unit = {},
        onDone: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        onStart()
        scope.launch {
            // First attempt: Gemini TTS model gemini-3.8-flash-tts
            val geminiResult = GeminiClient.generateSpeechAudio(text)
            if (geminiResult.isSuccess) {
                val audioBytes = geminiResult.getOrNull()
                if (audioBytes != null && audioBytes.isNotEmpty()) {
                    playAudioBytes(audioBytes, onDone, onError)
                    return@launch
                }
            }

            // Fallback: Android native TextToSpeech engine
            withContext(Dispatchers.Main) {
                if (isTtsInitialized && nativeTts != null) {
                    nativeTts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "TTS_${System.currentTimeMillis()}")
                    onDone()
                } else {
                    onError("Text to speech is not ready.")
                }
            }
        }
    }

    private suspend fun playAudioBytes(
        bytes: ByteArray,
        onDone: () -> Unit,
        onError: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            val tempFile = File.createTempFile("gemini_tts_", ".mp3", context.cacheDir)
            val fos = FileOutputStream(tempFile)
            fos.write(bytes)
            fos.close()

            withContext(Dispatchers.Main) {
                stopPlayback()
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(tempFile.absolutePath)
                    prepare()
                    setOnCompletionListener {
                        onDone()
                        tempFile.delete()
                    }
                    setOnErrorListener { _, _, _ ->
                        onError("Playback error")
                        tempFile.delete()
                        true
                    }
                    start()
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onError("Failed to play audio: ${e.message}")
            }
        }
    }

    fun stopPlayback() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            nativeTts?.stop()
        } catch (e: Exception) {
            // ignore
        }
    }

    fun destroy() {
        stopPlayback()
        nativeTts?.shutdown()
        nativeTts = null
    }
}
