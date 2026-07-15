# TCP Port Monitor Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let every server select Java status/MOTD detection or TCP port-connect detection.

**Architecture:** Persist `CheckMode` with `MonitorConfig`, treating missing legacy values as `STATUS`. One dispatcher selects the existing status probe or a five-second TCP connect probe; manual and WorkManager checks use the dispatcher.

**Tech Stack:** Kotlin, Jetpack Compose, DataStore Preferences, WorkManager, JUnit 4.

## Global Constraints

- `STATUS` is the default for new and legacy configurations.
- TCP is online only after a connection to the configured host and port within five seconds.
- No server address is bundled in the app.

---

### Task 1: Persist check mode

**Files:**
- Modify: `app/src/main/java/com/xiancheng/serversentinel/data/MonitorConfig.kt`
- Modify: `app/src/main/java/com/xiancheng/serversentinel/data/MonitorStore.kt`
- Test: `app/src/test/java/com/xiancheng/serversentinel/data/MonitorConfigTest.kt`

**Interfaces:** Produces `enum class CheckMode { STATUS, TCP }` and `MonitorConfig.checkMode: CheckMode = CheckMode.STATUS`. JSON writes `checkMode` and reads missing or invalid values as `STATUS`.

- [ ] **Step 1: Write failing tests**

```kotlin
@Test fun `new configuration uses status mode`() {
    assertEquals(CheckMode.STATUS, MonitorConfig("a", "A", "host", 25565, true).checkMode)
}
```

- [ ] **Step 2: Verify red**

Run `./gradlew.bat :app:testDebugUnitTest --tests com.xiancheng.serversentinel.data.MonitorConfigTest --no-parallel`.

Expected: compilation fails because `CheckMode` and `checkMode` do not exist.

- [ ] **Step 3: Implement minimal persistence**

```kotlin
enum class CheckMode { STATUS, TCP }
data class MonitorConfig(..., val checkMode: CheckMode = CheckMode.STATUS)
```

Save `checkMode.name`. Parse with `runCatching { CheckMode.valueOf(json.optString("checkMode", "STATUS")) }.getOrDefault(CheckMode.STATUS)`.

- [ ] **Step 4: Verify green and commit**

Run the Step 2 command; then commit model, store and test with message `Add per-server check mode`.

### Task 2: Dispatch TCP checks

**Files:**
- Modify: `app/src/main/java/com/xiancheng/serversentinel/MainActivity.kt`
- Test: `app/src/test/java/com/xiancheng/serversentinel/MonitorProbeTest.kt`

**Interfaces:** Consumes `MonitorConfig.checkMode`. Produces `ServerProbe.check(config: MonitorConfig): ServerStatus`.

- [ ] **Step 1: Write a failing mode-dispatch test**

```kotlin
@Test fun `tcp mode is selected by the server probe`() {
    assertEquals(CheckMode.TCP, ServerProbe.modeFor(MonitorConfig("a", "A", "host", 25565, true, CheckMode.TCP)))
}
```

- [ ] **Step 2: Verify red**

Run `./gradlew.bat :app:testDebugUnitTest --tests com.xiancheng.serversentinel.MonitorProbeTest --no-parallel`.

Expected: compilation fails because `ServerProbe` does not exist.

- [ ] **Step 3: Implement and replace callers**

`ServerProbe.check` switches `STATUS` to `MinecraftPing.ping` and `TCP` to `TcpPortProbe.ping`. `TcpPortProbe` calls `Socket().connect(InetSocketAddress(host, port), 5000)`, returning `端口可连接` and latency on success. Use `ServerProbe.check` in manual and worker loops.

- [ ] **Step 4: Verify green and commit**

Run the Step 2 command; commit source and test with message `Add TCP port monitoring`.

### Task 3: Add settings control and package

**Files:**
- Modify: `app/src/main/java/com/xiancheng/serversentinel/MainActivity.kt`
- Modify: `app/build.gradle.kts`

**Interfaces:** Settings provides a per-server dropdown that copies the draft item with its selected `checkMode`.

- [ ] **Step 1: Add selector**

Below the port field, add a dropdown displaying `状态/MOTD 检测` or `TCP 端口检测（适合关闭 enable-status）`.

- [ ] **Step 2: Bump version**

Set `versionCode = 6` and `versionName = "1.4.0"`.

- [ ] **Step 3: Verify, commit and push**

Run `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug --no-parallel`; verify `BUILD SUCCESSFUL`; commit with message `Expose monitoring mode in settings`; then `git push`.
