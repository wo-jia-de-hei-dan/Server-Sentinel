package com.xiancheng.serversentinel

import com.xiancheng.serversentinel.data.CheckMode
import com.xiancheng.serversentinel.data.MonitorConfig
import org.junit.Assert.assertEquals
import org.junit.Test

class MonitorProbeTest {
    @Test fun `tcp mode is selected by the server probe`() {
        assertEquals(CheckMode.TCP, ServerProbe.modeFor(MonitorConfig("a", "A", "host", 25565, true, CheckMode.TCP)))
    }
}
