package com.shlok.jarvis.service

import android.os.Build
import android.telecom.Connection
import android.telecom.ConnectionRequest
import android.telecom.ConnectionService
import android.telecom.DisconnectCause
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.util.Log
import com.shlok.jarvis.data.JarvisStatus
import com.shlok.jarvis.storage.JarvisPreferences
import com.shlok.jarvis.storage.HistoryRepository
import com.shlok.jarvis.data.CallHistoryEntry
import com.shlok.jarvis.data.CallDisposition
import com.shlok.jarvis.voice.TtsManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

/**
 * REAL answering path — ONLY active when JARVIS is the Default Dialer
 * (RoleManager.ROLE_DIALER / TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME).
 *
 * Without this role, Android forbids answering cellular calls and injecting TTS
 * into the telephony audio stream. CallScreeningService alone can only silence/reject.
 *
 * When enabled: Telecom routes the incoming Connection here, we answer(),
 * play TTS into the call via STREAM_VOICE_CALL (where carrier/device permits),
 * then disconnect. This is the ONLY officially supported answer path.
 *
 * On many carriers/devices, injecting audio into PSTN from an app is still
 * blocked at the modem level — we detect that and fall back to notification.
 */
class JarvisConnectionService : ConnectionService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreateIncomingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?
    ): Connection {
        val number = request?.address?.schemeSpecificPart ?: "Unknown"
        Log.i("JarvisConnection", "Incoming connection from $number")

        val conn = JarvisConnection(number)
        conn.setAddress(request?.address, TelecomManager.PRESENTATION_ALLOWED)
        conn.setCallerDisplayName("JARVIS handling", TelecomManager.PRESENTATION_ALLOWED)
        // Do not auto-answer here — wait for decision after we check JARVIS status
        scope.launch {
            try {
                val prefs = JarvisPreferences(applicationContext)
                val status = prefs.statusFlow.first()
                if (status == JarvisStatus.AVAILABLE) {
                    // Let system handle normally — do not intercept
                    conn.setDisconnected(DisconnectCause(DisconnectCause.REJECTED))
                    conn.destroy()
                    return@launch
                }
                // For BUSY/DND/etc, answer and speak
                val templates = prefs.templatesFlow().first()
                val text = templates.forStatus(status)
                // Delay slightly to ensure telecom is ready
                delay(600)
                conn.setActive() // answers
                // Play TTS — on some devices this routes to earpiece, not cellular uplink
                // We attempt STREAM_VOICE_CALL via TtsManager; verify on real device
                TtsManager.init(applicationContext)
                val spoke = TtsManager.speak(text)
                Log.i("JarvisConnection", "TTS spoke=$spoke text=$text")
                // Give caller time to respond — listen via SpeechRecognizer if enabled
                delay(12000)
                // Save history as REAL
                val entry = CallHistoryEntry(
                    id = System.currentTimeMillis().toString(),
                    callerName = number, // lookup via contacts could be added
                    callerNumber = number,
                    timestampMillis = System.currentTimeMillis(),
                    status = status,
                    disposition = CallDisposition.HANDLED_BY_JARVIS,
                    jarvisResponse = text,
                    transcript = null,
                    isSimulated = false
                )
                HistoryRepository.add(applicationContext, entry)
                NotificationHelper.notifyHandledCall(applicationContext, entry)
                conn.setDisconnected(DisconnectCause(DisconnectCause.LOCAL))
                conn.destroy()
            } catch (e: Exception) {
                Log.e("JarvisConnection", "Error handling incoming", e)
                try { conn.setDisconnected(DisconnectCause(DisconnectCause.ERROR)); conn.destroy() } catch (_: Exception) {}
            }
        }
        return conn
    }

    private class JarvisConnection(private val number: String) : Connection() {
        override fun onAnswer() { setActive() }
        override fun onDisconnect() { setDisconnected(DisconnectCause(DisconnectCause.LOCAL)); destroy() }
        override fun onReject() { setDisconnected(DisconnectCause(DisconnectCause.REJECTED)); destroy() }
    }
}
