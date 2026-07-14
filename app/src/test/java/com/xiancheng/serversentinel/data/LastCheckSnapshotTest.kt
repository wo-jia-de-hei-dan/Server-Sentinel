package com.xiancheng.serversentinel.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class LastCheckSnapshotTest {
    @Test fun `round trips last check state`() {
        val original = LastCheckSnapshot(1_720_931_680_000, listOf(LastServerState("default", true, "1.21", "4/20 玩家", "Hello", 38)))
        assertEquals(original, LastCheckSnapshot.fromJson(original.toJson()))
    }

    @Test fun `formats saved timestamp relative to the current time`() {
        val checkedAt = ZonedDateTime.of(2026, 7, 14, 10, 8, 0, 0, ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli()
        val now = checkedAt + 61_000L
        assertEquals("1 分钟前 · 10:08", LastCheckTimeFormatter.format(checkedAt, now, ZoneId.of("Asia/Shanghai")))
    }
}
