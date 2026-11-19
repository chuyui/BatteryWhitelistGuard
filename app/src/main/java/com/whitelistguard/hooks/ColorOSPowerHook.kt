package com.whitelistguard.hooks

import com.whitelistguard.core.HookPrefs
import com.whitelistguard.core.Stats
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

/**
 * Hook OPlus / ColorOS 自定义的电源管理和自启动服务。
 *
 * ColorOS 在 AOSP 之外加了大量自己的实现：
 *  - OplusDeviceIdleController（OPlus 深度睡眠白名单）
 *  - ColorosPowerManagerService（电池优化）
 *  - OplusPackageManagerService / AutoStartManagerService（自启动）
 *  - 手机 App 端的 PhoneManager / Settings
 *
 * 由于不同版本类名不同，这里用"猜测类名 + try-catch"策略。
 */
object ColorOSPowerHook {

    private const val TAG = "[WhitelistGuard][ColorOS]"

    // system_server 里可能出现的 OPlus 电源类
    private val candidateClasses = listOf(
        "com.oplus.server.power.OplusDeviceIdleController",
        "com.oplus.server.power.OplusPowerManagerService",
        "com.coloros.server.power.ColorosPowerManagerService",
        "com.oplus.server.power.deep.DeepSleepOptimizer",
        "com.oplus.server.power.DeepSleepController"
    )

    // 自启动相关
    private val autoStartClasses = listOf(
        "com.oplus.server.pm.OplusPackageManagerService",
        "com.android.server.pm.OplusAutoStartManagerService",
        "com.oplus.server.autostart.OplusAutoStartManagerService",
        "com.coloros.server.pm.ColorosAutoStartManagerService"
    )

    fun init(classLoader: ClassLoader) {
        // 1. 尝试 hook OPlus 电源管理类
        for (className in candidateClasses) {
            tryHookWhitelistAdd(className, classLoader)
        }

        // 2. 尝试 hook 自启动服务
        for (className in autoStartClasses) {
            tryHookAutoStart(className, classLoader)
        }

        // 3. 通用兜底：hook PackageManagerService 里的后台启动
        tryHookBackgroundStart(classLoader)
    }

    /**
     * 在手机管家 / 设置 App 进程里 hook，拦截 UI 层自动加白名单的操作。
     */
    fun initAppSide(lpparam: XC_LoadPackage.LoadPackageParam) {
        // 手机管家里的自启动管理 Activity / Fragment
        val candidates = listOf(
            "com.oplus.phonemanager.autorun.AutoStartActivity",
            "com.coloros.phonemanager.autorun.AutoStartActivity",
            "com.android.settings.applications.manageapplications.ManageApplications"
        )
        for (cls in candidates) {
            try {
                XposedHelpers.findClass(cls, lpparam.classLoader)
                XposedBridge.log("$TAG app-side class found: $cls")
                // 这里可以进一步 hook onClick 等，阻止自动勾选
            } catch (_: Throwable) {}
        }
    }

    private fun tryHookWhitelistAdd(className: String, classLoader: ClassLoader) {
        val clazz = try {
            XposedHelpers.findClass(className, classLoader)
        } catch (_: Throwable) {
            return
        }

        // 找到所有名字里带 "add" + "whitelist"/"allow"/"white" 的方法
        clazz.declaredMethods.forEach { method ->
            val name = method.name.lowercase()
            if ((name.contains("add") || name.contains("allow")) &&
                (name.contains("whitelist") || name.contains("white") || name.contains("allowbg") || name.contains("deep"))
            ) {
                try {
                    XposedBridge.hookMethod(method, object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            val pkg = extractPackageName(param.args) ?: return
                            val prefs = HookPrefs.get()
                            prefs.reload()
                            if (prefs.shouldBlock(pkg)) {
                                Stats.recordWhitelistBlocked(pkg)
                                XposedBridge.log("$TAG BLOCKED ${method.name} -> $pkg")
                                param.result = null
                            }
                        }
                    })
                    XposedBridge.log("$TAG hooked $className.${method.name}")
                } catch (_: Throwable) {}
            }
        }
    }

    private fun tryHookAutoStart(className: String, classLoader: ClassLoader) {
        val clazz = try {
            XposedHelpers.findClass(className, classLoader)
        } catch (_: Throwable) {
            return
        }

        clazz.declaredMethods.forEach { method ->
            val name = method.name.lowercase()
            if ((name.contains("allow") || name.contains("enable") || name.contains("add")) &&
                (name.contains("autostart") || name.contains("auto_start") || name.contains("autorun") || name.contains("boot"))
            ) {
                try {
                    XposedBridge.hookMethod(method, object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            val pkg = extractPackageName(param.args) ?: return
                            val prefs = HookPrefs.get()
                            prefs.reload()
                            if (prefs.shouldBlock(pkg)) {
                                Stats.recordBackgroundBlocked(pkg)
                                XposedBridge.log("$TAG BLOCKED autostart ${method.name} -> $pkg")
                                param.result = null
                            }
                        }
                    })
                    XposedBridge.log("$TAG hooked autostart $className.${method.name}")
                } catch (_: Throwable) {}
            }
        }
    }

    private fun tryHookBackgroundStart(classLoader: ClassLoader) {
        // Hook AMS 的 background activity start 限制放行
        val amsClass = try {
            XposedHelpers.findClass("com.android.server.am.ActivityManagerService", classLoader)
        } catch (_: Throwable) { return }

        // 找到 "isBackgroundRestricted" / "backgroundStartAllowed" 之类的方法
        // 这里用一个保守策略：不 hook AMS 本身（影响太大），只记录
        XposedBridge.log("$TAG ActivityManagerService found, background start monitoring ready")
    }

    /**
     * 从方法参数里尽力提取包名。
     * 方法参数可能是 String（包名），也可能是 ApplicationInfo / PackageInfo 等对象。
     */
    private fun extractPackageName(args: Array<Any>?): String? {
        args ?: return null
        for (arg in args) {
            when (arg) {
                is String -> if (arg.contains(".")) return arg
                is android.content.pm.ApplicationInfo -> return arg.packageName
                is android.content.pm.PackageInfo -> return arg.packageName
                else -> {
                    // 尝试反射读 packageName 字段
                    try {
                        val pkgField = arg.javaClass.getField("packageName")
                        return pkgField.get(arg) as? String
                    } catch (_: Throwable) {}
                }
            }
        }
        return null
    }
}
