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
    BUSY("BUSY", "Busy", 10, JarvisStatus.BUSY, "Hello, I'm JARVIS, Shlok's personal assistant. Shlok is currently busy and unable to take the call. Would you like to leave a message?"),
    EXAM("EXAM", "Exam", 90, JarvisStatus.MEETING, "Hello, I'm JARVIS, Shlok's personal assistant. Shlok is currently in an exam and cannot take the call. Please leave a message and I'll make sure he receives it."),
    MEETING("MEETING", "Meeting", 80, JarvisStatus.MEETING, "Hello, I'm JARVIS, Shlok's personal assistant. Shlok is currently in a meeting and cannot take the call. Please leave a message."),
    DRIVING("DRIVING", "Driving", 70, JarvisStatus.DRIVING, "Hello, I'm JARVIS, Shlok's personal assistant. Shlok is currently driving and cannot safely answer the phone. Please leave a message."),
    SLEEP("SLEEP", "Sleeping", 60, JarvisStatus.SLEEPING, "Hello, I'm JARVIS, Shlok's personal assistant. Shlok is currently resting and may not be available. Please leave a message."),
    REST("REST", "Resting", 50, JarvisStatus.SLEEPING, "Hello, I'm JARVIS, Shlok's personal assistant. Shlok is currently resting and may not be available. Please leave a message."),
    STUDY("STUDY", "Studying", 55, JarvisStatus.BUSY, "Hello, I'm JARVIS, Shlok's personal assistant. Shlok is currently studying and is unavailable at the moment. Please leave a message."),
    CLASS("CLASS", "In class", 65, JarvisStatus.MEETING, "Hello, I'm JARVIS, Shlok's personal assistant. Shlok is currently in class and cannot take the call. Please leave a message."),
    WORK("WORK", "Working", 45, JarvisStatus.BUSY, "Hello, I'm JARVIS, Shlok's personal assistant. Shlok is currently working and may not be able to answer. Please leave a message."),
    GYM("GYM", "At gym", 40, JarvisStatus.BUSY, "Hello, I'm JARVIS, Shlok's personal assistant. Shlok is currently working out and may not be able to answer. Please leave a message."),
    DND("DND", "Do not disturb", 95, JarvisStatus.DND, "Hello, I'm JARVIS, Shlok's personal assistant. Shlok is not available at the moment. Please leave a message."),
    TRAVEL("TRAVEL", "Traveling", 35, JarvisStatus.BUSY, "Hello, I'm JARVIS, Shlok's personal assistant. Shlok is currently traveling and may not be available. Please leave a message."),
    EATING("EATING", "Eating", 20, JarvisStatus.BUSY, "Hello, I'm JARVIS, Shlok's personal assistant. Shlok is currently unavailable. Please leave a message."),
    CINEMA("CINEMA", "At cinema", 30, JarvisStatus.DND, "Hello, I'm JARVIS, Shlok's personal assistant. Shlok is currently unavailable. Please leave a message."),
    FOCUS("FOCUS", "Focus mode", 75, JarvisStatus.DND, "Hello, I'm JARVIS, Shlok's personal assistant. Shlok is currently in focus mode and cannot take the call. Please leave a message."),
    CUSTOM("CUSTOM", "Custom", 15, JarvisStatus.BUSY, "Hello, I'm JARVIS, Shlok's personal assistant. Shlok is currently unavailable. Please leave a message.");

    companion object {
        fun fromString(name: String): SmartMode = entries.find { it.name.equals(name, ignoreCase = true) } ?: AVAILABLE
        fun fromStatus(status: JarvisStatus): SmartMode = entries.find { it.status == status } ?: AVAILABLE
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
        return when {
            lower.contains("exam") -> SmartMode.EXAM
            lower.contains("meeting") -> SmartMode.MEETING
            lower.contains("driving") || lower.contains("drive") -> SmartMode.DRIVING
            lower.contains("sleep") || lower.contains("sleeping") -> SmartMode.SLEEP
            lower.contains("rest") || lower.contains("resting") -> SmartMode.REST
            lower.contains("study") || lower.contains("studying") -> SmartMode.STUDY
            lower.contains("class") -> SmartMode.CLASS
            lower.contains("work") || lower.contains("working") -> SmartMode.WORK
            lower.contains("gym") -> SmartMode.GYM
            lower.contains("dnd") || lower.contains("don't disturb") || lower.contains("do not disturb") -> SmartMode.DND
            lower.contains("travel") || lower.contains("traveling") -> SmartMode.TRAVEL
            lower.contains("eating") || lower.contains("eat") -> SmartMode.EATING
            lower.contains("cinema") || lower.contains("movie") -> SmartMode.CINEMA
            lower.contains("focus") -> SmartMode.FOCUS
            lower.contains("busy") -> SmartMode.BUSY
            lower.contains("available") -> SmartMode.AVAILABLE
            else -> null
        }
    }
}

object ResponseGenerator {
    fun generate(mode: SmartMode, privacy: PrivacyLevel, caller: String? = null): String {
        val base = when (privacy) {
            PrivacyLevel.LOW -> mode.callResponse
            PrivacyLevel.MEDIUM -> when (mode) {
                SmartMode.EXAM -> "Hello, I'm JARVIS, Shlok's assistant. He's currently unavailable because he's in an exam. Please leave a message."
                SmartMode.DRIVING -> "Hello, I'm JARVIS, Shlok's assistant. He's currently driving and can't safely take the call. Please leave a message."
                SmartMode.SLEEP, SmartMode.REST -> "Hello, I'm JARVIS, Shlok's assistant. He's currently unavailable and may not be able to answer. Please leave a message."
                else -> "Hello, I'm JARVIS, Shlok's assistant. He's currently ${mode.description.lowercase()} and can't take the call. Please leave a message."
            }
            PrivacyLevel.HIGH -> "Hello, I'm JARVIS, Shlok's personal assistant. Shlok is currently unavailable and cannot take the call. Please leave a message."
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
