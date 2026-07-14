# 可分享版首次引导 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 发布不含个人服务器数据的 1.3.0 分享版，并提供首次启动引导与多服务器管理。

**Architecture:** MonitorStore 以空列表为默认值并保存 `onboarding_complete`；MainActivity 根据该状态显示欢迎页、服务器管理页或主页。服务器管理改为可添加、删除的可变草稿列表，顶部使用单一三点菜单。

**Tech Stack:** Kotlin、Jetpack Compose Material 3、DataStore Preferences、Android Settings Intent、JUnit 4。

## Global Constraints

- 不预填 IP、端口、服务器名称或 MOTD。
- 初次启动必须先显示欢迎页，再进入服务器管理。
- 支持任意数量服务器，至少一台才可保存。
- 全部页面保留右侧 38dp 安全边距。
- `versionName` 为 `1.3.0`，`versionCode` 为 `4`。

---

### Task 1: 空默认配置与首次启动状态

**Files:**
- Modify: `app/src/main/java/com/xiancheng/serversentinel/data/MonitorStore.kt`
- Modify: `app/src/test/java/com/xiancheng/serversentinel/data/MonitorConfigTest.kt`

- [ ] **Step 1: Write a failing test for an empty shared-app default**

```kotlin
@Test fun `shared app has no bundled server defaults`() {
    assertTrue(MonitorStore.defaults.isEmpty())
}
```

- [ ] **Step 2: Run it and verify failure**

Run: `./gradlew.bat :app:testDebugUnitTest --tests 'com.xiancheng.serversentinel.data.MonitorConfigTest' --no-parallel`

Expected: failure because defaults contain bundled servers.

- [ ] **Step 3: Make defaults empty and add onboarding accessors**

```kotlin
private val onboardingKey = stringPreferencesKey("onboarding_complete")
suspend fun onboardingComplete(): Boolean = context.monitorDataStore.data.first()[onboardingKey] == "true"
suspend fun setOnboardingComplete() { context.monitorDataStore.edit { it[onboardingKey] = "true" } }
companion object { val defaults = emptyList<MonitorConfig>() }
```

- [ ] **Step 4: Run the unit test and verify pass**

Run: `./gradlew.bat :app:testDebugUnitTest --tests 'com.xiancheng.serversentinel.data.MonitorConfigTest' --no-parallel`

Expected: `BUILD SUCCESSFUL`.

### Task 2: Welcome and multi-server configuration

**Files:**
- Modify: `app/src/main/java/com/xiancheng/serversentinel/MainActivity.kt`

**Interfaces:**
- Consumes: `MonitorStore.onboardingComplete`, `setOnboardingComplete`, `save`.
- Produces: `WelcomePage` and extended `SettingsPage`.

- [ ] **Step 1: Add welcome flow states**

```kotlin
var onboardingComplete by remember { mutableStateOf(false) }
LaunchedEffect(Unit) { onboardingComplete = store.onboardingComplete() }
```

Render `WelcomePage` while false; its primary action opens `Settings.ACTION_APPLICATION_DETAILS_SETTINGS` and its continue action routes to configuration.

- [ ] **Step 2: Add server creation and removal**

```kotlin
Button(onClick = { drafts = drafts + MonitorConfig(UUID.randomUUID().toString(), "Minecraft 服务器", "", 25565, true) }) { Text("添加服务器") }
IconButton(onClick = { drafts = drafts - item }) { Icon(Icons.Default.Delete, "删除服务器") }
```

Disable save when `drafts` is empty or a server fails `validate()`.

- [ ] **Step 3: Mark onboarding complete only after a valid save**

```kotlin
store.save(updated)
store.setOnboardingComplete()
onboardingComplete = true
```

### Task 3: Safe navigation and share version

**Files:**
- Modify: `app/src/main/java/com/xiancheng/serversentinel/MainActivity.kt`
- Modify: `app/build.gradle.kts`

- [ ] **Step 1: Replace two header buttons with a single overflow menu**

Use `IconButton` plus `DropdownMenu` containing “检测日志” and “服务器设置”; retain the existing right-side 38dp content padding.

- [ ] **Step 2: Bump APK version**

```kotlin
versionCode = 4
versionName = "1.3.0"
```

- [ ] **Step 3: Build, verify and copy APK**

Run: `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug --no-parallel`

Run: `aapt dump badging app/build/outputs/apk/debug/app-debug.apk`

Expected: `versionCode='4' versionName='1.3.0'`.

Run: `Copy-Item app/build/outputs/apk/debug/app-debug.apk C:\Users\Xiancheng\Desktop\Server-Sentinel-1.3.0-分享版.apk -Force`
