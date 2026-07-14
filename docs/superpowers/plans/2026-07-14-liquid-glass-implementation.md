# 深色 Liquid Glass Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让 Server Sentinel 的监控、设置和日志页面呈现可读的深色毛玻璃界面。

**Architecture:** 在 MainActivity 中定义统一的玻璃色板和 `GlassCard` 容器；所有现有卡片复用容器，页面背景保持不变。仅改视觉层，不调整检测、通知与日志逻辑。

**Tech Stack:** Kotlin、Jetpack Compose Material 3、Compose UI graphics。

## Global Constraints

- 保留现有蓝色 Minecraft 夜景背景。
- 仅使用 Compose 透明色、描边、阴影和现有背景图；不引入 React 或 Web 依赖。
- 不做高耗电的实时折射或持续动画。
- 正文和状态颜色维持高对比度。

---

### Task 1: 玻璃容器

**Files:**
- Modify: `app/src/main/java/com/xiancheng/serversentinel/MainActivity.kt`

**Interfaces:**
- Produces: `GlassCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit)`。

- [ ] **Step 1: Define the reusable glass treatment**

```kotlin
private val GlassFill = Color(0x9B101D31)
private val GlassBorder = Color(0x4D9BE8FF)
@Composable private fun GlassCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = GlassFill),
        border = BorderStroke(1.dp, GlassBorder), shape = RoundedCornerShape(22.dp), content = content)
}
```

- [ ] **Step 2: Replace overview, status, settings and log cards with `GlassCard`**

Use `GlassCard` wherever the current UI calls `Card` for monitor content. Preserve all existing padding, data and click handlers.

- [ ] **Step 3: Build and run unit tests**

Run: `./gradlew.bat :app:testDebugUnitTest --no-parallel`

Expected: `BUILD SUCCESSFUL`.

### Task 2: Glass icon controls and verification

**Files:**
- Modify: `app/src/main/java/com/xiancheng/serversentinel/MainActivity.kt`

**Interfaces:**
- Consumes: `GlassFill`, `GlassBorder`.
- Produces: consistent glass-styled history, settings and refresh icon controls.

- [ ] **Step 1: Apply a translucent circular container to icon buttons**

```kotlin
Modifier.background(Color(0x751B3352), CircleShape).border(1.dp, GlassBorder, CircleShape)
```

Apply it to the history, settings and refresh buttons while keeping their existing `onClick` callbacks.

- [ ] **Step 2: Build the final APK**

Run: `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug --no-parallel`

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Copy and hash the APK**

Run: `Copy-Item app\build\outputs\apk\debug\app-debug.apk C:\Users\Xiancheng\Desktop\Server-Sentinel-Liquid-Glass.apk -Force; Get-FileHash C:\Users\Xiancheng\Desktop\Server-Sentinel-Liquid-Glass.apk -Algorithm SHA256`

Expected: one SHA-256 hash is printed.
