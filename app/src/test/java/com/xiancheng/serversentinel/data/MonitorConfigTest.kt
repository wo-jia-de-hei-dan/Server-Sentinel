package com.xiancheng.serversentinel.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MonitorConfigTest {
    @Test fun `new configuration uses status mode`() = assertEquals(CheckMode.STATUS, MonitorConfig("a", "A", "host", 25565, true).checkMode)
    @Test fun `shared app has no bundled server defaults`() = assertTrue(MonitorStore.defaults.isEmpty())
    @Test fun `empty host is rejected`() = assertEquals("服务器地址不能为空", MonitorConfig("a", "A", "", 25565, true).validate())
    @Test fun `out of range port is rejected`() = assertEquals("端口范围是 1 到 65535", MonitorConfig("a", "A", "host", 70000, true).validate())
}
