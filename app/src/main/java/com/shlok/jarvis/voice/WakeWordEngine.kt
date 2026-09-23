package com.shlok.jarvis.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import com.shlok.jarvis.storage.JarvisLogger
import com.shlok.jarvis.storage.VoiceDiagnostics
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Wake Word Engine — local "Hey JARVIS" detection.
 * Uses SpeechRecognizer locally, checks string contains "hey jarvis" — no browser, no Chrome, no server.
 * Battery-aware: runs only when VoiceAssistantService is active and user enabled wake word.
 * This is independent component — failure must not crash UI.
 */
interface WakeWordEngine {
    suspend fun listenForWakeWord(ctx: Context): WakeResult?
    suspend fun listenForCommand(ctx: Context): String?
    fun isAvailable(ctx: Context): Boolean
}

sealed class WakeResult {
    object WakeDetected : WakeResult()
    data class Command(val text: String) : WakeResult()
    object NoWake : WakeResult()
}

class SpeechRecognizerWakeWordEngine : WakeWordEngine {
    override fun isAvailable(ctx: Context): Boolean {
        return try { SpeechRecognizer.isRecognitionAvailable(ctx) } catch (_: Exception) { false }
    }

    override suspend fun listenForWakeWord(ctx: Context): WakeResult? {
        val text = listenOnce(ctx, timeoutMillis = 5000) ?: return WakeResult.NoWake
        val lower = text.lowercase()
        return if (lower.contains("hey jarvis") || lower.contains("hey jarvis")) {
            // Also handle one-shot: "Hey JARVIS, I'm busy"
            val withoutWake = lower.removePrefix("hey jarvis").removePrefix("hey jarvis").trim()
            if (withoutWake.isNotEmpty() && withoutWake.length > 3) {
                WakeResult.Command(withoutWake)
            } else {
                WakeResult.WakeDetected
            }
        } else if (lower.contains("jarvis")) {
            // Treat any jarvis mention as wake
            WakeResult.Command(lower.removePrefix("jarvis").trim())
        } else {
            WakeResult.NoWake
        }
    }

    override suspend fun listenForCommand(ctx: Context): String? {
        return listenOnce(ctx, timeoutMillis = 7000)
    }

    private suspend fun listenOnce(ctx: Context, timeoutMillis: Long = 5000): String? {
        // Check permission first — must not throw SecurityException
        if (ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return null
        }
        if (!isAvailable(ctx)) return null
        return suspendCancellableCoroutine { cont ->
            var recognizer: SpeechRecognizer? = null
            try {
                recognizer = SpeechRecognizer.createSpeechRecognizer(ctx)
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1200L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 700L)
                }
                recognizer.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        try { GlobalScope.launch { try { VoiceDiagnostics.setState(ctx, "MICROPHONE_READY") } catch (_: Exception) {} } } catch (_: Exception) {}
                    }
                    override fun onBeginningOfSpeech() {
                        try { GlobalScope.launch { try { VoiceDiagnostics.setLastAudio(ctx, System.currentTimeMillis()); VoiceDiagnostics.setState(ctx, "AUDIO_STREAM_STARTED") } catch (_: Exception) {} } } catch (_: Exception) {}
                    }
                    override fun onRmsChanged(rmsdB: Float) {
                        val level = (rmsdB + 2f).coerceIn(0f, 10f).toInt().toString()
                        try { GlobalScope.launch { try { VoiceDiagnostics.setMicLevel(ctx, level) } catch (_: Exception) {} } } catch (_: Exception) {}
                    }
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {}
                    override fun onError(error: Int) {
                        val msg = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "Audio error"
                            SpeechRecognizer.ERROR_CLIENT -> "Client error"
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permission error"
                            SpeechRecognizer.ERROR_NETWORK -> "Network error"
                            SpeechRecognizer.ERROR_NO_MATCH -> "No match"
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Busy"
                            SpeechRecognizer.ERROR_SERVER -> "Server error"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Timeout"
                            else -> "Error $error"
                        }
                        try { GlobalScope.launch { try { VoiceDiagnostics.setState(ctx, "ERROR: $msg") } catch (_: Exception) {} } } catch (_: Exception) {}
                        if (cont.isActive) cont.resume(null, null)
                        try { recognizer?.destroy() } catch (_: Exception) {}
                    }
                    override fun onResults(results: Bundle?) {
                        val list = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = list?.firstOrNull()
                        if (text != null) try { GlobalScope.launch { try { VoiceDiagnostics.setLastAudio(ctx, System.currentTimeMillis()) } catch (_: Exception) {} } } catch (_: Exception) {}
                        if (cont.isActive) cont.resume(text, null)
                        try { recognizer?.destroy() } catch (_: Exception) {}
                    }
                    override fun onPartialResults(partialResults: Bundle?) {}
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
                recognizer.startListening(intent)
                try { GlobalScope.launch { try { VoiceDiagnostics.setState(ctx, "WAKE_DETECTING") } catch (_: Exception) {} } } catch (_: Exception) {}
                cont.invokeOnCancellation { try { recognizer?.destroy() } catch (_: Exception) {} }
                // Timeout handling is done by caller via withTimeoutOrNull; we just wait for result
            } catch (e: Exception) {
                try { GlobalScope.launch { try { VoiceDiagnostics.setState(ctx, "ERROR: ${e.message}") } catch (_: Exception) {} } } catch (_: Exception) {}
                if (cont.isActive) cont.resume(null, null)
                try { recognizer?.destroy() } catch (_: Exception) {}
            }
        }
    }
}
