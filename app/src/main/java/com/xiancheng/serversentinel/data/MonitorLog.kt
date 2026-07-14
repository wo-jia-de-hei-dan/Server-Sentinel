package com.xiancheng.serversentinel.data

import org.json.JSONArray
import org.json.JSONObject

data class MonitorLogServer(val name: String, val online: Boolean, val detail: String, val latency: Long?)
data class MonitorLogEntry(val checkedAtMillis: Long, val source: String, val servers: List<MonitorLogServer>)

object MonitorLogCodec {
    fun append(raw: String?, entry: MonitorLogEntry): List<MonitorLogEntry> =
        (decode(raw) + entry).sortedByDescending { it.checkedAtMillis }.take(100)

    fun decode(raw: String?): List<MonitorLogEntry> = runCatching {
        if (raw.isNullOrBlank()) return emptyList()
        val array = JSONArray(raw)
        List(array.length()) { index ->
            val entry = array.getJSONObject(index)
            MonitorLogEntry(
                entry.getLong("checkedAtMillis"),
                entry.getString("source"),
                entry.getJSONArray("servers").let { servers ->
                    List(servers.length()) { serverIndex ->
                        servers.getJSONObject(serverIndex).let { server ->
                            MonitorLogServer(server.getString("name"), server.getBoolean("online"), server.getString("detail"), if (server.isNull("latency")) null else server.getLong("latency"))
                        }
                    }
                }
            )
        }
    }.getOrDefault(emptyList())

    fun toJson(entries: List<MonitorLogEntry>): String = JSONArray().also { array ->
        entries.forEach { entry ->
            array.put(JSONObject()
                .put("checkedAtMillis", entry.checkedAtMillis)
                .put("source", entry.source)
                .put("servers", JSONArray().also { servers ->
                    entry.servers.forEach { server ->
                        servers.put(JSONObject()
                            .put("name", server.name)
                            .put("online", server.online)
                            .put("detail", server.detail)
                            .put("latency", server.latency))
                    }
                }))
        }
    }.toString()
}
