package com.whitelistguard.core

import android.content.Context
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedHelpers

/**
 * system_server 进程里读取模块配置的桥。
 *
 * 使用 XSharedPreferences（MODE_WORLD_READABLE），由 UI 进程写入，
 * hook 进程在 system_server 里读取。
 *
 * 注意：Android 8.0+ 对 MODE_WORLD_READABLE 有限制，
 * 需要 LSPosed 的 "匯出設定" 或在模块 UI 里手动 refresh。
 */
class HookPrefs(context: Context? = null) {

    companion object {
        @Volatile
        private var instance: HookPrefs? = null

        fun get(): HookPrefs {
            return instance ?: synchronized(this) {
                instance ?: HookPrefs().also { instance = it }
            }
        }
    }

    private val prefs: XSharedPreferences by lazy {
        XSharedPreferences(Constants.MODULE_PACKAGE, Constants.PREFS_NAME).apply {
            makeWorldReadable()
            reload()
        }
    }

    fun reload() = prefs.reload()

    fun isEnabled(): Boolean = prefs.getBoolean(Constants.KEY_ENABLED, true)

    fun isBlacklistMode(): Boolean = prefs.getBoolean(Constants.KEY_BLACKLIST_MODE, true)

    fun getSelectedPackages(): Set<String> {
        @Suppress("UNCHECKED_CAST")
        return prefs.getStringSet(Constants.KEY_SELECTED_PACKAGES, emptySet())
            ?: emptySet()
    }

    /**
     * 判断某个包名是否应该被拦截。
     *
     * 黑名单模式：只拦截 selected 里的包
     * 白名单模式：拦截所有不在 selected 里的包
     */
    fun shouldBlock(packageName: String): Boolean {
        if (!isEnabled()) return false
        val selected = getSelectedPackages()
        return if (isBlacklistMode()) {
            selected.contains(packageName)
        } else {
            !selected.contains(packageName)
        }
    }
}
