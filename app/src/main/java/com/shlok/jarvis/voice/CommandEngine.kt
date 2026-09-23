package com.shlok.jarvis.voice

/**
 * Command Engine — independent from VoiceAssistant and UI.
 * Parses natural language utterances into structured commands.
 * No network, no browser — local parsing.
 * Must handle all spec examples:
 *  "Hey JARVIS, I'm busy." / "I'm driving." / "I'm in an exam." / "I'm going to sleep." / "I'm in a meeting." / "I'm available now."
 */
object CommandEngine {
    sealed class Command {
        data class SetMode(val utterance: String) : Command()
        data class ScheduleMode(val utterance: String) : Command()
        data class AskStatus(val utterance: String) : Command()
        data class ShowHistory(val utterance: String) : Command()
        data class CustomTell(val utterance: String) : Command()
        data class Unknown(val utterance: String) : Command()
    }

    fun parse(utterance: String): Command {
        val lower = utterance.lowercase().trim().removePrefix("hey jarvis").removePrefix("jarvis").trim()
        return when {
            lower.contains("what mode") || lower.contains("how long") || lower.contains("what's my mode") -> Command.AskStatus(utterance)
            lower.contains("who called") || lower.contains("show messages") || lower.contains("show calls") || lower.contains("missed calls") || lower.contains("call history") -> Command.ShowHistory(utterance)
            lower.contains("tell callers") || lower.contains("tell caller") -> Command.CustomTell(utterance)
            lower.contains("in ") && lower.contains("minute") || lower.contains("in ") && lower.contains("hour") || lower.contains("for ") && lower.contains("hour") || lower.contains("for ") && lower.contains("minute") || lower.contains("until ") || lower.contains("at ") && lower.contains(":") || lower.contains("tonight") || lower.contains("tomorrow") -> {
                // Contains time phrase — likely scheduling
                Command.ScheduleMode(utterance)
            }
            // Direct mode set
            lower.contains("busy") || lower.contains("available") || lower.contains("dnd") || lower.contains("don't disturb") || lower.contains("driving") || lower.contains("sleep") || lower.contains("exam") || lower.contains("meeting") || lower.contains("gym") || lower.contains("study") || lower.contains("class") || lower.contains("work") || lower.contains("cinema") || lower.contains("rest") || lower.contains("at the") -> Command.SetMode(utterance)
            else -> Command.Unknown(utterance)
        }
    }
}
