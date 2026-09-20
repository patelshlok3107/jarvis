package com.shlok.jarvis.service

import android.telecom.Call
import android.telecom.InCallService
import android.util.Log
import com.shlok.jarvis.storage.HistoryRepository
import com.shlok.jarvis.data.CallHistoryEntry
import com.shlok.jarvis.data.CallDisposition
import com.shlok.jarvis.data.JarvisStatus
import com.shlok.jarvis.storage.JarvisPreferences
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

/**
 * InCallService — required companion to ConnectionService when JARVIS is Default Dialer.
 * Receives real Call objects from Telecom. Used to observe state, record duration,
 * and ensure we only intercept when JARVIS status != AVAILABLE.
 *
 * This is NOT a fake overlay — it is the official Telecom InCallService.
 * Without Default Dialer, this service is never invoked (OS ignores it).
 */
class JarvisInCallService : InCallService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        val number = call.details?.handle?.schemeSpecificPart ?: "Unknown"
        Log.i("JarvisInCall", "Real call added: $number state=${call.state} isIncoming=${call.details?.callDirection == Call.Details.DIRECTION_INCOMING}")

        scope.launch {
            try {
                val prefs = JarvisPreferences(applicationContext)
                val status = prefs.statusFlow.first()
                if (status == JarvisStatus.AVAILABLE) {
                    // Do nothing — let default UI ring
                    return@launch
                }
                // For non-AVAILABLE, we could show in-call UI or auto-manage
                // Actual answering is handled by JarvisConnectionService; this service monitors
                call.registerCallback(object : Call.Callback() {
                    override fun onStateChanged(c: Call, state: Int) {
                        Log.i("JarvisInCall", "Call $number -> state $state")
                        if (state == Call.STATE_DISCONNECTED) {
                            // Call ended — history already saved by ConnectionService or CallScreeningService
                        }
                    }
                })
            } catch (e: Exception) {
                Log.e("JarvisInCall", "Error", e)
            }
        }
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        Log.i("JarvisInCall", "Call removed")
    }
}
