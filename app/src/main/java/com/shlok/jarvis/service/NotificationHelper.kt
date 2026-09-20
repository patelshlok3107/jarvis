package com.shlok.jarvis.service

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.shlok.jarvis.MainActivity
import com.shlok.jarvis.data.CallHistoryEntry

object NotificationHelper {
    fun notifyHandledCall(ctx: Context, entry: CallHistoryEntry) {
        val intent = Intent(ctx, MainActivity::class.java).apply { putExtra("open_history", true) }
        val pi = PendingIntent.getActivity(ctx, entry.id.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val simTag = if (entry.isSimulated) " [Demo — grant Default Dialer for real answering]" else ""
        val n = NotificationCompat.Builder(ctx, "jarvis_calls")
            .setSmallIcon(android.R.drawable.sym_call_missed)
            .setContentTitle("JARVIS handled call from ${entry.callerName ?: entry.callerNumber}$simTag")
            .setContentText(entry.jarvisResponse ?: "Busy — caller was notified.")
            .setStyle(NotificationCompat.BigTextStyle().bigText(entry.jarvisResponse))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .build()
        try { NotificationManagerCompat.from(ctx).notify(entry.id.hashCode(), n) } catch (_: SecurityException) {}
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
        try { NotificationManagerCompat.from(ctx).notify((from+message).hashCode(), n) } catch (_: SecurityException) {}
    }
}
