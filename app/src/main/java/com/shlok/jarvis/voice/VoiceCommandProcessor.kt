package com.shlok.jarvis.voice

import com.shlok.jarvis.data.JarvisStatus
import com.shlok.jarvis.storage.HistoryRepository

sealed class VoiceCommand {
    data class SetStatus(val status: JarvisStatus, val durationMinutes: Int? = null) : VoiceCommand()
    data object ShowMissedCalls : VoiceCommand()
    data object WhoCalled : VoiceCommand()
    data class CallContact(val name: String) : VoiceCommand()
    data class TellContact(val name: String, val message: String) : VoiceCommand()
    data object Unknown : VoiceCommand()
}

data class VoiceResult(val reply: String, val command: VoiceCommand, val newStatus: JarvisStatus? = null)

object VoiceCommandProcessor {

    fun parse(utterance: String): VoiceCommand {
        val u = utterance.lowercase().trim()
            .removePrefix("hey jarvis").removePrefix("jarvis").trim()
        return when {
            // status
            "i'm busy" in u || "i am busy" in u || "busy mode" in u -> VoiceCommand.SetStatus(JarvisStatus.BUSY)
            "available" in u -> VoiceCommand.SetStatus(JarvisStatus.AVAILABLE)
            "don't disturb" in u || "do not disturb" in u || "dnd" in u -> {
                val mins = Regex("(\\d+)\\s*(hour|minute|min)").find(u)?.let {
                    val n = it.groupValues[1].toIntOrNull() ?: 0
                    if ("hour" in it.groupValues[2]) n*60 else n
                }
                VoiceCommand.SetStatus(JarvisStatus.DND, mins)
            }
            "driving" in u -> VoiceCommand.SetStatus(JarvisStatus.DRIVING)
            "sleep" in u || "going to sleep" in u -> VoiceCommand.SetStatus(JarvisStatus.SLEEPING)
            "meeting" in u -> VoiceCommand.SetStatus(JarvisStatus.MEETING)
            "disable busy" in u || "turn off busy" in u -> VoiceCommand.SetStatus(JarvisStatus.AVAILABLE)
            "who called" in u -> VoiceCommand.WhoCalled
            "missed calls" in u || "show missed" in u || "show calls" in u -> VoiceCommand.ShowMissedCalls
            u.startsWith("call ") -> VoiceCommand.CallContact(u.removePrefix("call ").trim())
            u.startsWith("tell ") && "i'm busy" in u -> {
                val name = u.removePrefix("tell ").substringBefore(" ").trim()
                VoiceCommand.TellContact(name, "Shlok is busy")
            }
            else -> VoiceCommand.Unknown
        }
    }

    fun replyFor(cmd: VoiceCommand, status: JarvisStatus? = null): VoiceResult = when(cmd){
        is VoiceCommand.SetStatus -> VoiceResult(cmd.status.spokenAck, cmd, cmd.status)
        VoiceCommand.WhoCalled -> VoiceResult("Checking your recent calls, Shlok.", cmd)
        VoiceCommand.ShowMissedCalls -> VoiceResult("Opening your call history.", cmd)
        is VoiceCommand.CallContact -> VoiceResult("Calling ${cmd.name}, Shlok.", cmd)
        is VoiceCommand.TellContact -> VoiceResult("I'll let ${cmd.name} know you're busy.", cmd)
        VoiceCommand.Unknown -> VoiceResult("Sorry Shlok, I didn't catch that. Try: 'I'm busy' or 'I'm available'.", cmd)
    }
}
