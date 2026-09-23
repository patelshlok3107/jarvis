package com.shlok.jarvis.service

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.shlok.jarvis.manager.ModeManager
import com.shlok.jarvis.storage.JarvisLogger
import com.shlok.jarvis.storage.JarvisPreferences
import com.shlok.jarvis.storage.VoiceDiagnostics
import com.shlok.jarvis.voice.SpeechRecognizerWakeWordEngine
import com.shlok.jarvis.voice.TtsManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * JARVIS Voice Assistant Service — clean FGS lifecycle per Android 14 rules.
 *
 * Architecture:
 *  User opens JARVIS -> enables voice -> permission granted -> startForegroundService from eligible foreground state
 *  -> creates notification -> initializes mic -> wake-word engine
 *
 *  If Android rejects (SecurityException), we do NOT crash, not suppress, not retry loop.
 *  Instead show: "VOICE ASSISTANT — Android has restricted microphone background access. [ VIEW SETUP ]"
 *
 *  Independent from UI — failure here does NOT crash MainActivity.
 */
class VoiceAssistantService : LifecycleService() {

    private val wakeEngine = SpeechRecognizerWakeWordEngine()
    private var isListening = false
    private var isWakeWordMode = false

    companion object {
        const val NOTIF_ID = 1002

        /**
         * Starts voice service ONLY if mic permission granted and caller is in eligible foreground state.
         * Returns false if permission missing — caller should show setup.
         */
        fun tryStart(ctx: Context, wakeWord: Boolean = false): Boolean {
            if (ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
            return try {
                val i = Intent(ctx, VoiceAssistantService::class.java).apply { putExtra("wakeWord", wakeWord) }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ctx.startForegroundService(i) else ctx.startService(i)
                true
            } catch (e: SecurityException) {
                android.util.Log.e("VoiceService", "FGS SecurityException", e)
                false
            } catch (_: Exception) { false }
        }

        // Legacy start wrapper — still checks permission
        fun start(ctx: Context, wakeWord: Boolean = false) { tryStart(ctx, wakeWord) }

        fun stop(ctx: Context) { try { ctx.stopService(Intent(ctx, VoiceAssistantService::class.java)) } catch (_: Exception) {} }
    }

    override fun onCreate() {
        super.onCreate()
        try { TtsManager.init(this) } catch (_: Exception) {}
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        isWakeWordMode = intent?.getBooleanExtra("wakeWord", false) ?: false

        // Critical: Check permission BEFORE starting foreground — Android 14 throws SecurityException if mic FGS without permission
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            // Do NOT start foreground with microphone type — show offline notification instead
            try {
                startForeground(NOTIF_ID, JarvisNotificationManager.buildOfflineNotification(this))
                lifecycleScope.launch { try { VoiceDiagnostics.setState(this@VoiceAssistantService, "STOPPED: Permission denied") } catch (_: Exception) {} }
                JarvisLogger.logSync(this, "VOICE_BLOCKED", "RECORD_AUDIO denied")
            } catch (_: Exception) {}
            // Stop gracefully — don't crash
            lifecycleScope.launch { delay(1500); stopSelf() }
            return START_NOT_STICKY
        }

        // Check SpeechRecognizer availability
        if (!wakeEngine.isAvailable(this)) {
            try {
                startForeground(NOTIF_ID, JarvisNotificationManager.buildOfflineNotification(this))
                lifecycleScope.launch { try { VoiceDiagnostics.setState(this@VoiceAssistantService, "ERROR: SpeechRecognizer unavailable") } catch (_: Exception) {} }
            } catch (_: Exception) {}
            lifecycleScope.launch { delay(1500); stopSelf() }
            return START_NOT_STICKY
        }

        // Proper FGS start — must be called within ~5 sec of startForegroundService, with microphone type
        try {
            startForeground(NOTIF_ID, JarvisNotificationManager.buildVoiceNotification(this, false, isWakeWordMode))
            lifecycleScope.launch { try { VoiceDiagnostics.setState(this@VoiceAssistantService, "MICROPHONE_READY") } catch (_: Exception) {} }
            JarvisLogger.logSync(this, "VOICE_STARTED", "wakeWord=$isWakeWordMode")
        } catch (e: SecurityException) {
            android.util.Log.e("VoiceService", "startForeground SecurityException", e)
            try {
                // Fallback: show offline state without crashing
                lifecycleScope.launch { try { VoiceDiagnostics.setState(this@VoiceAssistantService, "ERROR: ${e.message}") } catch (_: Exception) {} }
                JarvisLogger.logSync(this, "VOICE_FGS_BLOCKED", e.message ?: "SecurityException")
                // Post notification via NM directly (workaround if FGS failed)
                val nm = getSystemService(NotificationManager::class.java)
                nm.notify(NOTIF_ID, JarvisNotificationManager.buildOfflineNotification(this))
            } catch (_: Exception) {}
            lifecycleScope.launch { delay(1500); stopSelf() }
            return START_NOT_STICKY
        } catch (e: Exception) {
            android.util.Log.e("VoiceService", "FGS error", e)
            lifecycleScope.launch { delay(1500); stopSelf() }
            return START_NOT_STICKY
        }

        if (isWakeWordMode) startWakeWordLoop() else startOneShot()

        return START_STICKY
    }

    private fun startOneShot() {
        lifecycleScope.launch {
            try {
                isListening = true
                updateNotification()
                val result = wakeEngine.listenForCommand(this@VoiceAssistantService)
                isListening = false
                if (result != null) handleUtterance(result)
                updateNotification()
                delay(1800)
                stopSelf()
            } catch (e: Exception) {
                isListening = false
                updateNotification()
                JarvisLogger.log(this@VoiceAssistantService, "VOICE_ONESHOT_ERROR", e.message ?: "unknown")
                stopSelf()
            }
        }
    }

    private fun startWakeWordLoop() {
        lifecycleScope.launch {
            var consecutiveErrors = 0
            while (isWakeWordMode) {
                try {
                    isListening = true
                    updateNotification()
                    val wake = wakeEngine.listenForWakeWord(this@VoiceAssistantService)
                    isListening = false
                    when (wake) {
                        is com.shlok.jarvis.voice.WakeResult.WakeDetected -> {
                            consecutiveErrors = 0
                            TtsManager.speak("Yes?")
                            isListening = true
                            updateNotification()
                            lifecycleScope.launch { try { VoiceDiagnostics.setState(this@VoiceAssistantService, "COMMAND_LISTENING") } catch (_: Exception) {} }
                            val cmd = wakeEngine.listenForCommand(this@VoiceAssistantService)
                            isListening = false
                            if (cmd != null) {
                                val handled = handleUtterance(cmd)
                                if (!handled) {
                                    // If mode changed, TTS already spoken; else fallback
                                    TtsManager.speak("I'm busy right now.")
                                }
                            } else {
                                TtsManager.speak("I'm busy right now.")
                            }
                        }
                        is com.shlok.jarvis.voice.WakeResult.Command -> {
                            consecutiveErrors = 0
                            // One-shot with wake word included: "Hey JARVIS, I'm busy"
                            TtsManager.speak("Yes?")
                            delay(300)
                            handleUtterance(wake.text)
                        }
                        else -> { /* no wake */ }
                    }
                    updateNotification()
                    consecutiveErrors = 0
                    delay(400)
                } catch (e: Exception) {
                    isListening = false
                    consecutiveErrors++
                    JarvisLogger.log(this@VoiceAssistantService, "WAKE_LOOP_ERROR", e.message ?: "unknown")
                    if (consecutiveErrors > 5) {
                        try { VoiceDiagnostics.setState(this@VoiceAssistantService, "ERROR: loop failed $consecutiveErrors") } catch (_: Exception) {}
                        delay(3000)
                        consecutiveErrors = 0
                    } else delay(1000)
                }
            }
        }
    }

    private fun updateNotification() {
        try {
            val nm = getSystemService(NotificationManager::class.java)
            nm.notify(NOTIF_ID, JarvisNotificationManager.buildVoiceNotification(this, isListening, isWakeWordMode))
        } catch (_: Exception) {}
        lifecycleScope.launch {
            try {
                val state = when {
                    isListening && isWakeWordMode -> "WAKE_DETECTING"
                    isListening -> "COMMAND_LISTENING"
                    isWakeWordMode -> "MICROPHONE_READY"
                    else -> "STOPPED"
                }
                VoiceDiagnostics.setState(this@VoiceAssistantService, state)
            } catch (_: Exception) {}
        }
    }

    private suspend fun handleUtterance(utterance: String): Boolean {
        return try {
            val prefs = JarvisPreferences(this)
            val modeManager = ModeManager(prefs)
            val result = modeManager.setModeByUtterance(this, utterance)
            if (result.isSuccess) {
                val ack = result.getOrNull()?.ack ?: "Done."
                TtsManager.speak(ack)
                JarvisLogger.log(this, "VOICE_CMD", "${result.getOrNull()?.mode?.name}: $utterance")
                true
            } else {
                // Check special queries
                val lower = utterance.lowercase().trim().removePrefix("hey jarvis").removePrefix("jarvis").trim()
                when {
                    lower.contains("what mode") || lower.contains("how long") -> {
                        val cur = modeManager.getCurrentMode(this)
                        TtsManager.speak("You're in ${cur.displayName} mode.")
                        true
                    }
                    lower.contains("who called") || lower.contains("show messages") || lower.contains("show calls") -> {
                        TtsManager.speak("Check your call history in the app.")
                        true
                    }
                    else -> {
                        TtsManager.speak("Sorry Shlok, I didn't catch that. Try: I'm busy or I'm in an exam.")
                        JarvisLogger.log(this, "VOICE_UNKNOWN", utterance)
                        false
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("VoiceService", "handle error", e)
            false
        }
    }

    override fun onDestroy() {
        // Fire-and-forget state update — don't block destroy
        try { lifecycleScope.launch { try { VoiceDiagnostics.setState(this@VoiceAssistantService, "STOPPED") } catch (_: Exception) {} } } catch (_: Exception) {}
        super.onDestroy()
    }
}
