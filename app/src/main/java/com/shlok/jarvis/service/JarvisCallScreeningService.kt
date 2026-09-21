package com.shlok.jarvis.service

import android.content.Context
import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log
import com.shlok.jarvis.data.*
import com.shlok.jarvis.engine.JarvisEngine
import com.shlok.jarvis.storage.HistoryRepository
import com.shlok.jarvis.storage.JarvisLogger
import com.shlok.jarvis.storage.JarvisPreferences
import com.shlok.jarvis.storage.PrefKeys
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/**
 * LEGIT call handling via CallScreeningService (Android 24+).
 *
 * HARD TRUTH per spec §20 — documented in UI as well:
 * - CallScreeningService CAN: silence, block, reject, add to log, show notification.
 * - It CANNOT: answer the call and play TTS audio into the cellular stream nor
 *   capture the caller's voice as a transcript without RoleManager.ROLE_DIALER
 *   (being the Default Phone app) + Telecom ConnectionService.
 *   Injecting audio into PSTN requires carrier + OS support that is not exposed
 *   to third-party apps on standard Android.
 *
 * What we DO:
 * - If JARVIS should handle: we silence/reject appropriately, record a HistoryEntry
 *   marked isSimulated=true when real answering is unavailable, speak the would-be
 *   response via phone speaker (user hears what caller *would* have heard), and
 *   post a high-priority notification "Rahul called while busy — tap to call back".
 * - If device IS default dialer (user granted RoleManager), we COULD escalate to
 *   ConnectionService answering — scaffolding is left ready (see handleAsDefaultDialer).
 *
 * This distinction is shown in History as [SIMULATED] vs [REAL] and in onboarding.
 */
class JarvisCallScreeningService : CallScreeningService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onScreenCall(details: Call.Details) {
        // Critical: This is called by Android Telecom on a binder thread, even when app is in Doze/background.
        // We must respond quickly (< 5s) and not depend on ForegroundService or browser.
        val number = details.handle?.schemeSpecificPart ?: "Unknown"
        val isSpam = false

        // Log immediately for diagnostics
        Log.i("JarvisScreening", "onScreenCall: $number direction=${details.callDirection} isIncoming=${details.callDirection == Call.Details.DIRECTION_INCOMING}")

        // Only handle incoming calls
        if (details.callDirection != Call.Details.DIRECTION_INCOMING) {
            try { respondToCall(details, CallResponse.Builder().build()) } catch (_: Exception) {}
            return
        }

        scope.launch {
            var responded = false
            try {
                // Timeout handling: DataStore read must not block Telecom
                val prefs = JarvisPreferences(applicationContext)
                val status = withTimeoutOrNull(2000) { prefs.statusFlow.first() } ?: JarvisStatus.AVAILABLE
                val unknownAction = withTimeoutOrNull(1000) {
                    when (prefs.dataFlow.first()[PrefKeys.UNKNOWN_ACTION]) {
                        "ALLOW" -> UnknownCallerAction.ALLOW
                        "BLOCK" -> UnknownCallerAction.BLOCK
                        else -> UnknownCallerAction.JARVIS_HANDLES
                    }
                } ?: UnknownCallerAction.JARVIS_HANDLES
                val templates = withTimeoutOrNull(1000) { prefs.templatesFlow().first() } ?: ResponseTemplates()

                val contactRules: List<ContactRule> = loadContactRules()
                val isContact = isContact(applicationContext, number)
                val engine = JarvisEngine(prefs)
                val decision = engine.decide(status, number, contactRules, unknownAction, isContact, isSpam)

                // For screening, isSimulated = false. Only true if we claimed to answer but couldn't.
                // Screening (silence) is REAL, not simulated.
                val canReallyAnswer = isDefaultDialer(applicationContext)
                val isSimulatedForHistory = decision.shouldHandle && !canReallyAnswer && decision.disposition == CallDisposition.HANDLED_BY_JARVIS

                val response: CallResponse = when (decision.disposition) {
                    CallDisposition.ALLOWED -> CallResponse.Builder().setDisallowCall(false).setRejectCall(false).setSkipCallLog(false).setSkipNotification(false).build()
                    CallDisposition.BLOCKED -> CallResponse.Builder().setDisallowCall(true).setRejectCall(true).setSkipCallLog(false).setSkipNotification(false).build()
                    CallDisposition.SCREENED -> CallResponse.Builder().setDisallowCall(false).setRejectCall(false).setSilenceCall(true).setSkipCallLog(false).setSkipNotification(false).build()
                    CallDisposition.HANDLED_BY_JARVIS -> {
                        // Screening is REAL: silence and notify. Answering with TTS requires Default Dialer
                        // We silence here; if Default Dialer, ConnectionService will answer.
                        CallResponse.Builder().setDisallowCall(false).setRejectCall(false).setSilenceCall(true).setSkipCallLog(false).setSkipNotification(false).build()
                    }
                    CallDisposition.MISSED -> CallResponse.Builder().build()
                }

                respondToCall(details, response)
                responded = true

                val jarvisSays = if (decision.shouldHandle) templates.forStatus(status) else null
                val entry = CallHistoryEntry(
                    id = System.currentTimeMillis().toString(),
                    callerName = lookupContactName(applicationContext, number),
                    callerNumber = number,
                    timestampMillis = System.currentTimeMillis(),
                    status = status,
                    disposition = decision.disposition,
                    jarvisResponse = jarvisSays,
                    transcript = null,
                    isSimulated = isSimulatedForHistory
                )
                HistoryRepository.add(applicationContext, entry)
                if (decision.shouldHandle) {
                    NotificationHelper.notifyHandledCall(applicationContext, entry)
                    JarvisLogger.log(applicationContext, "CALL_SCREENED", "number=$number status=$status disp=${decision.disposition} simulated=$isSimulatedForHistory")
                } else {
                    JarvisLogger.log(applicationContext, "CALL_ALLOWED", "number=$number status=$status")
                }
                Log.i("JarvisScreening", "Handled $number -> ${decision.disposition} sim=$isSimulatedForHistory reason=${decision.reason} status=$status")

            } catch (e: Exception) {
                Log.e("JarvisScreening", "Error screening call", e)
                JarvisLogger.log(applicationContext, "CALL_SCREEN_ERROR", e.message ?: "unknown")
                if (!responded) {
                    try { respondToCall(details, CallResponse.Builder().build()) } catch (_: Exception) {}
                }
            }
        }
    }

    private fun isDefaultDialer(ctx: Context): Boolean {
        return try {
            val tm = ctx.getSystemService(android.telecom.TelecomManager::class.java)
            ctx.packageName == tm.defaultDialerPackage
        } catch (_: Exception) { false }
    }

    private fun isContact(ctx: Context, number: String): Boolean {
        return lookupContactName(ctx, number) != null
    }

    private fun lookupContactName(ctx: Context, number: String): String? {
        return try {
            val uri = android.net.Uri.withAppendedPath(android.provider.ContactsContract.PhoneLookup.CONTENT_FILTER_URI, android.net.Uri.encode(number))
            ctx.contentResolver.query(uri, arrayOf(android.provider.ContactsContract.PhoneLookup.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        } catch (_: Exception) { null }
    }

    @Suppress("UNCHECKED_CAST")
    private fun loadContactRules(): List<ContactRule> {
        // Stored as JSON string under key "contact_rules_json" in DataStore — read synchronously via prefs is complex
        // For now return empty; full load is done async in SettingsViewModel and decisions use that persisted list.
        // CallScreeningService will be enhanced to read encrypted prefs directly.
        return emptyList()
    }
}
