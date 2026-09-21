package com.shlok.jarvis.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.shlok.jarvis.engine.ScheduledMode
import com.shlok.jarvis.engine.SmartMode
import com.shlok.jarvis.storage.JarvisLogger
import com.shlok.jarvis.storage.JarvisPreferences
import com.shlok.jarvis.storage.ScheduledModeStore
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

object ModeScheduler {
    private const val REQ_START = 1001
    private const val REQ_END = 1002

    fun schedule(ctx: Context, mode: ScheduledMode) {
        try {
            val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val startIntent = Intent(ctx, ModeAlarmReceiver::class.java).apply {
                action = "MODE_START"
                putExtra("mode", mode.mode.name)
                putExtra("id", mode.id)
            }
            val endIntent = Intent(ctx, ModeAlarmReceiver::class.java).apply {
                action = "MODE_END"
                putExtra("mode", mode.mode.name)
                putExtra("id", mode.id)
            }
            val startPI = PendingIntent.getBroadcast(ctx, REQ_START + mode.id.hashCode(), startIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val endPI = PendingIntent.getBroadcast(ctx, REQ_END + mode.id.hashCode(), endIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, mode.startTime, startPI)
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, mode.endTime, endPI)
            } else {
                am.setExact(AlarmManager.RTC_WAKEUP, mode.startTime, startPI)
                am.setExact(AlarmManager.RTC_WAKEUP, mode.endTime, endPI)
            }
            JarvisLogger.logSync(ctx, "SCHEDULER_SET", "${mode.mode.name} start=${mode.startTime} end=${mode.endTime}")
        } catch (e: Exception) {
            android.util.Log.e("ModeScheduler", "Failed to schedule", e)
        }
    }

    fun cancel(ctx: Context, mode: ScheduledMode) {
        try {
            val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val startIntent = Intent(ctx, ModeAlarmReceiver::class.java).apply { action = "MODE_START" }
            val endIntent = Intent(ctx, ModeAlarmReceiver::class.java).apply { action = "MODE_END" }
            am.cancel(PendingIntent.getBroadcast(ctx, REQ_START + mode.id.hashCode(), startIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            am.cancel(PendingIntent.getBroadcast(ctx, REQ_END + mode.id.hashCode(), endIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
        } catch (_: Exception) {}
    }

    suspend fun restoreAll(ctx: Context) {
        try {
            val all = ScheduledModeStore.flow(ctx).first()
            all.forEach { schedule(ctx, it) }
            JarvisLogger.log(ctx, "SCHEDULER_RESTORE", "restored ${all.size} modes")
        } catch (_: Exception) {}
    }
}

class ModeAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val modeName = intent.getStringExtra("mode") ?: return
        val id = intent.getStringExtra("id") ?: ""
        val action = intent.action
        val mode = try { SmartMode.fromString(modeName) } catch (_: Exception) { return }

        GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val prefs = JarvisPreferences(ctx)
                when (action) {
                    "MODE_START" -> {
                        prefs.setStatus(mode.status)
                        JarvisLogger.log(ctx, "MODE_AUTO_START", mode.name)
                        // Notify
                        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                        val notif = androidx.core.app.NotificationCompat.Builder(ctx, "jarvis_service")
                            .setSmallIcon(android.R.drawable.presence_online)
                            .setContentTitle("JARVIS — ${mode.displayName} MODE")
                            .setContentText("${mode.displayName} mode is now active")
                            .setAutoCancel(true)
                            .build()
                        try { nm.notify(2001, notif) } catch (_: Exception) {}
                    }
                    "MODE_END" -> {
                        // Restore to AVAILABLE or next scheduled mode
                        val next = ScheduledModeStore.getActive(ctx, System.currentTimeMillis() + 1000)
                        if (next == null) {
                            prefs.setStatus(SmartMode.AVAILABLE.status)
                            JarvisLogger.log(ctx, "MODE_AUTO_END", "${mode.name} -> AVAILABLE")
                            val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                            val notif = androidx.core.app.NotificationCompat.Builder(ctx, "jarvis_service")
                                .setSmallIcon(android.R.drawable.presence_online)
                                .setContentTitle("JARVIS — AVAILABLE")
                                .setContentText("${mode.displayName} mode has ended. You're back to Available.")
                                .setAutoCancel(true)
                                .build()
                            try { nm.notify(2001, notif) } catch (_: Exception) {}
                        }
                        ScheduledModeStore.remove(ctx, id)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ModeAlarm", "Error", e)
            }
        }
    }
}
