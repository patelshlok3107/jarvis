package com.shlok.jarvis.permissions

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

object PermissionManager {

    val REQUIRED = arrayOf(
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.READ_CALL_LOG,
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.POST_NOTIFICATIONS,
    )
    val CALL = arrayOf(Manifest.permission.CALL_PHONE, Manifest.permission.ANSWER_PHONE_CALLS)

    fun missingPermissions(ctx: Context): List<String> = (REQUIRED + CALL).filter {
        ContextCompat.checkSelfPermission(ctx, it) != PackageManager.PERMISSION_GRANTED
    }

    fun isCallScreeningGranted(ctx: Context): Boolean {
        // CallScreeningService role is granted via ROLE_CALL_SCREENING (Android 10+) or default dialer
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val rm = ctx.getSystemService(RoleManager::class.java)
                rm.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
            } else true // pre-Q, manifest permission suffices if user enables via settings
        } catch (_: Exception) { false }
    }

    fun isDefaultDialer(ctx: Context): Boolean {
        return try {
            val tm = ctx.getSystemService(android.telecom.TelecomManager::class.java)
            ctx.packageName == tm.defaultDialerPackage
        } catch (_: Exception) { false }
    }

    fun intentRequestCallScreeningRole(ctx: Context): Intent? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val rm = ctx.getSystemService(RoleManager::class.java)
                rm.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
            } else null
        } catch (_: Exception) { null }
    }

    fun intentDefaultDialer(ctx: Context): Intent =
        Intent(android.telecom.TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).putExtra(android.telecom.TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, ctx.packageName)

    fun intentBatteryOptimization(ctx: Context): Intent =
        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = android.net.Uri.parse("package:${ctx.packageName}")
        }

    fun intentNotificationSettings(ctx: Context): Intent =
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
}
