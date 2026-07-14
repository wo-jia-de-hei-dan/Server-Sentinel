# 本地检测日志 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 Android APP 内保存并展示最近 100 次手动和自动服务器检测记录。

**Architecture:** 新建纯 Kotlin 日志模型负责 JSON 序列化和 100 条裁剪；MonitorStore 将日志保存到既有 DataStore。MainActivity 的手动检测和 MonitorWorker 的后台检测共享追加接口，Compose 日志页只读取本地数据。

**Tech Stack:** Kotlin、Jetpack Compose Material 3、DataStore Preferences、WorkManager、JUnit 4。

## Global Constraints

- 日志只保存在本机，不上传、不导出。
- 手动和 15 分钟后台检测必须记录到同一日志。
- 仅保留最近 100 条，按时间倒序显示。
- APK 完成后复制到 `C:\Users\Xiancheng\Desktop`。

---

### Task 1: 日志数据模型与保留上限

**Files:**
- Create: `app/src/main/java/com/xiancheng/serversentinel/data/MonitorLog.kt`
- Create: `app/src/test/java/com/xiancheng/serversentinel/data/MonitorLogTest.kt`

**Interfaces:**
- Produces: `MonitorLogEntry`, `MonitorLogServer`, `MonitorLogCodec.append(raw: String?, entry: MonitorLogEntry): List<MonitorLogEntry>`。

- [ ] **Step 1: Write the failing tests**

```kotlin
@Test fun `appending keeps newest 100 entries`() {
    val logs = (1..101).fold(emptyList<MonitorLogEntry>()) { current, index ->
        MonitorLogCodec.append(MonitorLogCodec.toJson(current), MonitorLogEntry(index.toLong(), "手动", emptyList()))
    }
    assertEquals(100, logs.size)
    assertEquals(101L, logs.first().checkedAtMillis)
}
```

- [ ] **Step 2: Run the test and verify it fails**

Run: `./gradlew.bat :app:testDebugUnitTest --tests 'com.xiancheng.serversentinel.data.MonitorLogTest' --no-parallel`

Expected: compilation failure because `MonitorLogCodec` does not exist.

- [ ] **Step 3: Implement the model and codec**

```kotlin
data class MonitorLogServer(val name: String, val online: Boolean, val detail: String, val latency: Long?)
data class MonitorLogEntry(val checkedAtMillis: Long, val source: String, val servers: List<MonitorLogServer>)
object MonitorLogCodec {
    fun append(raw: String?, entry: MonitorLogEntry): List<MonitorLogEntry> =
        (decode(raw) + entry).sortedByDescending { it.checkedAtMillis }.take(100)
}
```

- [ ] **Step 4: Run the test and verify it passes**

Run: `./gradlew.bat :app:testDebugUnitTest --tests 'com.xiancheng.serversentinel.data.MonitorLogTest' --no-parallel`

Expected: `BUILD SUCCESSFUL`.

### Task 2: 持久化并接入检测流程

**Files:**
- Modify: `app/src/main/java/com/xiancheng/serversentinel/data/MonitorStore.kt`
- Modify: `app/src/main/java/com/xiancheng/serversentinel/MainActivity.kt`

**Interfaces:**
- Consumes: `MonitorLogCodec.append`, `MonitorLogEntry`, `MonitorLogServer`。
- Produces: `MonitorStore.logs(): List<MonitorLogEntry>` and `MonitorStore.appendLog(entry: MonitorLogEntry)`。

- [ ] **Step 1: Add DataStore accessors**

```kotlin
suspend fun logs(): List<MonitorLogEntry> = MonitorLogCodec.decode(context.monitorDataStore.data.first()[logsKey])
suspend fun appendLog(entry: MonitorLogEntry) = context.monitorDataStore.edit { prefs ->
    prefs[logsKey] = MonitorLogCodec.toJson(MonitorLogCodec.append(prefs[logsKey], entry))
}
```

- [ ] **Step 2: Write log entries from both paths**

```kotlin
store.appendLog(MonitorLogEntry(System.currentTimeMillis(), "手动", checked.map { status ->
    MonitorLogServer(status.target.name, status.online, status.detail, status.latency)
}))
```

Use the same mapping in `MonitorWorker` with source `"自动"` immediately after saving the latest snapshot.

- [ ] **Step 3: Build and run all unit tests**

Run: `./gradlew.bat :app:testDebugUnitTest --no-parallel`

Expected: `BUILD SUCCESSFUL`.

### Task 3: Compose 日志页面

**Files:**
- Modify: `app/src/main/java/com/xiancheng/serversentinel/MainActivity.kt`

**Interfaces:**
- Consumes: `MonitorStore.logs()` and `MonitorLogEntry`.
- Produces: 首页日志入口和 `LogsPage`。

- [ ] **Step 1: Add the navigation state and entry**

```kotlin
var logsPage by remember { mutableStateOf(false) }
IconButton(onClick = { logsPage = true }) { Icon(Icons.Default.List, "检测日志") }
```

- [ ] **Step 2: Render logs in reverse chronological order**

```kotlin
@Composable private fun LogsPage(logs: List<MonitorLogEntry>, onBack: () -> Unit) {
    if (logs.isEmpty()) Text("暂无检测记录")
    logs.forEach { entry -> Text("${entry.source}检测 · ${formatTime(entry.checkedAtMillis)}") }
}
```

Render each server result below the entry, with green online/red offline state and latency or failure detail.

- [ ] **Step 3: Build debug APK and install-free verification**

Run: `./gradlew.bat :app:assembleDebug --no-parallel`

Expected: `BUILD SUCCESSFUL` and `app/build/outputs/apk/debug/app-debug.apk` exists.

### Task 4: 桌面交付

**Files:**
- Create: `C:\Users\Xiancheng\Desktop\Server-Sentinel-日志版.apk`

- [ ] **Step 1: Copy the verified APK**

Run: `Copy-Item app\build\outputs\apk\debug\app-debug.apk C:\Users\Xiancheng\Desktop\Server-Sentinel-日志版.apk -Force`

- [ ] **Step 2: Verify the artifact**

Run: `Get-FileHash C:\Users\Xiancheng\Desktop\Server-Sentinel-日志版.apk -Algorithm SHA256`

Expected: one SHA-256 hash is printed.
