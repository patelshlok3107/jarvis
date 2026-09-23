package com.shlok.jarvis.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.shlok.jarvis.MainActivity
import com.shlok.jarvis.data.JarvisStatus
import com.shlok.jarvis.engine.SmartMode

/**
 * Notification Manager — independent component.
 * Builds persistent notifications that ALWAYS reflect actual service state.
 * Failure here must not crash UI/service.
 */
object JarvisNotificationManager {

    fun buildForegroundNotification(ctx: Context, status: JarvisStatus, isListening: Boolean = false, wakeWordActive: Boolean = false): Notification {
        val open = PendingIntent.getActivity(
            ctx, 0, Intent(ctx, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stop = PendingIntent.getService(
            ctx, 1, Intent(ctx, JarvisForegroundService::class.java).apply { action = JarvisForegroundService.ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val dot = when (status) {
            JarvisStatus.AVAILABLE -> "● AVAILABLE"
            JarvisStatus.BUSY -> "● BUSY"
            JarvisStatus.DND -> "◐ DND"
            JarvisStatus.DRIVING -> "◑ DRIVING"
            JarvisStatus.SLEEPING -> "◒ SLEEPING"
            JarvisStatus.MEETING -> "◓ MEETING"
        }
        val title = when {
            status == JarvisStatus.AVAILABLE && !isListening -> "JARVIS — $dot"
            status != JarvisStatus.AVAILABLE && !isListening -> "JARVIS — $dot"
            else -> "JARVIS — $dot"
        }
        val text = when {
            wakeWordActive && isListening -> "Listening for \"Hey JARVIS\""
            wakeWordActive -> "Wake word active — say \"Hey JARVIS\""
            status != JarvisStatus.AVAILABLE -> "Call Assistant Active • Tap to open"
            else -> status.subtitle + " • Tap to open"
        }
        return NotificationCompat.Builder(ctx, "jarvis_service")
            .setSmallIcon(android.R.drawable.presence_online)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setContentIntent(open)
            .addAction(android.R.drawable.ic_delete, "Stop", stop)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    fun buildVoiceNotification(ctx: Context, listening: Boolean, wakeWord: Boolean): Notification {
        val open = PendingIntent.getActivity(ctx, 0, Intent(ctx, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val title = when {
            listening && wakeWord -> "JARVIS — Listening for Hey JARVIS"
            listening -> "JARVIS — Listening"
            wakeWord -> "JARVIS — Wake word active"
            else -> "JARVIS — Voice ready"
        }
        val text = when {
            listening -> "Microphone active"
            wakeWord -> "Say \"Hey JARVIS\" • Tap to open"
            else -> "Tap notification to speak"
        }
        return NotificationCompat.Builder(ctx, "jarvis_service")
            .setSmallIcon(android.R.drawable.presence_audio_online)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(wakeWord || listening)
            .setContentIntent(open)
            .build()
    }

    fun buildOfflineNotification(ctx: Context): Notification {
        val open = PendingIntent.getActivity(ctx, 0, Intent(ctx, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(ctx, "jarvis_service")
            .setSmallIcon(android.R.drawable.presence_invisible)
            .setContentTitle("JARVIS ⚠ Voice Assistant Offline")
            .setContentText("Tap to fix • Check microphone permission")
            .setContentIntent(open)
            .setOngoing(false)
            .build()
    }

    fun buildBusyNotification(ctx: Context, mode: SmartMode): Notification {
        val open = PendingIntent.getActivity(ctx, 0, Intent(ctx, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(ctx, "jarvis_service")
            .setSmallIcon(android.R.drawable.presence_busy)
            .setContentTitle("JARVIS — ● ${mode.displayName}")
            .setContentText("Call Assistant Active • ${mode.description}")
            .setContentIntent(open)
            .setOngoing(true)
            .build()
    }

    // Unified call-handled notification — replaces old NotificationHelper
    fun notifyHandledCall(ctx: Context, entry: com.shlok.jarvis.data.CallHistoryEntry) {
        val intent = Intent(ctx, MainActivity::class.java).apply { putExtra("open_history", true) }
        val pi = PendingIntent.getActivity(ctx, entry.id.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val simTag = if (entry.isSimulated) " [Simulated — grant Default Dialer for real answering]" else ""
        val n = NotificationCompat.Builder(ctx, "jarvis_calls")
            .setSmallIcon(android.R.drawable.sym_call_missed)
            .setContentTitle("JARVIS handled call from ${entry.callerName ?: entry.callerNumber}$simTag")
            .setContentText(entry.jarvisResponse ?: "Busy — caller was notified.")
            .setStyle(NotificationCompat.BigTextStyle().bigText(entry.jarvisResponse))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .build()
        try { androidx.core.app.NotificationManagerCompat.from(ctx).notify(entry.id.hashCode(), n) } catch (_: SecurityException) {}
    }

    fun notifyMessage(ctx: Context, from: String, message: String) {
        val pi = PendingIntent.getActivity(ctx, 0, Intent(ctx, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(ctx, "jarvis_messages")
            .setSmallIcon(android.R.drawable.sym_action_chat)
            .setContentTitle("Message from $from")
            .setContentText(message)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        try { androidx.core.app.NotificationManagerCompat.from(ctx).notify((from+message).hashCode(), n) } catch (_: SecurityException) {}
    }
}
