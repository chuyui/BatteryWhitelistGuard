package com.whitelistguard.core

import de.robv.android.xposed.XposedBridge

/**
 * 在 system_server 进程里记录拦截统计。
 *
 * 由于 system_server 无法直接写模块的 SharedPreferences，
 * 这里先写到一个内存里的计数器，再通过文件广播给 UI 进程。
 *
 * 简化版：直接用 XposedBridge.log + 写一个 /data/local/tmp/whitelist_guard_stats.json
 * 后续 UI 进程读取这个文件展示统计。
 */
object Stats {

    private val lock = Any()
    private var totalBlocked = 0
    private var totalBackgroundBlocked = 0
    private val blockDetail = mutableMapOf<String, Int>()
    private val hourlyBuckets = LongArray(24) { 0L }

    fun recordWhitelistBlocked(packageName: String) {
        synchronized(lock) {
            totalBlocked++
            blockDetail[packageName] = (blockDetail[packageName] ?: 0) + 1
            val hour = (System.currentTimeMillis() / 3600000L % 24).toInt()
            hourlyBuckets[hour]++
        }
        XposedBridge.log("[WhitelistGuard] BLOCKED whitelist add: $packageName (total=$totalBlocked)")
    }

    fun recordBackgroundBlocked(packageName: String) {
        synchronized(lock) {
            totalBackgroundBlocked++
        }
        XposedBridge.log("[WhitelistGuard] BLOCKED background start: $packageName (total=$totalBackgroundBlocked)")
    }

    fun snapshot(): StatsSnapshot = synchronized(lock) {
        StatsSnapshot(
            totalBlocked = totalBlocked,
            totalBackgroundBlocked = totalBackgroundBlocked,
            blockDetail = blockDetail.toMap(),
            hourlyBuckets = hourlyBuckets.copyOf()
        )
    }
}

data class StatsSnapshot(
    val totalBlocked: Int,
    val totalBackgroundBlocked: Int,
    val blockDetail: Map<String, Int>,
    val hourlyBuckets: LongArray
)
