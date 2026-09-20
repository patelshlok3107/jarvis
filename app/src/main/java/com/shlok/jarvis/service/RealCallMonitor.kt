package com.shlok.jarvis.service

import android.content.Context
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Observes REAL telephony state on the physical device.
 * No simulation — only fires when Android reports a real cellular call.
 * Used by HomeScreen to show "REAL INCOMING CALL - JARVIS HANDLING".
 */
object RealCallMonitor {
    private val _state = MutableStateFlow(CallState.IDLE)
    val state: StateFlow<CallState> = _state

    enum class CallState { IDLE, RINGING, OFFHOOK, HANDLED_BY_JARVIS }

    private var listener: PhoneStateListener? = null

    fun start(ctx: Context) {
        try {
            val tm = ctx.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            listener = object : PhoneStateListener() {
                @Deprecated("deprecated")
                override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                    when (state) {
                        TelephonyManager.CALL_STATE_RINGING -> _state.value = CallState.RINGING
                        TelephonyManager.CALL_STATE_OFFHOOK -> _state.value = CallState.OFFHOOK
                        TelephonyManager.CALL_STATE_IDLE -> _state.value = CallState.IDLE
                    }
                }
            }
            @Suppress("DEPRECATION")
            tm.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
        } catch (_: SecurityException) {
            // Permission not yet granted — PhoneConnectionScreen will prompt
        } catch (_: Exception) {}
    }

    fun stop(ctx: Context) {
        try {
            val tm = ctx.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            listener?.let { tm.listen(it, PhoneStateListener.LISTEN_NONE) }
        } catch (_: Exception) {}
    }

    fun markHandled() { _state.value = CallState.HANDLED_BY_JARVIS }
}
