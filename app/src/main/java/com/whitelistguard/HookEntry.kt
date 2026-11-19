package com.whitelistguard

import android.content.Context
import com.whitelistguard.core.Constants
import com.whitelistguard.hooks.ColorOSPowerHook
import com.whitelistguard.hooks.DeviceIdleHook
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.callbacks.XC_LoadPackage

/**
 * LSPosed / Xposed 模块入口。
 *
 * 作用在两个进程上：
 *  1. android (system_server) —— hook DeviceIdleController、ColorOS 电源/自启动服务
 *  2. 模块自身进程 (com.whitelistguard) —— 不需要 hook，仅提供 UI
 */
class HookEntry : IXposedHookLoadPackage {

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        when (lpparam.packageName) {
            // 系统主进程：所有电源管理、Doze 白名单、自启动都在这里
            "android" -> {
                try {
                    DeviceIdleHook.init(lpparam.classLoader)
                } catch (t: Throwable) {
                    XposedBridge.log("[WhitelistGuard] DeviceIdleHook init failed: ${t.message}")
                }
                try {
                    ColorOSPowerHook.init(lpparam.classLoader)
                } catch (t: Throwable) {
                    XposedBridge.log("[WhitelistGuard] ColorOSPowerHook init failed: ${t.message}")
                }
            }
            // 手机管家 / 安全中心：ColorOS 在这里做自启动和电池优化的 UI 与逻辑
            "com.coloros.phonemanager",
            "com.oplus.phonemanager",
            "com.android.settings" -> {
                try {
                    ColorOSPowerHook.initAppSide(lpparam)
                } catch (t: Throwable) {
                    XposedBridge.log("[WhitelistGuard] AppSide hook failed: ${t.message}")
                }
            }
        }
    }
}
