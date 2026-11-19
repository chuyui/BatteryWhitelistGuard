package com.whitelistguard.ui

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whitelistguard.core.Constants

class MainActivity : ComponentActivity() {

    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences(Constants.PREFS_NAME, Context.MODE_WORLD_READABLE)

        setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                WhitelistGuardScreen(prefs)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhitelistGuardScreen(prefs: SharedPreferences) {
    var enabled by remember { mutableStateOf(prefs.getBoolean(Constants.KEY_ENABLED, true)) }
    var blacklistMode by remember { mutableStateOf(prefs.getBoolean(Constants.KEY_BLACKLIST_MODE, true)) }
    var selectedPackages by remember {
        mutableStateOf(prefs.getStringSet(Constants.KEY_SELECTED_PACKAGES, emptySet())?.toSet() ?: emptySet())
    }

    // 统计数据（演示用，实际应从 system_server 读取）
    val totalBlocked = remember { mutableIntStateOf(634) }
    val totalBackgroundBlocked = remember { mutableIntStateOf(1723) }
    val interceptedEntries = remember { mutableIntStateOf(18) }

    fun savePrefs() {
        prefs.edit()
            .putBoolean(Constants.KEY_ENABLED, enabled)
            .putBoolean(Constants.KEY_BLACKLIST_MODE, blacklistMode)
            .putStringSet(Constants.KEY_SELECTED_PACKAGES, selectedPackages)
            .apply()
    }

    Scaffold(topBar = {
        Text(
            "白名单守卫",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 20.dp, top = 40.dp, bottom = 8.dp)
        )
    }) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFFF5F6F8))
                .padding(horizontal = 20.dp)
        ) {
            // 副标题
            item {
                Text(
                    "阻止 ColorOS 把应用塞进电池优化白名单",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            // 工作状态卡片
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFD4F5E0)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                if (enabled) "工作中" else "已暂停",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1B7A3E)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text("已接管 $interceptedEntries 个系统入口", fontSize = 14.sp, color = Color(0xFF2D6A4F))
                            Text("已拦截 ${totalBlocked.value} 次 · 涉及 ${selectedPackages.size.coerceAtLeast(7)} 个应用", fontSize = 14.sp, color = Color(0xFF2D6A4F))
                            Text("已阻断后台启动 ${totalBackgroundBlocked.value} 次", fontSize = 14.sp, color = Color(0xFF2D6A4F))
                        }
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF2ECC71),
                            modifier = Modifier.size(80.dp)
                        )
                    }
                }
            }

            // 模式说明
            item {
                Text(
                    if (blacklistMode) "黑名单模式：只拦截你勾选的应用，其余保持系统默认。"
                    else "白名单模式：未勾选的应用全部拦截，只放行勾选的。",
                    fontSize = 13.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            // 触发频率图
            item {
                Text("触发频率（近 24 小时）", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))
                FrequencyChart()
                Spacer(Modifier.height(6.dp))
                Text("近 24 小时触发 1697 次 · 累计 5282 次", fontSize = 12.sp, color = Color.Gray)
                Spacer(Modifier.height(20.dp))
            }

            // 拦截方式切换
            item {
                Text("拦截方式", fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 8.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    ModeButton(
                        text = "黑名单：勾选的拦截",
                        selected = blacklistMode,
                        modifier = Modifier.weight(1f)
                    ) {
                        blacklistMode = true
                        savePrefs()
                    }
                    Spacer(Modifier.width(12.dp))
                    ModeButton(
                        text = "白名单：未勾选的全拦",
                        selected = !blacklistMode,
                        modifier = Modifier.weight(1f)
                    ) {
                        blacklistMode = false
                        savePrefs()
                    }
                }
                Spacer(Modifier.height(20.dp))
            }

            // 批量操作
            item {
                Text("批量操作", fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 8.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    BatchButton("全部拦截", Modifier.weight(1f)) { /* TODO */ }
                    Spacer(Modifier.width(10.dp))
                    BatchButton("全部放行", Modifier.weight(1f)) { /* TODO */ }
                    Spacer(Modifier.width(10.dp))
                    BatchButton("立即清理", Modifier.weight(1f)) { /* TODO */ }
                }
                Spacer(Modifier.height(20.dp))
            }

            // 开关
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("启用模块", fontSize = 16.sp, modifier = Modifier.weight(1f))
                    Switch(checked = enabled, onCheckedChange = {
                        enabled = it
                        savePrefs()
                    })
                }
                Spacer(Modifier.height(20.dp))
            }

            // 应用列表（演示数据）
            item {
                Text("应用列表", fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 8.dp))
            }

            val demoApps = listOf(
                "com.tencent.mm" to "微信",
                "com.taobao.taobao" to "淘宝",
                "com.ss.android.ugc.aweme" to "抖音",
                "com.sina.weibo" to "微博",
                "com.xunmeng.pinduoduo" to "拼多多",
                "com.netease.cloudmusic" to "网易云音乐",
                "com.qiyi.video" to "爱奇艺"
            )

            items(demoApps) { (pkg, name) ->
                AppRow(
                    name = name,
                    packageName = pkg,
                    checked = selectedPackages.contains(pkg),
                    onCheckedChange = { checked ->
                        selectedPackages = if (checked) selectedPackages + pkg else selectedPackages - pkg
                        savePrefs()
                    }
                )
            }
        }
    }
}

@Composable
fun FrequencyChart() {
    val bars = listOf(0.8f, 0.3f, 0.6f, 0.05f, 0.02f, 0.02f, 0.02f, 0.25f,
                      0.02f, 0.02f, 0.02f, 0.05f, 0.2f, 0.9f, 0.4f, 0.02f,
                      0.65f, 0.4f, 0.05f, 0.4f)
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("峰 281", fontSize = 12.sp, color = Color.Gray)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                bars.forEach { h ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 1.5.dp)
                            .height((h * 80).dp)
                            .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                            .background(Color(0xFF3B82F6))
                    )
                }
            }
        }
    }
}

@Composable
fun ModeButton(text: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) Color(0xFF2563EB) else Color(0xFFEEF0F4),
            contentColor = if (selected) Color.White else Color.Black
        )
    ) {
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun BatchButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEEF0F4), contentColor = Color.Black)
    ) {
        Text(text, fontSize = 13.sp)
    }
}

@Composable
fun AppRow(name: String, packageName: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFFE0E7FF)),
            contentAlignment = Alignment.Center
        ) {
            Text(name.take(1), fontWeight = FontWeight.Bold, color = Color(0xFF4F46E5))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(name, fontSize = 15.sp)
            Text(packageName, fontSize = 11.sp, color = Color.Gray)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
