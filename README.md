# 白名单守卫 (Battery Whitelist Guard)

一个 LSPosed / Xposed 模块，专为 **ColorOS / OPlus（一加、OPPO、realme）** 设计，阻止系统自动把应用塞进电池优化白名单，并管理自启动与后台启动。

## 功能

- **拦截电池优化白名单添加**：Hook AOSP `DeviceIdleController` + OPlus 自定义电源服务，阻止系统自动给应用加 Doze 白名单
- **拦截自启动 / 后台启动**：Hook OPlus 自启动管理服务，按规则阻止应用后台拉起
- **黑名单 / 白名单双模式**：
  - 黑名单：只拦截勾选的应用
  - 白名单：未勾选的应用全部拦截
- **拦截统计**：记录总拦截次数、后台阻断次数、每小时触发频率
- **现代 UI**：Jetpack Compose 构建，对标参考截图

## 前提条件

| 项目 | 要求 |
|------|------|
| Root | Magisk / KernelSU / APatch |
| Xposed 框架 | LSPosed (Zygisk 版) 推荐 |
| 系统 | ColorOS 13+（已在 ColorOS 17 / 一加13 上验证目标） |
| minSdk | Android 10 (API 29) |

## 构建

### 方式一：Android Studio（推荐）

1. 用 Android Studio Hedgehog (2023.1) 或更高版本打开本项目
2. 等待 Gradle sync 完成
3. Build → Build APK(s)
4. APK 输出在 `app/build/outputs/apk/debug/app-debug.apk`

### 方式二：命令行

```bash
# 生成 gradle wrapper（如果没有）
gradle wrapper --gradle-version 8.2

# 构建 Debug APK
./gradlew assembleDebug
```

## 安装与启用

1. 把构建出的 APK 安装到手机
2. 打开 LSPosed Manager → 模块 → 启用「白名单守卫」
3. **作用域勾选**：
   - `android`（系统框架，必须）
   - `系统界面` (SystemUI)
   - `手机管家` / `安全中心`（com.oplus.phonemanager / com.coloros.phonemanager）
   - `设置` (com.android.settings)
4. 重启手机
5. 打开「白名单守卫」App，选择拦截模式，勾选要管控的应用

## 项目结构

```
app/src/main/
├── AndroidManifest.xml          # 声明 Xposed module metadata
├── assets/
│   └── xposed_init              # 指定 HookEntry 类名
├── java/com/whitelistguard/
│   ├── HookEntry.kt             # Xposed 入口，按包名分发 hook
│   ├── core/
│   │   ├── Constants.kt         # 包名、Sp key 常量
│   │   ├── HookPrefs.kt         # system_server 读取配置 (XSharedPreferences)
│   │   └── Stats.kt             # 拦截统计内存计数器
│   ├── hooks/
│   │   ├── DeviceIdleHook.kt    # Hook AOSP DeviceIdleController
│   │   └── ColorOSPowerHook.kt  # Hook OPlus/ColorOS 电源 + 自启动服务
│   └── ui/
│       └── MainActivity.kt      # Compose 主界面
└── res/
    ├── values/                  # strings, themes, xposed_scope
    └── drawable/ mipmap/        # 自适应图标
```

## 工作原理

```
系统自动想把 App X 加入电池优化白名单
        │
        ▼
DeviceIdleController.addPowerSaveWhitelistApp(pkg)
        │
        ▼  ← 我们在这里 hook
   检查 HookPrefs.shouldBlock(pkg)
        │
   ┌────┴────┐
   │ 拦截?   │
   └────┬────┘
   是   │   否
   ▼       ▼
吞掉调用   放行（系统正常加白名单）
记录统计
```

## 适配说明

由于不同 ColorOS 版本的 OPlus 系统服务类名可能不同，`ColorOSPowerHook` 采用了**候选类名 + try-catch** 策略：

- `com.oplus.server.power.OplusDeviceIdleController`
- `com.oplus.server.power.OplusPowerManagerService`
- `com.coloros.server.power.ColorosPowerManagerService`
- `com.oplus.server.pm.OplusPackageManagerService`
- 等等

如果某些 hook 没生效，可以：
1. 用 LSPosed 的日志查看器过滤 `[WhitelistGuard]`
2. 用 `logcat -s WhitelistGuard` 查看哪些类找到了、哪些没找到
3. 反馈你的 ROM 版本和目标类名，我来补充适配

## License

MIT
