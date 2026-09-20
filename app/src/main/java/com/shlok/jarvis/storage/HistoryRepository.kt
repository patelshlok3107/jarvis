package com.shlok.jarvis.storage

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import com.shlok.jarvis.data.CallHistoryEntry
import org.json.JSONArray
import org.json.JSONObject

private val HISTORY_KEY = stringPreferencesKey("call_history_json")

object HistoryRepository {

    fun historyFlow(context: Context): Flow<List<CallHistoryEntry>> =
        context.dataStore.data.map { prefs ->
            val raw = prefs[HISTORY_KEY] ?: "[]"
            try { decode(raw) } catch (_: Exception) { emptyList() }
        }

    suspend fun add(context: Context, entry: CallHistoryEntry) {
        val current = historyFlow(context).first().toMutableList()
        current.add(0, entry)
        if (current.size > 500) current.subList(500, current.size).clear()
        context.dataStore.edit { it[HISTORY_KEY] = encode(current) }
    }

    suspend fun clear(context: Context) {
        context.dataStore.edit { it[HISTORY_KEY] = "[]" }
    }

    suspend fun delete(context: Context, id: String) {
        val filtered = historyFlow(context).first().filterNot { it.id == id }
        context.dataStore.edit { it[HISTORY_KEY] = encode(filtered) }
    }

    private fun encode(list: List<CallHistoryEntry>): String {
        val arr = JSONArray()
        list.forEach { e ->
            arr.put(JSONObject().apply {
                put("id", e.id); put("callerName", e.callerName); put("callerNumber", e.callerNumber)
                put("timestampMillis", e.timestampMillis); put("status", e.status.name)
                put("disposition", e.disposition.name); put("jarvisResponse", e.jarvisResponse)
                put("transcript", e.transcript); put("isSimulated", e.isSimulated)
            })
        }
        return arr.toString()
    }
    private fun decode(raw: String): List<CallHistoryEntry> {
        val arr = JSONArray(raw); val out = mutableListOf<CallHistoryEntry>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out.add(CallHistoryEntry(
                id = o.getString("id"),
                callerName = if (o.isNull("callerName")) null else o.getString("callerName"),
                callerNumber = o.getString("callerNumber"),
                timestampMillis = o.getLong("timestampMillis"),
                status = com.shlok.jarvis.data.JarvisStatus.fromString(o.getString("status")),
                disposition = try { com.shlok.jarvis.data.CallDisposition.valueOf(o.getString("disposition")) } catch (_:Exception){ com.shlok.jarvis.data.CallDisposition.HANDLED_BY_JARVIS },
                jarvisResponse = if (o.isNull("jarvisResponse")) null else o.getString("jarvisResponse"),
                transcript = if (o.isNull("transcript")) null else o.getString("transcript"),
                isSimulated = o.optBoolean("isSimulated", false)
            ))
        }
        return out
    }
}


