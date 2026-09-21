package com.shlok.jarvis.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.shlok.jarvis.MainActivity
import com.shlok.jarvis.engine.SmartMode
import com.shlok.jarvis.engine.SmartModeEngine
import com.shlok.jarvis.engine.SmartModeMapper
import com.shlok.jarvis.engine.TimeParser
import com.shlok.jarvis.storage.JarvisLogger
import com.shlok.jarvis.storage.JarvisPreferences
import com.shlok.jarvis.voice.TtsManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Voice Assistant Service - independent from UI Activity
 * Handles wake word "Hey JARVIS" and natural language commands
 * Battery-efficient: uses tap-to-talk by default, continuous wake word only if user enables and device supports it
 * Clearly indicates mic active via notification
 */
class VoiceAssistantService : LifecycleService() {

    private var recognizer: SpeechRecognizer? = null
    private var isListening = false
    private var isWakeWordMode = false

    companion object {
        const val NOTIF_ID = 1002
        fun start(ctx: Context, wakeWord: Boolean = false) {
            val i = Intent(ctx, VoiceAssistantService::class.java).apply { putExtra("wakeWord", wakeWord) }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ctx.startForegroundService(i) else ctx.startService(i)
        }
        fun stop(ctx: Context) { ctx.stopService(Intent(ctx, VoiceAssistantService::class.java)) }
    }

    override fun onCreate() {
        super.onCreate()
        TtsManager.init(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        isWakeWordMode = intent?.getBooleanExtra("wakeWord", false) ?: false

        startForeground(NOTIF_ID, buildNotification(isListening, isWakeWordMode))

        if (isWakeWordMode) {
            // Continuous wake word is battery-intensive and restricted on some OEMs
            // We start listening with a timeout and restart
            startWakeWordListening()
        } else {
            // One-shot listening (tap-to-talk)
            startOneShotListening()
        }

        return START_STICKY
    }

    private fun buildNotification(listening: Boolean, wakeWord: Boolean): Notification {
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val title = when {
            listening && wakeWord -> "JARVIS — Listening for Hey JARVIS"
            listening -> "JARVIS — Listening"
            wakeWord -> "JARVIS — Wake word active"
            else -> "JARVIS — Voice ready"
        }
        val text = if (listening) "Microphone active" else "Tap notification to speak"
        return NotificationCompat.Builder(this, "jarvis_service")
            .setSmallIcon(android.R.drawable.presence_audio_online)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(wakeWord || listening)
            .setContentIntent(open)
            .build()
    }

    private fun startOneShotListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            stopSelf()
            return
        }
        lifecycleScope.launch {
            try {
                isListening = true
                startForeground(NOTIF_ID, buildNotification(true, false))
                val result = listenOnce()
                isListening = false
                if (result != null) {
                    handleUtterance(result)
                }
                updateNotification()
                delay(2000)
                stopSelf()
            } catch (e: Exception) {
                isListening = false
                updateNotification()
                stopSelf()
            }
        }
    }

    private fun startWakeWordListening() {
        lifecycleScope.launch {
            while (isWakeWordMode) {
                try {
                    isListening = true
                    startForeground(NOTIF_ID, buildNotification(true, true))
                    val result = listenOnce()
                    isListening = false
                    if (result != null && result.lowercase().contains("hey jarvis")) {
                        // Wake word detected
                        TtsManager.speak("Yes?")
                        // Now listen for command
                        isListening = true
                        startForeground(NOTIF_ID, buildNotification(true, true))
                        val command = listenOnce()
                        isListening = false
                        if (command != null) {
                            handleUtterance(command.removePrefix("hey jarvis").trim())
                        }
                    } else if (result != null) {
                        // No wake word, but got speech - treat as command if contains jarvis
                        if (result.lowercase().contains("jarvis")) {
                            handleUtterance(result)
                        }
                    }
                    updateNotification()
                    delay(500)
                } catch (e: Exception) {
                    isListening = false
                    delay(1000)
                }
            }
        }
    }

    private fun updateNotification() {
        try {
            val nm = getSystemService(NotificationManager::class.java)
            nm.notify(NOTIF_ID, buildNotification(isListening, isWakeWordMode))
        } catch (_: Exception) {}
    }

    private suspend fun listenOnce(): String? {
        return kotlinx.coroutines.suspendCancellableCoroutine { cont ->
            try {
                val r = SpeechRecognizer.createSpeechRecognizer(this)
                recognizer = r
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                }
                r.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: android.os.Bundle?) {}
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {}
                    override fun onError(error: Int) { if (cont.isActive) cont.resume(null, null) }
                    override fun onResults(results: android.os.Bundle?) {
                        val list = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (cont.isActive) cont.resume(list?.firstOrNull(), null)
                    }
                    override fun onPartialResults(partialResults: android.os.Bundle?) {}
                    override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
                })
                r.startListening(intent)
                cont.invokeOnCancellation { r.destroy() }
            } catch (e: Exception) {
                if (cont.isActive) cont.resume(null, null)
            }
        }
    }

    private suspend fun handleUtterance(utterance: String) {
        try {
            val lower = utterance.lowercase().trim().removePrefix("hey jarvis").removePrefix("jarvis").trim()
            val prefs = JarvisPreferences(this)
            val engine = SmartModeEngine(prefs)

            // Check for cancel/available
            if (lower.contains("available") || lower.contains("cancel") || lower.contains("disable")) {
                val mode = SmartMode.AVAILABLE
                engine.setMode(this, mode)
                TtsManager.speak("You're available, Shlok.")
                JarvisLogger.log(this, "VOICE_CMD", "available: $utterance")
                return
            }

            // Check for what mode / how long
            if (lower.contains("what mode") || lower.contains("how long")) {
                val current = engine.getCurrentMode(this)
                TtsManager.speak("You're in ${current.displayName} mode.")
                return
            }
            if (lower.contains("who called") || lower.contains("show messages")) {
                TtsManager.speak("Check your call history in the app.")
                return
            }

            val mode = SmartModeMapper.fromUtterance(lower)
            if (mode != null) {
                val (start, end) = TimeParser.parse(lower)
                if (start != null && start > System.currentTimeMillis() + 30_000) {
                    // Scheduled for future
                    com.shlok.jarvis.storage.ScheduledModeStore.add(this, com.shlok.jarvis.engine.ScheduledMode(
                        id = System.currentTimeMillis().toString(),
                        mode = mode,
                        startTime = start,
                        endTime = end ?: (start + 2*60*60*1000),
                        privacy = com.shlok.jarvis.engine.PrivacyLevel.MEDIUM
                    ))
                    com.shlok.jarvis.service.ModeScheduler.schedule(this, com.shlok.jarvis.engine.ScheduledMode(
                        id = System.currentTimeMillis().toString(),
                        mode = mode,
                        startTime = start,
                        endTime = end ?: (start + 2*60*60*1000)
                    ))
                    val fmt = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault()).format(java.util.Date(start))
                    TtsManager.speak("Got it. I'll switch to ${mode.displayName} at $fmt.")
                } else if (end != null && end > System.currentTimeMillis()) {
                    // For duration
                    engine.setMode(this, mode)
                    com.shlok.jarvis.storage.ScheduledModeStore.add(this, com.shlok.jarvis.engine.ScheduledMode(
                        id = System.currentTimeMillis().toString(),
                        mode = mode,
                        startTime = System.currentTimeMillis(),
                        endTime = end
                    ))
                    com.shlok.jarvis.service.ModeScheduler.schedule(this, com.shlok.jarvis.engine.ScheduledMode(
                        id = System.currentTimeMillis().toString(),
                        mode = mode,
                        startTime = System.currentTimeMillis(),
                        endTime = end
                    ))
                    val mins = ((end - System.currentTimeMillis()) / 60000).toInt()
                    TtsManager.speak("${mode.displayName} mode is active for the next ${if (mins >= 60) "${mins/60} hours" else "$mins minutes"}.")
                } else {
                    engine.setMode(this, mode)
                    val ack = when (mode) {
                        SmartMode.EXAM -> "Understood. I'll activate Exam Mode."
                        SmartMode.MEETING -> "Meeting mode on. I'll handle your calls."
                        SmartMode.DRIVING -> "Driving mode on."
                        SmartMode.SLEEP -> "Sleep mode on."
                        else -> mode.status.spokenAck
                    }
                    TtsManager.speak(ack)
                }
                JarvisLogger.log(this, "VOICE_CMD", "${mode.name}: $utterance")
            } else {
                TtsManager.speak("Sorry Shlok, I didn't catch that. Try: I'm busy or I'm in an exam.")
                JarvisLogger.log(this, "VOICE_UNKNOWN", utterance)
            }
        } catch (e: Exception) {
            android.util.Log.e("VoiceService", "handle error", e)
        }
    }

    override fun onDestroy() {
        try { recognizer?.destroy() } catch (_: Exception) {}
        super.onDestroy()
    }
}
