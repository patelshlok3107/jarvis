package com.shlok.jarvis.data

enum class JarvisStatus(
    val displayName: String,
    val subtitle: String,
    val spokenAck: String
) {
    AVAILABLE("AVAILABLE", "Calls ring normally", "You're available, Shlok. I'll let calls through."),
    BUSY("BUSY", "I'm handling your calls", "Understood, Shlok. I'll handle your incoming calls."),
    DND("DO NOT DISTURB", "Screening all calls", "Do not disturb enabled. I'll screen every call."),
    DRIVING("DRIVING", "Driving — hands-free responses", "Driving mode on. I'll let callers know you're on the road."),
    SLEEPING("SLEEPING", "Sleeping — quiet handling", "Sleep mode on. I'll keep things quiet."),
    MEETING("MEETING", "In a meeting", "Meeting mode enabled. I'll tell callers you're in a meeting.");

    companion object {
        fun fromString(raw: String): JarvisStatus =
            entries.find { it.name.equals(raw, true) } ?: AVAILABLE
    }
}

enum class ContactRuleAction { ALWAYS_ALLOW, JARVIS_HANDLES, BLOCK_SCREEN }
enum class UnknownCallerAction { JARVIS_HANDLES, ALLOW, BLOCK }

data class ContactRule(
    val id: String,
    val displayName: String,
    val phoneNumber: String? = null, // null = wildcard for unknown/spam
    val action: ContactRuleAction
)

data class CallHistoryEntry(
    val id: String,
    val callerName: String?,
    val callerNumber: String,
    val timestampMillis: Long,
    val status: JarvisStatus, // status at time of call
    val disposition: CallDisposition,
    val jarvisResponse: String?,
    val transcript: String?,
    val isSimulated: Boolean = false // true if OS blocked real answering
)

enum class CallDisposition { ALLOWED, HANDLED_BY_JARVIS, SCREENED, BLOCKED, MISSED }

/**
 * Configurable templates. User edits these in Settings.
 */
data class ResponseTemplates(
    val busy: String = "Hello. I am JARVIS, Shlok's personal AI assistant. Shlok is currently busy and cannot take your call. Please leave a message after the tone.",
    val dnd: String = "Hello. I am JARVIS, Shlok's personal AI assistant. Shlok is not available right now. Please leave a message and he will get back to you.",
    val driving: String = "Hello. I am JARVIS. Shlok is driving right now and cannot take your call. Please leave a message.",
    val sleeping: String = "Hello. I am JARVIS. Shlok is currently resting. Please leave a message and he'll respond when he's available.",
    val meeting: String = "Hello. I am JARVIS. Shlok is in a meeting. Please leave a message after the tone.",
    val followUp: String = "Would you like to leave a message?"
) {
    fun forStatus(s: JarvisStatus): String = when (s) {
        JarvisStatus.BUSY -> busy
        JarvisStatus.DND -> dnd
        JarvisStatus.DRIVING -> driving
        JarvisStatus.SLEEPING -> sleeping
        JarvisStatus.MEETING -> meeting
        JarvisStatus.AVAILABLE -> "" // never used
    }
}
