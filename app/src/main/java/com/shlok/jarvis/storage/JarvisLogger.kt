package com.shlok.jarvis.storage

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val LOG_KEY = stringPreferencesKey("jarvis_log_json")

data class LogEntry(
    val timestamp: Long,
    val event: String,
    val details: String
)

object JarvisLogger {
    private const val MAX_LOGS = 200

    suspend fun log(ctx: Context, event: String, details: String) {
        try {
            val entry = LogEntry(System.currentTimeMillis(), event, details)
            val current = getLogs(ctx).toMutableList()
            current.add(0, entry)
            if (current.size > MAX_LOGS) current.subList(MAX_LOGS, current.size).clear()
            val json = JSONArray()
            current.forEach { e ->
                json.put(JSONObject().apply {
                    put("timestamp", e.timestamp)
                    put("event", e.event)
                    put("details", e.details)
                })
            }
            ctx.dataStore.edit { it[LOG_KEY] = json.toString() }
            Log.i("JarvisLogger", "$event: $details")
        } catch (e: Exception) {
            Log.e("JarvisLogger", "Failed to log", e)
        }
    }

    fun logSync(ctx: Context, event: String, details: String) {
        // For use from non-suspend contexts, fire and forget
        kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            log(ctx, event, details)
        }
    }

    suspend fun getLogs(ctx: Context): List<LogEntry> {
        return try {
            val raw = ctx.dataStore.data.map { it[LOG_KEY] ?: "[]" }.first()
            val arr = JSONArray(raw)
            val list = mutableListOf<LogEntry>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(LogEntry(o.getLong("timestamp"), o.getString("event"), o.getString("details")))
            }
            list
        } catch (_: Exception) { emptyList() }
    }

    fun getLogsFlow(ctx: Context): Flow<List<LogEntry>> = ctx.dataStore.data.map { prefs ->
        try {
            val raw = prefs[LOG_KEY] ?: "[]"
            val arr = JSONArray(raw)
            val list = mutableListOf<LogEntry>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(LogEntry(o.getLong("timestamp"), o.getString("event"), o.getString("details")))
            }
            list
        } catch (_: Exception) { emptyList() }
    }

    fun format(entry: LogEntry): String {
        val fmt = SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault()).format(Date(entry.timestamp))
        return "[$fmt] ${entry.event}: ${entry.details}"
    }
}
