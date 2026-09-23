package com.shlok.jarvis.engine

import android.content.Context
import com.shlok.jarvis.data.JarvisStatus
import com.shlok.jarvis.storage.JarvisPreferences
import kotlinx.coroutines.flow.first
import java.util.Calendar

enum class SmartMode(
    val displayName: String,
    val description: String,
    val priority: Int,
    val status: JarvisStatus,
    val callResponse: String
) {
    AVAILABLE("AVAILABLE", "Available", 0, JarvisStatus.AVAILABLE, "Shlok is available."),
    BUSY("BUSY", "Busy", 10, JarvisStatus.BUSY, "Hello, I'm JARVIS, Shlok's AI assistant. He's currently busy and can't take the call. Please leave a message."),
    DND("DND", "Do not disturb", 95, JarvisStatus.DND, "Hello, I'm JARVIS, Shlok's AI assistant. Shlok is not available at the moment. Please leave a message."),
    DRIVING("DRIVING", "Driving", 70, JarvisStatus.DRIVING, "Hello, I'm JARVIS, Shlok's AI assistant. He's currently driving and can't safely answer the call. Please leave a message."),
    SLEEPING("SLEEPING", "Sleeping", 60, JarvisStatus.SLEEPING, "Hello, I'm JARVIS, Shlok's AI assistant. He's currently resting and isn't available right now. Please leave a message."),
    RESTING("RESTING", "Resting", 50, JarvisStatus.SLEEPING, "Hello, I'm JARVIS, Shlok's AI assistant. He's currently resting and isn't available right now. Please leave a message."),
    STUDYING("STUDYING", "Studying", 55, JarvisStatus.BUSY, "Hello, I'm JARVIS, Shlok's AI assistant. Shlok is currently studying and is unavailable at the moment. Please leave a message."),
    CLASS("CLASS", "In class", 65, JarvisStatus.MEETING, "Hello, I'm JARVIS, Shlok's AI assistant. Shlok is currently in class and cannot take the call. Please leave a message."),
    MEETING("MEETING", "Meeting", 80, JarvisStatus.MEETING, "Hello, I'm JARVIS, Shlok's AI assistant. He's currently in a meeting and can't take the call. Please leave a message."),
    WORKING("WORKING", "Working", 45, JarvisStatus.BUSY, "Hello, I'm JARVIS, Shlok's AI assistant. Shlok is currently working and may not be able to answer. Please leave a message."),
    EXAM("EXAM", "Exam", 90, JarvisStatus.MEETING, "Hello, I'm JARVIS, Shlok's AI assistant. He's currently in an exam and can't take the call. Please leave a message."),
    GYM("GYM", "At gym", 40, JarvisStatus.BUSY, "Hello, I'm JARVIS, Shlok's AI assistant. Shlok is currently at the gym and can't take the call. Please leave a message."),
    CINEMA("CINEMA", "At cinema", 30, JarvisStatus.DND, "Hello, I'm JARVIS, Shlok's AI assistant. He's currently at the cinema and can't take the call. Please leave a message."),
    CUSTOM("CUSTOM", "Custom", 15, JarvisStatus.BUSY, "Hello, I'm JARVIS, Shlok's AI assistant. Shlok is currently unavailable. Please leave a message."),

    // Backward compat aliases — map old names to new canonical modes
    SLEEP("SLEEPING", "Sleeping", 60, JarvisStatus.SLEEPING, "Hello, I'm JARVIS, Shlok's AI assistant. He's currently resting and isn't available right now. Please leave a message."),
    REST("RESTING", "Resting", 50, JarvisStatus.SLEEPING, "Hello, I'm JARVIS, Shlok's AI assistant. He's currently resting and isn't available right now. Please leave a message."),
    STUDY("STUDYING", "Studying", 55, JarvisStatus.BUSY, "Hello, I'm JARVIS, Shlok's AI assistant. Shlok is currently studying and is unavailable at the moment. Please leave a message."),
    WORK("WORKING", "Working", 45, JarvisStatus.BUSY, "Hello, I'm JARVIS, Shlok's AI assistant. Shlok is currently working and may not be able to answer. Please leave a message."),
    FOCUS("FOCUS", "Focus", 75, JarvisStatus.DND, "Hello, I'm JARVIS, Shlok's AI assistant. Shlok is currently in focus mode and cannot take the call. Please leave a message.");

    companion object {
        fun fromString(name: String): SmartMode {
            val n = name.uppercase().trim()
            // Handle legacy aliases
            return when (n) {
                "SLEEP" -> SLEEPING
                "REST" -> RESTING
                "STUDY" -> STUDYING
                "WORK" -> WORKING
                "TRAVEL", "EATING", "FOCUS" -> BUSY
                else -> entries.find { it.name.equals(n, true) } ?: AVAILABLE
            }
        }
        fun fromStatus(status: JarvisStatus): SmartMode = entries.find { it.status == status && it.name in setOf("AVAILABLE","BUSY","DND","DRIVING","SLEEPING","MEETING") } ?: when(status){
            JarvisStatus.AVAILABLE -> AVAILABLE
            JarvisStatus.BUSY -> BUSY
            JarvisStatus.DND -> DND
            JarvisStatus.DRIVING -> DRIVING
            JarvisStatus.SLEEPING -> SLEEPING
            JarvisStatus.MEETING -> MEETING
            else -> AVAILABLE
        }
        // All spec modes
        fun specModes(): List<SmartMode> = listOf(AVAILABLE, BUSY, DND, DRIVING, SLEEPING, RESTING, STUDYING, CLASS, MEETING, WORKING, EXAM, GYM, CINEMA, CUSTOM)
    }
}

enum class PrivacyLevel { LOW, MEDIUM, HIGH }

data class ScheduledMode(
    val id: String,
    val mode: SmartMode,
    val startTime: Long,
    val endTime: Long,
    val privacy: PrivacyLevel = PrivacyLevel.MEDIUM,
    val createdAt: Long = System.currentTimeMillis()
)

object TimeParser {
    fun parse(utterance: String): Pair<Long?, Long?> {
        val lower = utterance.lowercase()
        var start: Long? = null
        var end: Long? = null
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()

        // in X minutes/hours
        Regex("""in (\d+)\s*(minute|min|hour|hr)s?""").find(lower)?.let {
            val n = it.groupValues[1].toIntOrNull() ?: 0
            val unit = it.groupValues[2]
            val millis = if (unit.startsWith("hour") || unit.startsWith("hr")) n * 60 * 60 * 1000L else n * 60 * 1000L
            start = now + millis
        }
        // for X hours/minutes
        Regex("""for (\d+)\s*(minute|min|hour|hr)s?""").find(lower)?.let {
            val n = it.groupValues[1].toIntOrNull() ?: 0
            val unit = it.groupValues[2]
            val millis = if (unit.startsWith("hour") || unit.startsWith("hr")) n * 60 * 60 * 1000L else n * 60 * 1000L
            if (start == null) start = now
            end = (start ?: now) + millis
        }
        // for the next X hours
        Regex("""for the next (\d+)\s*(hour|hr)s?""").find(lower)?.let {
            val n = it.groupValues[1].toIntOrNull() ?: 0
            if (start == null) start = now
            end = (start ?: now) + n * 60 * 60 * 1000L
        }
        // until X PM/AM
        Regex("""until (\d+)(?::(\d+))?\s*(am|pm)?""").find(lower)?.let {
            val hour = it.groupValues[1].toIntOrNull() ?: 0
            val minute = it.groupValues[2].toIntOrNull() ?: 0
            var h = hour
            val ampm = it.groupValues[3]
            if (ampm == "pm" && h < 12) h += 12
            if (ampm == "am" && h == 12) h = 0
            cal.set(Calendar.HOUR_OF_DAY, h)
            cal.set(Calendar.MINUTE, minute)
            cal.set(Calendar.SECOND, 0)
            if (cal.timeInMillis <= now) cal.add(Calendar.DAY_OF_YEAR, 1)
            end = cal.timeInMillis
            if (start == null) start = now
        }
        // at X (for scheduling)
        Regex("""at (\d+)(?::(\d+))?\s*(am|pm)?""").find(lower)?.let {
            if (start == null) {
                val hour = it.groupValues[1].toIntOrNull() ?: 0
                val minute = it.groupValues[2].toIntOrNull() ?: 0
                var h = hour
                val ampm = it.groupValues[3]
                if (ampm == "pm" && h < 12) h += 12
                if (ampm == "am" && h == 12) h = 0
                cal.set(Calendar.HOUR_OF_DAY, h)
                cal.set(Calendar.MINUTE, minute)
                cal.set(Calendar.SECOND, 0)
                if (cal.timeInMillis <= now) cal.add(Calendar.DAY_OF_YEAR, 1)
                start = cal.timeInMillis
            }
        }
        // tonight, tomorrow morning
        if (lower.contains("tonight") && end == null) {
            cal.set(Calendar.HOUR_OF_DAY, 22)
            cal.set(Calendar.MINUTE, 0)
            if (cal.timeInMillis <= now) cal.add(Calendar.DAY_OF_YEAR, 1)
            end = cal.timeInMillis
            if (start == null) start = now
        }
        if (lower.contains("tomorrow morning") && end == null) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
            cal.set(Calendar.HOUR_OF_DAY, 8)
            cal.set(Calendar.MINUTE, 0)
            end = cal.timeInMillis
            if (start == null) start = now
        }

        return Pair(start, end)
    }
}

object SmartModeMapper {
    fun fromUtterance(utterance: String): SmartMode? {
        val lower = utterance.lowercase()
        // Check custom first - e.g. "I'm at the gym" -> GYM, "I'm at the cinema" -> CINEMA
        // Then priority modes
        return when {
            lower.contains("exam") -> SmartMode.EXAM
            lower.contains("meeting") -> SmartMode.MEETING
            lower.contains("driving") || lower.contains("drive") -> SmartMode.DRIVING
            lower.contains("sleep") || lower.contains("sleeping") || lower.contains("going to sleep") -> SmartMode.SLEEPING
            lower.contains("resting") || (lower.contains("rest") && !lower.contains("restaurant")) -> SmartMode.RESTING
            lower.contains("study") || lower.contains("studying") -> SmartMode.STUDYING
            lower.contains("class") -> SmartMode.CLASS
            lower.contains("working") || (lower.contains("work") && !lower.contains("wake")) -> SmartMode.WORKING
            lower.contains("gym") -> SmartMode.GYM
            lower.contains("cinema") || lower.contains("movie") -> SmartMode.CINEMA
            lower.contains("dnd") || lower.contains("don't disturb") || lower.contains("do not disturb") -> SmartMode.DND
            lower.contains("busy") -> SmartMode.BUSY
            lower.contains("available") -> SmartMode.AVAILABLE
            // Custom fallback: e.g. "I'm at the gym" already handled, but "I'm at the cafe" -> CUSTOM with custom description
            lower.contains("at the") || lower.contains("at a") -> SmartMode.CUSTOM
            else -> null
        }
    }
    // For custom utterances like "I'm at the gym" we can extract location for dynamic response
    fun extractCustomLocation(utterance: String): String? {
        val lower = utterance.lowercase()
        Regex("""at (?:the )?([a-z\s]+?)(?:\.|,|$)""").find(lower)?.let { return it.groupValues[1].trim() }
        Regex("""in (?:a |an )?([a-z\s]+?)(?:\.|,|$)""").find(lower)?.let { return it.groupValues[1].trim() }
        return null
    }
}

object ResponseGenerator {
    // CallContext as per spec §16
    data class CallContext(
        val caller: String? = null,
        val mode: SmartMode,
        val time: Long = System.currentTimeMillis(),
        val customMessage: String? = null
    )
    fun generate(mode: SmartMode, privacy: PrivacyLevel, caller: String? = null): String {
        return generate(CallContext(caller, mode, customMessage = null), privacy)
    }
    fun generate(ctx: CallContext, privacy: PrivacyLevel): String {
        // If custom message set (e.g. "tell callers I'm studying"), use it
        ctx.customMessage?.let { return it }
        // If mode is CUSTOM with caller context, use generic unavailable
        if (ctx.mode == SmartMode.CUSTOM) {
            return "Hello, I'm JARVIS, Shlok's AI assistant. He's currently unavailable and can't take the call. Please leave a message."
        }
        val base = when (privacy) {
            PrivacyLevel.LOW -> ctx.mode.callResponse
            PrivacyLevel.MEDIUM -> when (ctx.mode) {
                SmartMode.EXAM -> "Hello, I'm JARVIS, Shlok's AI assistant. He's currently in an exam and can't take the call. Please leave a message."
                SmartMode.DRIVING -> "Hello, I'm JARVIS, Shlok's AI assistant. He's currently driving and can't safely answer the call. Please leave a message."
                SmartMode.SLEEPING, SmartMode.RESTING -> "Hello, I'm JARVIS, Shlok's AI assistant. He's currently resting and isn't available right now. Please leave a message."
                SmartMode.MEETING -> "Hello, I'm JARVIS, Shlok's AI assistant. He's currently in a meeting and can't take the call. Please leave a message."
                SmartMode.BUSY -> "Hello, I'm JARVIS, Shlok's AI assistant. He's currently busy and can't take the call. Please leave a message."
                SmartMode.CINEMA -> "Hello, I'm JARVIS, Shlok's AI assistant. He's currently at the cinema and can't take the call. Please leave a message."
                SmartMode.GYM -> "Hello, I'm JARVIS, Shlok's AI assistant. He's currently at the gym and can't take the call. Please leave a message."
                SmartMode.STUDYING -> "Hello, I'm JARVIS, Shlok's AI assistant. He's currently studying and can't take the call. Please leave a message."
                SmartMode.CLASS -> "Hello, I'm JARVIS, Shlok's AI assistant. He's currently in class and can't take the call. Please leave a message."
                SmartMode.WORKING -> "Hello, I'm JARVIS, Shlok's AI assistant. He's currently working and can't take the call. Please leave a message."
                SmartMode.DND -> "Hello, I'm JARVIS, Shlok's AI assistant. He's currently not available. Please leave a message."
                else -> "Hello, I'm JARVIS, Shlok's AI assistant. He's currently ${ctx.mode.description.lowercase()} and can't take the call. Please leave a message."
            }
            PrivacyLevel.HIGH -> "Hello, I'm JARVIS, Shlok's AI assistant. Shlok is currently unavailable and cannot take the call. Please leave a message."
        }
        return base
    }
}

class SmartModeEngine(private val prefs: JarvisPreferences) {
    suspend fun getCurrentMode(context: Context): SmartMode {
        return try {
            val status = prefs.statusFlow.first()
            SmartMode.fromStatus(status)
        } catch (_: Exception) { SmartMode.AVAILABLE }
    }

    suspend fun setMode(context: Context, mode: SmartMode) {
        prefs.setStatus(mode.status)
        com.shlok.jarvis.storage.JarvisLogger.log(context, "MODE_SET", mode.name)
    }

    suspend fun scheduleMode(context: Context, mode: SmartMode, start: Long, end: Long?) {
        // Save to DataStore as JSON
        val scheduled = ScheduledMode(
            id = System.currentTimeMillis().toString(),
            mode = mode,
            startTime = start,
            endTime = end ?: (start + 2 * 60 * 60 * 1000), // default 2 hours
            privacy = PrivacyLevel.MEDIUM
        )
        // Save and schedule via AlarmManager
        com.shlok.jarvis.storage.ScheduledModeStore.add(context, scheduled)
        com.shlok.jarvis.service.ModeScheduler.schedule(context, scheduled)
        com.shlok.jarvis.storage.JarvisLogger.log(context, "MODE_SCHEDULED", "${mode.name} start=${start} end=${scheduled.endTime}")
    }

    fun getResponseForCall(context: Context, caller: String?, mode: SmartMode, privacy: PrivacyLevel): String {
        return ResponseGenerator.generate(mode, privacy, caller)
    }
}
