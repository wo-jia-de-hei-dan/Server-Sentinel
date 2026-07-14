package com.xiancheng.serversentinel.data

import org.json.JSONArray
import org.json.JSONObject
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class LastServerState(val id: String, val online: Boolean, val detail: String, val players: String, val motd: String, val latency: Long?)

data class LastCheckSnapshot(val checkedAtMillis: Long, val servers: List<LastServerState>) {
    fun toJson(): String = JSONObject()
        .put("checkedAtMillis", checkedAtMillis)
        .put("servers", JSONArray().also { array ->
            servers.forEach { item ->
                array.put(JSONObject()
                    .put("id", item.id)
                    .put("online", item.online)
                    .put("detail", item.detail)
                    .put("players", item.players)
                    .put("motd", item.motd)
                    .put("latency", item.latency))
            }
        }).toString()

    companion object {
        fun fromJson(raw: String): LastCheckSnapshot {
            val root = JSONObject(raw)
            val array = root.getJSONArray("servers")
            return LastCheckSnapshot(
                root.optLong("checkedAtMillis", 0L),
                List(array.length()) { index ->
                    array.getJSONObject(index).let {
                        LastServerState(it.getString("id"), it.getBoolean("online"), it.getString("detail"), it.getString("players"), it.getString("motd"), if (it.isNull("latency")) null else it.getLong("latency"))
                    }
                }
            )
        }
    }
}

object DashboardLastCheck {
    fun timestamp(snapshot: LastCheckSnapshot?, logs: List<MonitorLogEntry>): Long =
        logs.firstOrNull()?.checkedAtMillis ?: snapshot?.checkedAtMillis ?: 0L
}

object LastCheckTimeFormatter {
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("MM-dd HH:mm")

    fun format(checkedAtMillis: Long, nowMillis: Long = System.currentTimeMillis(), zoneId: ZoneId = ZoneId.systemDefault()): String {
        if (checkedAtMillis <= 0L) return "尚未检测"
        val checkedAt = Instant.ofEpochMilli(checkedAtMillis).atZone(zoneId)
        val now = Instant.ofEpochMilli(nowMillis).atZone(zoneId)
        val minutesAgo = Duration.between(checkedAt, now).toMinutes().coerceAtLeast(0)
        val time = checkedAt.format(timeFormatter)
        return when {
            minutesAgo == 0L -> "刚刚 · $time"
            minutesAgo < 60L -> "$minutesAgo 分钟前 · $time"
            checkedAt.toLocalDate() == now.toLocalDate() -> "今天 · $time"
            else -> checkedAt.format(dateTimeFormatter)
        }
    }
}
