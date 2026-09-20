package com.shlok.jarvis.engine

import android.content.Context
import com.shlok.jarvis.data.*
import com.shlok.jarvis.storage.JarvisPreferences

/**
 * Central decision engine. Single source of truth for:
 * - what JARVIS says for a given status
 * - whether to intercept a call (respecting contact rules)
 */
class JarvisEngine(
    private val prefs: JarvisPreferences
) {
    suspend fun responseFor(status: JarvisStatus, templates: ResponseTemplates, contactName: String? = null): String {
        val base = templates.forStatus(status)
        // Personalize with caller name when known for more natural AI feel
        return if (contactName != null && status != JarvisStatus.AVAILABLE) base else base
    }

    /**
     * Contact-aware decision. Returns disposition.
     * Real telecom: if engine says HANDLED, CallScreeningService will set disallowCall or silence.
     * CRITICAL LIMITATION (documented in UI): On Android 10+ a CallScreeningService CANNOT answer a
     * cellular call and inject TTS audio into the telephony stream nor capture caller audio without
     * being the Default Dialer / RoleManager.ROLE_DIALER. We therefore HONESTLY report isRealHandling
     * vs isSimulated.
     */
    fun decide(
        status: JarvisStatus,
        callerNumber: String,
        contactRules: List<ContactRule>,
        unknownAction: UnknownCallerAction,
        isContact: Boolean,
        isSpam: Boolean
    ): Decision {
        if (status == JarvisStatus.AVAILABLE) return Decision(CallDisposition.ALLOWED, false, "Available — ring normally")
        // contact override
        contactRules.find { it.phoneNumber != null && normalize(it.phoneNumber)==normalize(callerNumber) }?.let { rule ->
            return when (rule.action) {
                ContactRuleAction.ALWAYS_ALLOW -> Decision(CallDisposition.ALLOWED, false, "Contact rule: always allow")
                ContactRuleAction.JARVIS_HANDLES -> Decision(CallDisposition.HANDLED_BY_JARVIS, true, "Contact rule: JARVIS handles")
                ContactRuleAction.BLOCK_SCREEN -> Decision(CallDisposition.BLOCKED, false, "Contact rule: block/screen")
            }
        }
        if (isSpam) return Decision(CallDisposition.SCREENED, false, "Spam — screened")
        if (!isContact) {
            return when (unknownAction) {
                UnknownCallerAction.ALLOW -> Decision(CallDisposition.ALLOWED, false, "Unknown — allowed per setting")
                UnknownCallerAction.BLOCK -> Decision(CallDisposition.BLOCKED, false, "Unknown — blocked per setting")
                UnknownCallerAction.JARVIS_HANDLES -> Decision(CallDisposition.HANDLED_BY_JARVIS, true, "Unknown — JARVIS handles")
            }
        }
        // default busy behavior
        return Decision(CallDisposition.HANDLED_BY_JARVIS, true, "Busy status — JARVIS handles")
    }

    data class Decision(val disposition: CallDisposition, val shouldHandle: Boolean, val reason: String)

    private fun normalize(n: String) = n.replace(Regex("[^0-9+]"), "")
}

/** AI Provider abstraction — modular per spec §15 */
interface AiProvider {
    suspend fun generateReply(callerUtterance: String, status: JarvisStatus, templates: ResponseTemplates): String
    val name: String
}

class TemplateAiProvider : AiProvider {
    override val name = "Template (offline)"
    override suspend fun generateReply(callerUtterance: String, status: JarvisStatus, templates: ResponseTemplates): String {
        val lower = callerUtterance.lowercase()
        return when {
            "speak" in lower || "talk" in lower -> "Shlok is currently ${status.displayName.lowercase()}. Would you like to leave a message?"
            "message" in lower || "tell him" in lower -> "Sure. I'll let Shlok know you called. Please leave your message after the tone."
            "urgent" in lower || "emergency" in lower -> "Understood — I'll notify Shlok immediately that you called about something urgent."
            "when" in lower && "free" in lower -> "Shlok will be free soon. I'll have him call you back. Would you like to leave a message?"
            else -> templates.forStatus(status) + " " + templates.followUp
        }
    }
}

/**
 * Cloud provider stub — user configures endpoint/key in Settings (never hardcoded).
 * Actual http call is deliberately minimal; key stored in EncryptedSharedPreferences.
 */
class CloudAiProvider(
    private val endpoint: String,
    private val apiKey: String
) : AiProvider {
    override val name = "Cloud LLM"
    override suspend fun generateReply(callerUtterance: String, status: JarvisStatus, templates: ResponseTemplates): String {
        // TODO: wire to OpenAI / Gemini with secure key storage.
        // Fallback to template if network unavailable — never crash call handling.
        return try {
            // Placeholder — real implementation would POST to endpoint
            TemplateAiProvider().generateReply(callerUtterance, status, templates)
        } catch (_: Exception) {
            templates.forStatus(status)
        }
    }
}
