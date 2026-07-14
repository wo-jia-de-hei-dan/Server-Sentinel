package com.xiancheng.serversentinel

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.pm.PackageManager
import android.os.Bundle
import android.net.Uri
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit
import java.util.UUID
import com.xiancheng.serversentinel.data.MonitorConfig
import com.xiancheng.serversentinel.data.MonitorStore
import com.xiancheng.serversentinel.data.LastCheckSnapshot
import com.xiancheng.serversentinel.data.LastServerState
import com.xiancheng.serversentinel.data.LastCheckTimeFormatter
import com.xiancheng.serversentinel.data.DashboardLastCheck
import com.xiancheng.serversentinel.data.MonitorLogEntry
import com.xiancheng.serversentinel.data.MonitorLogServer

private const val CHANNEL_ID = "server_outages"
data class ServerTarget(val name: String, val host: String, val port: Int)
data class ServerStatus(val target: ServerTarget, val online: Boolean, val detail: String, val players: String = "—", val motd: String = "", val latency: Long? = null)

private val GlassFill = Color(0xA6122238)
private val GlassBorder = Color(0x4D9BE8FF)
private val GlassButtonFill = Color(0x7A1D3B5B)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        createChannel(this); MonitorScheduler.schedule(this)
        setContent {
            var showLaunch by rememberSaveable { mutableStateOf(true) }
            LaunchedEffect(Unit) { delay(900); showLaunch = false }
            if (showLaunch) LaunchScreen() else SentinelApp(MonitorStore(applicationContext))
        }
    }
}

@Composable private fun LaunchScreen() {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val artwork = if (maxWidth > maxHeight) R.drawable.launch_landscape else R.drawable.launch_portrait
        Image(painterResource(artwork), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Color(0x3300142E)))
        Text(
            "Server Sentinel",
            modifier = Modifier.align(Alignment.Center).safeDrawingPadding(),
            color = Color.White,
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Black,
        )
    }
}

@Composable private fun GlassCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = GlassFill),
        border = BorderStroke(1.dp, GlassBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(22.dp),
        content = content
    )
}

private fun Modifier.glassIconButton(): Modifier = background(GlassButtonFill, CircleShape).border(1.dp, GlassBorder, CircleShape)

@Composable private fun SentinelApp(store: MonitorStore) {
    val context=LocalContext.current; val scope = rememberCoroutineScope(); var checking by remember { mutableStateOf(false) }; var settings by remember { mutableStateOf(false) }; var onboardingComplete by remember { mutableStateOf(false) }; var onboardingSetup by remember { mutableStateOf(false) }; var menuExpanded by remember { mutableStateOf(false) }
    val dashboardArtwork = if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE) R.drawable.dashboard_background else R.drawable.dashboard_portrait
    var configs by remember { mutableStateOf(emptyList<MonitorConfig>()) }; var realtime by remember { mutableStateOf(false) }
    var statuses by remember { mutableStateOf(emptyList<ServerStatus>()) }; var lastCheck by remember { mutableStateOf("尚未检测") }; var lastCheckMillis by remember { mutableLongStateOf(0L) }; var logs by remember { mutableStateOf(emptyList<MonitorLogEntry>()) }; var logsPage by remember { mutableStateOf(false) }
    val latestLog by store.latestLogFlow().collectAsState(initial = null)
    LaunchedEffect(Unit) { configs = store.monitors(); realtime = store.realtimeEnabled(); onboardingComplete=store.onboardingComplete(); logs=store.logs(); val snapshot=store.lastCheck(); lastCheckMillis=DashboardLastCheck.timestamp(snapshot, logs); lastCheck=LastCheckTimeFormatter.format(lastCheckMillis); statuses = configs.map { config -> snapshot?.servers?.firstOrNull { it.id==config.id }?.let { state -> ServerStatus(ServerTarget(config.name,config.host,config.port),state.online,state.detail,state.players,state.motd,state.latency) } ?: ServerStatus(ServerTarget(config.name, config.host, config.port), false, "等待首次检测") } }
    LaunchedEffect(latestLog) { latestLog?.let { entry -> lastCheckMillis=entry.checkedAtMillis; lastCheck=LastCheckTimeFormatter.format(entry.checkedAtMillis) } }
    LaunchedEffect(lastCheckMillis) { while(lastCheckMillis > 0L) { lastCheck=LastCheckTimeFormatter.format(lastCheckMillis); delay(30_000) } }
    MaterialTheme(colorScheme = darkColorScheme(background = Color(0xFF0B0E14), surface = Color(0xFF151A24), primary = Color(0xFF61D6A6), error = Color(0xFFFF7D74))) {
        Surface(Modifier.fillMaxSize(), color = Color.Transparent, contentColor = Color(0xFFF3F7FF)) { if(!onboardingComplete && !onboardingSetup) WelcomePage(onOpenSettings = { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))) }, onConfigure = { if(android.os.Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) ActivityCompat.requestPermissions(context as MainActivity, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 7); onboardingSetup=true }) else if(settings || onboardingSetup) Box(Modifier.fillMaxSize().background(Color(0xFF0B0E14))) { SettingsPage(configs, realtime, onBack = { if(onboardingSetup) onboardingSetup=false else settings=false }, onSave = { updated -> scope.launch { store.save(updated); configs=updated; statuses=updated.map { ServerStatus(ServerTarget(it.name,it.host,it.port),false,"等待首次检测") }; if(onboardingSetup) { store.setOnboardingComplete(); onboardingComplete=true; onboardingSetup=false }; settings=false } }, onRealtime = { enabled -> scope.launch { store.setRealtime(enabled); realtime=enabled; if(enabled) showOngoing(context, "正在监控 ${configs.count { it.enabled }} 台服务器") else NotificationManagerCompat.from(context).cancel(88) } }, onTest = { showTestNotification(context) }) } else if(logsPage) Box(Modifier.fillMaxSize().background(Color(0xFF0B0E14))) { LogsPage(logs, onBack = { logsPage=false }) } else Box(Modifier.fillMaxSize()) { Image(painterResource(dashboardArtwork), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop); Box(Modifier.fillMaxSize().background(Color(0x570B0E14))); Column(Modifier.safeDrawingPadding().padding(start=20.dp, top=20.dp, end=38.dp, bottom=20.dp).verticalScroll(rememberScrollState()).fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically) { Text("Server Sentinel", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, modifier=Modifier.weight(1f)); Box { IconButton(modifier=Modifier.size(42.dp).glassIconButton(), onClick={menuExpanded=true}) { Icon(Icons.Default.MoreVert, "更多") }; DropdownMenu(expanded=menuExpanded, onDismissRequest={menuExpanded=false}) { DropdownMenuItem(text={Text("检测日志")}, onClick={ scope.launch { logs=store.logs(); logsPage=true; menuExpanded=false } }); DropdownMenuItem(text={Text("服务器设置")}, onClick={ settings=true; menuExpanded=false }) } } }
            Text("本地 Minecraft 服务器监控", color = Color(0xFF98A2B3)); Spacer(Modifier.height(20.dp))
            val online = statuses.count { it.online }
            GlassCard { Row(Modifier.padding(18.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text("服务总览", fontWeight = FontWeight.SemiBold); Text("$online / ${statuses.size} 台在线", color = if (online == statuses.size) Color(0xFF61D6A6) else Color(0xFFFFB86B)) }
                FilledTonalIconButton(modifier=Modifier.size(48.dp).glassIconButton(), colors=IconButtonDefaults.filledTonalIconButtonColors(containerColor=Color.Transparent), enabled = !checking, onClick = { scope.launch { checking = true; val checked = withContext(Dispatchers.IO) { configs.filter { it.enabled }.map { MinecraftPing.ping(ServerTarget(it.name,it.host,it.port)) } }; statuses = checked; lastCheckMillis = System.currentTimeMillis(); lastCheck = LastCheckTimeFormatter.format(lastCheckMillis); store.saveLastCheck(LastCheckSnapshot(lastCheckMillis, checked.mapIndexed { index,status -> LastServerState(configs.filter { it.enabled }[index].id,status.online,status.detail,status.players,status.motd,status.latency) })); store.appendLog(MonitorLogEntry(lastCheckMillis, "手动", checked.map { status -> MonitorLogServer(status.target.name,status.online,status.detail,status.latency) })); if(realtime) showOngoing(context, "正在监控 ${configs.count { it.enabled }} 台服务器 · ${checked.count { it.online }} 台在线"); checking = false } }) { Icon(Icons.Default.Refresh, "立即检测") }
            } }
            Spacer(Modifier.height(10.dp)); Text("上次检测：$lastCheck", style = MaterialTheme.typography.labelMedium, color = Color(0xFF98A2B3)); Spacer(Modifier.height(12.dp))
            statuses.forEach { StatusCard(it) }
            Spacer(Modifier.weight(1f)); Text("每 15 分钟在本机检测一次 · 连续 2 次失败后告警", style = MaterialTheme.typography.labelSmall, color = Color(0xFF778195))
        } } }
    }
}

@Composable private fun WelcomePage(onOpenSettings: () -> Unit, onConfigure: () -> Unit) {
    Column(Modifier.safeDrawingPadding().padding(start=20.dp, top=36.dp, end=38.dp, bottom=20.dp).verticalScroll(rememberScrollState())) {
        Text("欢迎使用", style=MaterialTheme.typography.displaySmall, fontWeight=FontWeight.Black)
        Text("Server Sentinel", style=MaterialTheme.typography.headlineMedium, color=Color(0xFF61D6A6), fontWeight=FontWeight.Bold)
        Spacer(Modifier.height(24.dp)); GlassCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(20.dp)) {
            Text("本地服务器监控", style=MaterialTheme.typography.titleLarge, fontWeight=FontWeight.Bold)
            Spacer(Modifier.height(8.dp)); Text("检测在手机本机进行。服务器掉线后会通过 Android 通知提醒你。", color=Color(0xFFB9C2D0))
        } }
        Spacer(Modifier.height(14.dp)); GlassCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(20.dp)) {
            Text("后台保活", style=MaterialTheme.typography.titleLarge, fontWeight=FontWeight.Bold)
            Spacer(Modifier.height(8.dp)); Text("请在系统应用设置中将电池使用设为“不受限制”，并在近期任务中锁定本应用，避免系统结束后台检测。", color=Color(0xFFB9C2D0))
            Spacer(Modifier.height(16.dp)); OutlinedButton(onClick=onOpenSettings, modifier=Modifier.fillMaxWidth()) { Text("打开应用设置") }
        } }
        Spacer(Modifier.height(24.dp)); Button(onClick=onConfigure, modifier=Modifier.fillMaxWidth()) { Text("开始配置服务器") }
    }
}

@Composable private fun SettingsPage(configs: List<MonitorConfig>, realtime: Boolean, onBack: () -> Unit, onSave: (List<MonitorConfig>) -> Unit, onRealtime: (Boolean) -> Unit, onTest: () -> Unit) {
    var drafts by remember(configs) { mutableStateOf(configs) }; var enabled by remember { mutableStateOf(realtime) }
    Column(Modifier.safeDrawingPadding().padding(start=20.dp, top=20.dp, end=38.dp, bottom=20.dp).verticalScroll(rememberScrollState())) {
        Row(verticalAlignment=Alignment.CenterVertically) { Text("服务器设置", style=MaterialTheme.typography.headlineMedium, fontWeight=FontWeight.Bold, modifier=Modifier.weight(1f)); TextButton(onClick=onBack) { Text("返回") } }
        Text("通知", fontWeight=FontWeight.Bold); Row(verticalAlignment=Alignment.CenterVertically, modifier=Modifier.fillMaxWidth()) { Column(Modifier.weight(1f)) { Text("实时监控通知"); Text("显示当前监控状态", style=MaterialTheme.typography.bodySmall) }; Switch(checked=enabled,onCheckedChange={ enabled=it; onRealtime(it) }) }; Button(onClick=onTest, modifier=Modifier.fillMaxWidth()) { Text("测试通知") }
        Spacer(Modifier.height(20.dp)); Row(verticalAlignment=Alignment.CenterVertically, modifier=Modifier.fillMaxWidth()) { Text("服务器", fontWeight=FontWeight.Bold, modifier=Modifier.weight(1f)); TextButton(onClick={ drafts=drafts + MonitorConfig(UUID.randomUUID().toString(), "Minecraft 服务器", "", 25565, true) }) { Text("添加服务器") } }
        if(drafts.isEmpty()) Text("还没有服务器，点击“添加服务器”开始配置。", color=Color(0xFF98A2B3), modifier=Modifier.padding(vertical=20.dp))
        drafts.forEachIndexed { index, item -> GlassCard(Modifier.fillMaxWidth().padding(top=10.dp)) { Column(Modifier.padding(14.dp)) { var name by remember(item.id) { mutableStateOf(item.name) }; var host by remember(item.id) { mutableStateOf(item.host) }; var port by remember(item.id) { mutableStateOf(item.port.toString()) }; Row(verticalAlignment=Alignment.CenterVertically) { Text("启用", modifier=Modifier.weight(1f)); Switch(checked=item.enabled,onCheckedChange={ checked -> drafts=drafts.toMutableList().also { it[index]=item.copy(enabled=checked) } }); IconButton(onClick={ drafts=drafts.filterNot { it.id==item.id } }) { Icon(Icons.Default.Delete, "删除服务器") } }; OutlinedTextField(name,{ name=it; drafts=drafts.toMutableList().also { list->list[index]=list[index].copy(name=it) } },label={Text("名称")},modifier=Modifier.fillMaxWidth()); OutlinedTextField(host,{ host=it; drafts=drafts.toMutableList().also { list->list[index]=list[index].copy(host=it) } },label={Text("地址")},placeholder={Text("例如 play.example.com")},modifier=Modifier.fillMaxWidth()); OutlinedTextField(port,{ port=it; it.toIntOrNull()?.let { value-> drafts=drafts.toMutableList().also { list->list[index]=list[index].copy(port=value) } } },label={Text("端口")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),modifier=Modifier.fillMaxWidth()) } }
        }; Spacer(Modifier.height(18.dp)); Button(onClick={onSave(drafts)},enabled=drafts.isNotEmpty() && drafts.all { it.validate()==null },modifier=Modifier.fillMaxWidth()) { Text("保存服务器配置") }; Text("请在系统中允许通知和后台运行。", style=MaterialTheme.typography.bodySmall, color=Color(0xFF98A2B3))
    }
}

@Composable private fun LogsPage(logs: List<MonitorLogEntry>, onBack: () -> Unit) {
    Column(Modifier.safeDrawingPadding().padding(start=20.dp, top=20.dp, end=38.dp, bottom=20.dp).verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically) { Text("检测日志", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); TextButton(onClick = onBack) { Text("返回") } }
        Text("最近 ${logs.size} / 100 次检测 · 仅保存在本机", color = Color(0xFF98A2B3)); Spacer(Modifier.height(14.dp))
        if (logs.isEmpty()) Text("暂无检测记录", modifier = Modifier.fillMaxWidth().padding(top = 48.dp), color = Color(0xFF98A2B3), style = MaterialTheme.typography.bodyLarge)
        logs.forEach { entry ->
            GlassCard(Modifier.fillMaxWidth().padding(vertical = 6.dp)) { Column(Modifier.padding(16.dp)) {
                val online = entry.servers.count { it.online }
                Text("${entry.source}检测 · ${LastCheckTimeFormatter.format(entry.checkedAtMillis)}", fontWeight = FontWeight.Bold)
                Text("$online / ${entry.servers.size} 台在线", color = if (online == entry.servers.size) Color(0xFF61D6A6) else Color(0xFFFFB86B), style = MaterialTheme.typography.bodySmall)
                entry.servers.forEach { server ->
                    val color = if (server.online) Color(0xFF61D6A6) else Color(0xFFFF7D74)
                    Spacer(Modifier.height(10.dp)); Text("${if (server.online) "●" else "●"} ${server.name} · ${if (server.online) "在线" else "离线"}", color = color, fontWeight = FontWeight.SemiBold)
                    Text(if (server.online) "${server.detail} · ${server.latency ?: 0} ms" else server.detail, color = Color(0xFFB9C2D0), style = MaterialTheme.typography.bodySmall)
                }
            } }
        }
    }
}

@Composable private fun StatusCard(status: ServerStatus) { val accent = if (status.online) Color(0xFF61D6A6) else Color(0xFFFF7D74)
    GlassCard(Modifier.fillMaxWidth().padding(vertical = 6.dp)) { Column(Modifier.padding(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(10.dp).background(accent, RoundedCornerShape(8.dp))); Spacer(Modifier.width(9.dp)); Text(status.target.name, fontWeight = FontWeight.Bold); Spacer(Modifier.weight(1f)); Text(if(status.online) "在线" else "离线", color = accent, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(10.dp)); Text("${status.target.host}:${status.target.port}", color = Color(0xFFB9C2D0)); Text(status.detail, color = Color(0xFF98A2B3), style = MaterialTheme.typography.bodySmall)
        if(status.online) { Spacer(Modifier.height(8.dp)); Text("${status.players}  ·  ${status.latency ?: 0} ms", color = Color(0xFF61D6A6)); if(status.motd.isNotBlank()) Text(status.motd, style = MaterialTheme.typography.bodySmall, color = Color(0xFF98A2B3), maxLines = 2) }
    } }
}

object MinecraftPing {
    fun ping(target: ServerTarget): ServerStatus = try {
        Socket().use { socket -> socket.connect(InetSocketAddress(target.host, target.port), 5000); socket.soTimeout = 5000
            val out = DataOutputStream(socket.getOutputStream()); val input = DataInputStream(socket.getInputStream())
            val handshake = java.io.ByteArrayOutputStream().apply { writeVarInt(0); writeVarInt(760); writeString(target.host); writeShort(target.port); writeVarInt(1) }.toByteArray()
            out.writePacket(handshake); out.writePacket(byteArrayOf(0)); out.flush()
            readVarInt(input); readVarInt(input); val json = input.readString(); val start = System.nanoTime(); out.writePacket(java.io.ByteArrayOutputStream().apply { writeVarInt(1); writeLong(start) }.toByteArray()); out.flush(); readVarInt(input); readVarInt(input); input.readLong()
            val root = JSONObject(json); val players = root.optJSONObject("players"); val description = root.opt("description")?.toString()?.replace("§.", "") ?: ""
            ServerStatus(target, true, root.optJSONObject("version")?.optString("name") ?: "Java Server", "${players?.optInt("online", 0) ?: 0}/${players?.optInt("max", 0) ?: 0} 玩家", description, TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-start))
        }
    } catch (e: Exception) { ServerStatus(target, false, when(e) { is java.net.SocketTimeoutException -> "连接超时"; is java.net.ConnectException -> "连接被拒绝"; else -> "无法连接" }) }
    private fun java.io.ByteArrayOutputStream.writeVarInt(value: Int) { var v=value; do { var b=v and 0x7F; v = v ushr 7; if(v != 0) b = b or 0x80; write(b) } while(v != 0) }
    private fun java.io.ByteArrayOutputStream.writeString(text: String) { val b=text.toByteArray(StandardCharsets.UTF_8); writeVarInt(b.size); write(b) }
    private fun java.io.ByteArrayOutputStream.writeShort(value: Int) { write(value ushr 8); write(value and 0xFF) }
    private fun java.io.ByteArrayOutputStream.writeLong(value: Long) { java.nio.ByteBuffer.allocate(8).putLong(value).array().also { write(it) } }
    private fun DataOutputStream.writeVarInt(value: Int) { var v=value; do { var b=v and 0x7F; v=v ushr 7; if(v != 0) b=b or 0x80; writeByte(b) } while(v != 0) }
    private fun DataOutputStream.writePacket(data: ByteArray) { writeVarInt(data.size); write(data) }
    private fun readVarInt(input: DataInputStream): Int { var result=0; var shift=0; while(true) { val b=input.readUnsignedByte(); result=result or ((b and 0x7F) shl shift); if((b and 0x80)==0) return result; shift += 7; if(shift>35) throw IllegalArgumentException("VarInt too long") } }
    private fun DataInputStream.readString(): String { val len=readVarInt(this); val b=ByteArray(len); readFully(b); return String(b, StandardCharsets.UTF_8) }
}

class MonitorWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) { val prefs=applicationContext.getSharedPreferences("monitor", Context.MODE_PRIVATE); val store=MonitorStore(applicationContext); val items=store.monitors().filter { it.enabled }; val checked=items.map { MinecraftPing.ping(ServerTarget(it.name,it.host,it.port)) }; items.forEachIndexed { index, item -> val target=ServerTarget(item.name,item.host,item.port); val status=checked[index]; val failures=if(status.online) 0 else prefs.getInt("failures_$index", 0)+1; val wasOffline=prefs.getBoolean("offline_$index", false); if(!status.online && failures >= 2 && !wasOffline) notify(applicationContext, index, "服务器掉线", "${target.name} · ${status.detail}"); if(status.online && wasOffline) notify(applicationContext, index, "服务器已恢复", "${target.name} · ${status.latency} ms"); prefs.edit().putInt("failures_$index", failures).putBoolean("offline_$index", !status.online && failures >= 2).apply() }; val checkedAt=System.currentTimeMillis(); store.saveLastCheck(LastCheckSnapshot(checkedAt, checked.mapIndexed { index,status -> LastServerState(items[index].id,status.online,status.detail,status.players,status.motd,status.latency) })); store.appendLog(MonitorLogEntry(checkedAt, "自动", checked.map { status -> MonitorLogServer(status.target.name,status.online,status.detail,status.latency) })); if(store.realtimeEnabled()) showOngoing(applicationContext, "正在监控 ${items.size} 台服务器"); Result.success() }
}
object MonitorScheduler { fun schedule(context: Context) { val request=PeriodicWorkRequestBuilder<MonitorWorker>(15, TimeUnit.MINUTES).setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).build(); WorkManager.getInstance(context).enqueueUniquePeriodicWork("minecraft-monitor", ExistingPeriodicWorkPolicy.UPDATE, request) } }
private fun createChannel(context: Context) { val manager=context.getSystemService(NotificationManager::class.java); manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, "服务器告警", NotificationManager.IMPORTANCE_HIGH).apply { enableVibration(true) }) }
private fun notify(context: Context, id: Int, title: String, text: String) { if(android.os.Build.VERSION.SDK_INT < 33 || ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED) NotificationManagerCompat.from(context).notify(100+id, NotificationCompat.Builder(context, CHANNEL_ID).setSmallIcon(android.R.drawable.stat_notify_error).setContentTitle(title).setContentText(text).setPriority(NotificationCompat.PRIORITY_HIGH).setAutoCancel(true).build()) }
private fun showTestNotification(context: Context) = notify(context, 77, "Server Sentinel 测试通知", "通知通道、声音和震动工作正常")
private fun showOngoing(context: Context, text: String) { if(android.os.Build.VERSION.SDK_INT < 33 || ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED) NotificationManagerCompat.from(context).notify(88, NotificationCompat.Builder(context, CHANNEL_ID).setSmallIcon(android.R.drawable.stat_notify_sync).setContentTitle("Server Sentinel 正在监控").setContentText(text).setOngoing(true).setOnlyAlertOnce(true).setPriority(NotificationCompat.PRIORITY_LOW).build()) }
