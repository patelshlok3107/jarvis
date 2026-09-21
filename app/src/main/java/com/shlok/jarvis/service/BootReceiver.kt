package com.shlok.jarvis.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.first

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Critical: This is the ONLY way JARVIS becomes ready after reboot without opening Chrome/browser.
        // We restore the previous status from DataStore and restart the foreground service where permitted.
        // Android may require the user to have opened the app once after install (security), we log and handle.
        val action = intent.action
        if (action in setOf(
                Intent.ACTION_BOOT_COMPLETED,
                "android.intent.action.LOCKED_BOOT_COMPLETED",
                Intent.ACTION_MY_PACKAGE_REPLACED,
                Intent.ACTION_PACKAGE_REPLACED
            )) {
            try {
                android.util.Log.i("JarvisBoot", "Boot event: $action, restoring JARVIS")
                // Use goAsync to allow async DataStore read without blocking broadcast
                val pending = goAsync()
                kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        // Check if we have a stored status; if not, default to AVAILABLE and don't auto-start
                        // But we always start the service to make JARVIS ready, as the service is lightweight
                        // and will show notification only if status != AVAILABLE or user enabled it.
                        // We respect the user's previous choice: if they had set BUSY, we restore it.
                        // DataStore read with timeout to avoid blocking
                        val prefs = com.shlok.jarvis.storage.JarvisPreferences(context)
                        val status = try {
                            kotlinx.coroutines.withTimeoutOrNull(3000) { prefs.statusFlow.first() }
                        } catch (_: Exception) { null }
                        android.util.Log.i("JarvisBoot", "Restoring status: $status")
                        try {
                            com.shlok.jarvis.storage.JarvisLogger.log(context, "BOOT_RESTORE", "action=$action status=$status")
                        } catch (_: Exception) {}
                        // Always start the foreground service where permitted; it will show notification based on current status
                        // Handles Doze: the service is START_STICKY and will be restarted by system if killed
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(Intent(context, JarvisForegroundService::class.java))
                        } else {
                            context.startService(Intent(context, JarvisForegroundService::class.java))
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("JarvisBoot", "Failed to restore", e)
                    } finally {
                        pending.finish()
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("JarvisBoot", "Boot receiver error", e)
                try {
                    // Fallback: try direct start without async
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(Intent(context, JarvisForegroundService::class.java))
                    } else {
                        context.startService(Intent(context, JarvisForegroundService::class.java))
                    }
                } catch (_: Exception) {}
            }
        }
    }
}
