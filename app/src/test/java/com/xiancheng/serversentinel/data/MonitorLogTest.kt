package com.xiancheng.serversentinel.data

import org.junit.Assert.assertEquals
import org.junit.Test

class MonitorLogTest {
    @Test fun `appending keeps the newest 100 entries`() {
        var raw: String? = null
        (1..101).forEach { index ->
            raw = MonitorLogCodec.toJson(MonitorLogCodec.append(raw, MonitorLogEntry(index.toLong(), "手动", emptyList())))
        }

        val logs = MonitorLogCodec.decode(raw)
        assertEquals(100, logs.size)
        assertEquals(101L, logs.first().checkedAtMillis)
        assertEquals(2L, logs.last().checkedAtMillis)
    }

    @Test fun `round trips server log details`() {
        val original = listOf(MonitorLogEntry(123L, "自动", listOf(MonitorLogServer("主服", false, "连接超时", null))))
        assertEquals(original, MonitorLogCodec.decode(MonitorLogCodec.toJson(original)))
    }
}
