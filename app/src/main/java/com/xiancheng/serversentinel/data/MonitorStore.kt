package com.xiancheng.serversentinel.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.monitorDataStore by preferencesDataStore("server_sentinel")
private val monitorsKey = stringPreferencesKey("monitors")
private val realtimeKey = stringPreferencesKey("realtime")
private val lastCheckKey = stringPreferencesKey("last_check")
private val logsKey = stringPreferencesKey("monitor_logs")
private val onboardingKey = stringPreferencesKey("onboarding_complete")

class MonitorStore(private val context: Context) {
    suspend fun monitors(): List<MonitorConfig> {
        val raw = context.monitorDataStore.data.first()[monitorsKey] ?: return defaults
        return runCatching { JSONArray(raw).let { list -> List(list.length()) { index -> list.getJSONObject(index).let { MonitorConfig(it.getString("id"), it.getString("name"), it.getString("host"), it.getInt("port"), it.optBoolean("enabled", true)) } } } }.getOrElse { defaults }
    }
    suspend fun save(items: List<MonitorConfig>) { context.monitorDataStore.edit { it[monitorsKey] = JSONArray().also { array -> items.forEach { item -> array.put(JSONObject().put("id", item.id).put("name", item.name).put("host", item.host).put("port", item.port).put("enabled", item.enabled)) } }.toString() } }
    suspend fun realtimeEnabled(): Boolean = context.monitorDataStore.data.first()[realtimeKey] == "true"
    suspend fun setRealtime(enabled: Boolean) { context.monitorDataStore.edit { it[realtimeKey] = enabled.toString() } }
    suspend fun lastCheck(): LastCheckSnapshot? = context.monitorDataStore.data.first()[lastCheckKey]?.let { runCatching { LastCheckSnapshot.fromJson(it) }.getOrNull() }
    suspend fun saveLastCheck(snapshot: LastCheckSnapshot) { context.monitorDataStore.edit { it[lastCheckKey] = snapshot.toJson() } }
    suspend fun logs(): List<MonitorLogEntry> = MonitorLogCodec.decode(context.monitorDataStore.data.first()[logsKey])
    fun latestLogFlow(): Flow<MonitorLogEntry?> = context.monitorDataStore.data.map { prefs ->
        MonitorLogCodec.decode(prefs[logsKey]).firstOrNull()
    }
    suspend fun appendLog(entry: MonitorLogEntry) { context.monitorDataStore.edit { prefs -> prefs[logsKey] = MonitorLogCodec.toJson(MonitorLogCodec.append(prefs[logsKey], entry)) } }
    suspend fun onboardingComplete(): Boolean = context.monitorDataStore.data.first()[onboardingKey] == "true"
    suspend fun setOnboardingComplete() { context.monitorDataStore.edit { it[onboardingKey] = "true" } }
    companion object { val defaults = emptyList<MonitorConfig>() }
}
