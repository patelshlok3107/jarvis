package com.shlok.jarvis.manager

import android.content.Context
import com.shlok.jarvis.data.JarvisStatus
import com.shlok.jarvis.engine.PrivacyLevel
import com.shlok.jarvis.engine.ResponseGenerator
import com.shlok.jarvis.engine.ScheduledMode
import com.shlok.jarvis.engine.SmartMode
import com.shlok.jarvis.engine.SmartModeEngine
import com.shlok.jarvis.engine.SmartModeMapper
import com.shlok.jarvis.engine.TimeParser
import com.shlok.jarvis.service.ModeScheduler
import com.shlok.jarvis.storage.JarvisLogger
import com.shlok.jarvis.storage.JarvisPreferences
import com.shlok.jarvis.storage.ScheduledModeStore
import kotlinx.coroutines.flow.first

/**
 * Clean Mode Manager — independent from Voice/Call/Notification.
 * Single source of truth for mode transitions.
 * Failure here must NOT crash UI — all methods are suspend with try/catch at caller.
 */
class ModeManager(private val prefs: JarvisPreferences) {
    private val engine = SmartModeEngine(prefs)

    suspend fun getCurrentMode(ctx: Context): SmartMode = engine.getCurrentMode(ctx)

    suspend fun setMode(ctx: Context, mode: SmartMode): Result<SmartMode> {
        return try {
            prefs.setStatus(mode.status)
            JarvisLogger.log(ctx, "MODE_SET", mode.name)
            Result.success(mode)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun setModeByUtterance(ctx: Context, utterance: String): Result<ModeResult> {
        return try {
            val lower = utterance.lowercase()
            // Handle "I'm available now" / "cancel" / "disable"
            if (lower.contains("available") || lower.contains("cancel") || lower.contains("disable") || lower.contains("free now")) {
                setMode(ctx, SmartMode.AVAILABLE)
                return Result.success(ModeResult(SmartMode.AVAILABLE, isScheduled = false, ack = "You're available, Shlok."))
            }
            // Handle custom "tell callers I'm ..." — save custom message
            if (lower.contains("tell callers") || lower.contains("tell caller")) {
                val custom = extractCustomMessage(utterance)
                if (custom != null) {
                    // Save custom message as CUSTOM mode with custom TTS
                    prefs.setString(com.shlok.jarvis.storage.PrefKeys.CUSTOM_CALL_MESSAGE, custom)
                    setMode(ctx, SmartMode.CUSTOM)
                    return Result.success(ModeResult(SmartMode.CUSTOM, ack = "Okay, I'll tell callers: $custom"))
                }
            }
            // Handle "I'm at the gym" -> GYM etc, with optional time
            val mode = SmartModeMapper.fromUtterance(lower)
            if (mode != null) {
                val (start, end) = TimeParser.parse(lower)
                val now = System.currentTimeMillis()
                // If scheduled future: "I have an exam in 10 minutes"
                if (start != null && start > now + 30_000) {
                    val scheduled = ScheduledMode(
                        id = now.toString(),
                        mode = mode,
                        startTime = start,
                        endTime = end ?: (start + 2*60*60*1000),
                        privacy = PrivacyLevel.MEDIUM
                    )
                    ScheduledModeStore.add(ctx, scheduled)
                    ModeScheduler.schedule(ctx, scheduled)
                    val fmt = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault()).format(java.util.Date(start))
                    val ack = when (mode) {
                        SmartMode.EXAM -> "Okay. I'll activate Exam Mode in 10 minutes at $fmt."
                        else -> "Okay. I'll activate ${mode.displayName} mode at $fmt."
                    }
                    JarvisLogger.log(ctx, "MODE_SCHEDULED", "${mode.name} at $fmt")
                    return Result.success(ModeResult(mode, isScheduled = true, ack = ack, scheduled = scheduled))
                } else if (end != null && end > now) {
                    setMode(ctx, mode)
                    val scheduled = ScheduledMode(
                        id = now.toString(),
                        mode = mode,
                        startTime = now,
                        endTime = end
                    )
                    ScheduledModeStore.add(ctx, scheduled)
                    ModeScheduler.schedule(ctx, scheduled)
                    val mins = ((end - now) / 60000).toInt()
                    val ack = when (mode) {
                        SmartMode.EXAM -> "Understood. I'll activate Exam Mode for the next ${mins} minutes."
                        else -> "${mode.displayName} mode is active for the next ${if (mins >= 60) "${mins/60} hours" else "$mins minutes"}."
                    }
                    return Result.success(ModeResult(mode, ack = ack, scheduled = scheduled))
                } else {
                    // Immediate mode switch
                    setMode(ctx, mode)
                    val ack = when (mode) {
                        SmartMode.EXAM -> "Understood. I'll activate Exam Mode."
                        SmartMode.MEETING -> "Meeting mode on. I'll handle your calls."
                        SmartMode.DRIVING -> "Driving mode on."
                        SmartMode.SLEEPING -> "Sleep mode on."
                        SmartMode.BUSY -> "Understood, Shlok. I'll handle your incoming calls."
                        SmartMode.GYM -> "Okay. I'll handle your calls while you're at the gym."
                        SmartMode.STUDYING -> "Study mode on. I'll handle your calls while you're studying."
                        SmartMode.CINEMA -> "Cinema mode on. I'll keep things quiet."
                        else -> mode.status.spokenAck
                    }
                    return Result.success(ModeResult(mode, ack = ack))
                }
            }
            Result.failure(IllegalArgumentException("Unknown mode from: $utterance"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun extractCustomMessage(utterance: String): String? {
        val lower = utterance.lowercase()
        // "tell callers I'm studying" -> "I'm studying"
        Regex("""tell callers?\s+(.*)""").find(lower)?.let {
            var msg = it.groupValues[1].trim()
            if (msg.isNotEmpty()) {
                // Capitalize and turn into JARVIS speech: "He's studying"
                msg = msg.replace("i'm", "Shlok is").replace("i am", "Shlok is")
                return "Hello, I'm JARVIS, Shlok's AI assistant. $msg. Please leave a message."
            }
        }
        return null
    }

    fun generateCallResponse(mode: SmartMode, caller: String? = null, privacy: PrivacyLevel = PrivacyLevel.MEDIUM, customMessage: String? = null): String {
        return ResponseGenerator.generate(ResponseGenerator.CallContext(caller, mode, customMessage = customMessage), privacy)
    }

    data class ModeResult(
        val mode: SmartMode,
        val isScheduled: Boolean = false,
        val ack: String,
        val scheduled: ScheduledMode? = null
    )
}
