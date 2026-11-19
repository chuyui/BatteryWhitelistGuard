package com.whitelistguard.hooks

import com.whitelistguard.core.HookPrefs
import com.whitelistguard.core.Stats
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

/**
 * Hook AOSP 原生 DeviceIdleController。
 *
 * 目标：拦截系统自动把应用加入 Doze / 电池优化白名单的操作。
 *
 * AOSP 里相关的关键方法（不同 Android 版本略有差异）：
 *  - addPowerSaveWhitelistAppInternal(String pkg)
 *  - addPowerSaveWhitelistUserApp(String pkg, int userId)
 *  - addPowerSaveWhitelistSystemAppInternal(String pkg)
 *  - addPowerSaveWhitelistExceptIdleAppInternal(String pkg)
 */
object DeviceIdleHook {

    private const val TAG = "[WhitelistGuard][Doze]"

    fun init(classLoader: ClassLoader) {
        val clazz = try {
            XposedHelpers.findClass("com.android.server.deviceidle.DeviceIdleController", classLoader)
        } catch (t: Throwable) {
            XposedBridge.log("$TAG DeviceIdleController class not found: ${t.message}")
            return
        }

        // 所有可能的"加入白名单"方法名
        val addMethods = listOf(
            "addPowerSaveWhitelistAppInternal",
            "addPowerSaveWhitelistUserApp",
            "addPowerSaveWhitelistSystemAppInternal",
            "addPowerSaveWhitelistExceptIdleAppInternal",
            "addPowerSaveWhitelistApp"
        )

        for (methodName in addMethods) {
            try {
                XposedHelpers.findMethodsByExactParameters(
                    clazz, Void.TYPE, String::class.java
                ).forEach { method ->
                    if (method.name == methodName) {
                        XposedBridge.hookMethod(method, object : XC_MethodHook() {
                            override fun beforeHookedMethod(param: MethodHookParam) {
                                val pkg = param.args[0] as? String ?: return
                                val prefs = HookPrefs.get()
                                prefs.reload()
                                if (prefs.shouldBlock(pkg)) {
                                    Stats.recordWhitelistBlocked(pkg)
                                    XposedBridge.log("$TAG BLOCKED $methodName -> $pkg")
                                    param.result = null  // 吞掉这次调用，不真正加入白名单
                                }
                            }
                        })
                        XposedBridge.log("$TAG hooked: $methodName(String)")
                    }
                }
            } catch (t: Throwable) {
                // 某些方法在当前 ROM 不存在，跳过
            }
        }

        // 带 userId 版本
        try {
            XposedHelpers.findMethodsByExactParameters(
                clazz, Void.TYPE, String::class.java, Int::class.javaPrimitiveType
            ).forEach { method ->
                if (method.name.startsWith("addPowerSaveWhitelist")) {
                    XposedBridge.hookMethod(method, object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            val pkg = param.args[0] as? String ?: return
                            val prefs = HookPrefs.get()
                            prefs.reload()
                            if (prefs.shouldBlock(pkg)) {
                                Stats.recordWhitelistBlocked(pkg)
                                XposedBridge.log("$TAG BLOCKED ${method.name}(String,int) -> $pkg")
                                param.result = null
                            }
                        }
                    })
                    XposedBridge.log("$TAG hooked: ${method.name}(String,int)")
                }
            }
        } catch (_: Throwable) {}
    }
}
