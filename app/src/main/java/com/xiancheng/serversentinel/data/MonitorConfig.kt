package com.xiancheng.serversentinel.data

enum class CheckMode { STATUS, TCP }

data class MonitorConfig(val id: String, val name: String, val host: String, val port: Int, val enabled: Boolean, val checkMode: CheckMode = CheckMode.STATUS) {
    fun validate(): String? = when {
        host.trim().isEmpty() -> "服务器地址不能为空"
        port !in 1..65535 -> "端口范围是 1 到 65535"
        else -> null
    }
}
