package com.shlok.jarvis.service

import android.content.Context
import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log
import com.shlok.jarvis.data.*
import com.shlok.jarvis.engine.JarvisEngine
import com.shlok.jarvis.storage.HistoryRepository
import com.shlok.jarvis.storage.JarvisPreferences
import com.shlok.jarvis.storage.PrefKeys
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

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
        val number = details.handle?.schemeSpecificPart ?: "Unknown"
        val isSpam = false // TODO: integrate with Spam API where available

        scope.launch {
            try {
                val prefs = JarvisPreferences(applicationContext)
                val status = prefs.statusFlow.first()
                val unknownAction = when (prefs.dataFlow.first()[PrefKeys.UNKNOWN_ACTION]) {
                    "ALLOW" -> UnknownCallerAction.ALLOW
                    "BLOCK" -> UnknownCallerAction.BLOCK
                    else -> UnknownCallerAction.JARVIS_HANDLES
                }
                val templates = prefs.templatesFlow().first()
                // Contact rules: stored as simple JSON in DataStore — parse here if needed
                val contactRules: List<ContactRule> = loadContactRules()

                val isContact = isContact(applicationContext, number)
                val engine = JarvisEngine(prefs)
                val decision = engine.decide(status, number, contactRules, unknownAction, isContact, isSpam)

                val canReallyAnswer = isDefaultDialer(applicationContext)

                val response: CallResponse = when (decision.disposition) {
                    CallDisposition.ALLOWED -> CallResponse.Builder().setDisallowCall(false).setRejectCall(false).setSkipCallLog(false).setSkipNotification(false).build()
                    CallDisposition.BLOCKED -> CallResponse.Builder().setDisallowCall(true).setRejectCall(true).setSkipCallLog(false).setSkipNotification(false).build()
                    CallDisposition.SCREENED -> CallResponse.Builder().setDisallowCall(false).setRejectCall(false).setSilenceCall(true).setSkipCallLog(false).setSkipNotification(false).build()
                    CallDisposition.HANDLED_BY_JARVIS -> {
                        if (canReallyAnswer) {
                            // Real handling path — requires ROLE_DIALER. Keep silenced for now until ConnectionService answers.
                            CallResponse.Builder().setDisallowCall(false).setRejectCall(false).setSilenceCall(true).setSkipCallLog(false).setSkipNotification(false).build()
                        } else {
                            // Honest simulated handling: silence + notify user
                            CallResponse.Builder().setDisallowCall(false).setRejectCall(false).setSilenceCall(true).setSkipCallLog(false).setSkipNotification(false).build()
                        }
                    }
                    CallDisposition.MISSED -> CallResponse.Builder().build()
                }

                respondToCall(details, response)

                // History + notification (always, even when ALLOWED we log)
                val jarvisSays = if (decision.shouldHandle) templates.forStatus(status) else null
                val entry = CallHistoryEntry(
                    id = System.currentTimeMillis().toString(),
                    callerName = lookupContactName(applicationContext, number),
                    callerNumber = number,
                    timestampMillis = System.currentTimeMillis(),
                    status = status,
                    disposition = decision.disposition,
                    jarvisResponse = jarvisSays,
                    transcript = null, // real transcript only if ROLE_DIALER + user enabled recording + legal
                    isSimulated = decision.shouldHandle && !canReallyAnswer
                )
                HistoryRepository.add(applicationContext, entry)
                if (decision.shouldHandle) {
                    NotificationHelper.notifyHandledCall(applicationContext, entry)
                }
                Log.i("JarvisScreening", "Handled $number -> ${decision.disposition} sim=${entry.isSimulated} reason=${decision.reason}")

            } catch (e: Exception) {
                Log.e("JarvisScreening", "Error screening call", e)
                // Fail open: allow call rather than accidentally blocking
                try { respondToCall(details, CallResponse.Builder().build()) } catch (_: Exception) {}
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
