# Antigravity Mobile

轻量级安卓伴侣应用（`com.antigravity.mobile`），基于 **Kotlin + Jetpack Compose + Material 3（深色主题）** 构建，帮助你在 Android 手机上便捷连接与控制 Antigravity 网页控制台。

## 核心特性

1. **默认打开方式（Chrome Custom Tabs）**
   - 使用 `androidx.browser.customtabs` 以深色全屏、自动隐藏地址栏方式打开目标工作台。
   - 直接复用手机 Chrome 的 Google 账号登录态（避免 Google OAuth 拦截 WebView 登录）并保留浏览器推送通知。

2. **备用打开方式（内置 WebView）**
   - 提供开关一键切换内置 WebView 页面，支持 JavaScript、第三方 Cookie、文件/图片上传（`onShowFileChooser`）以及系统返回键逐级后退。
   - 若页面跳转至 `accounts.google.com` 登录页，自动无缝改用 Chrome Custom Tabs 打开。

3. **首页伴侣界面**
   - **工作台切换**：支持一键切换 **Antigravity 控制台** (`https://antigravity.google.com`)、**Google AI Studio** (`https://aistudio.google.com`) 以及**自定义 URL**（自动持久化保存）。
   - **常用开发指令快捷板**：预置 4 条高频开发指令（*查进度与 Diff*、*跑测试并修复*、*暂停等待确认*、*生成 Commit 信息*），支持自由新增、删除与重置并本地持久化；点击任意指令自动复制到剪贴板并立即打开目标工作台。
   - **启动设置**：提供「打开 App 自动直达工作台」开关（默认关闭，开启后点击桌面图标直接进入工作台）。

4. **自动打包 APK**
   - 内置 `.github/workflows/build-apk.yml`，在 `push` 到 `main`/`master` 分支或手动触发 `workflow_dispatch` 时，自动使用 JDK 17 + Gradle 编译 Debug APK 并上传 `app-debug.apk` Artifact。

## 本地编译

```bash
./gradlew assembleDebug
```

编译完成后，APK 位于：`app/build/outputs/apk/debug/app-debug.apk`。
