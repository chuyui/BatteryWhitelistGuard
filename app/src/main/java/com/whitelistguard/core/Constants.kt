package com.whitelistguard.core

/**
 * 全局常量。
 *
 * 注意：系统进程 (system_server) 和 UI 进程都引用同一份常量，
 * 包名和 Sp 文件名必须一致。
 */
object Constants {
    const val MODULE_PACKAGE = "com.whitelistguard"

    // SharedPreferences 文件名（MODE_WORLD_READABLE，供 system_server 读取）
    const val PREFS_NAME = "whitelist_guard_prefs"

    // ---- 配置 key ----
    /** true = 黑名单模式（只拦截勾选的应用），false = 白名单模式（未勾选的全拦） */
    const val KEY_BLACKLIST_MODE = "blacklist_mode"

    /** 被拦截/被放行的包名集合（Set<String>） */
    const val KEY_SELECTED_PACKAGES = "selected_packages"

    /** 模块总开关 */
    const val KEY_ENABLED = "enabled"

    // ---- 统计 key ----
    const val KEY_TOTAL_BLOCKED = "total_blocked"
    const val KEY_TOTAL_BACKGROUND_BLOCKED = "total_background_blocked"
    const val KEY_TOTAL_SESSIONS = "total_sessions"
    /** 最近 24 小时触发时间戳列表（LongArray，按小时桶聚合） */
    const val KEY_HOURLY_BUCKETS = "hourly_buckets"
    /** 拦截明细：包名 -> 次数 */
    const val KEY_BLOCK_DETAIL = "block_detail"
}
