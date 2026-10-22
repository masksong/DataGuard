# 不安装 Android Studio，在线构建 APK

此包内含 `.github/workflows/android-apk.yml`。把整个文件夹内容上传到一个 GitHub 仓库后即可使用 GitHub Actions 在线构建 APK。

## 操作步骤
1. 登录 GitHub，创建一个新仓库（建议 Private）。
2. 将本项目文件全部上传到仓库根目录，确保 `.github/workflows/android-apk.yml` 也上传。
3. 打开仓库的 **Actions** 页面。如果首次使用 Actions，按页面提示启用。
4. 选择 **Build Android APK**，点击 **Run workflow**，再点击绿色的 **Run workflow** 确认。
5. 等待工作流完成，打开该次运行页面，在 **Artifacts** 区域下载 `DataGuard-debug-apk`。
6. 解压后得到 `app-debug.apk`，传到 Android 手机安装。若系统提示，允许浏览器或文件管理器“安装未知应用”。

## 注意
- 这是 debug 签名 APK，适合个人测试安装，不是 Play 商店发行包。
- 如果构建失败，进入该次 Actions 运行页面查看失败步骤的日志。
- 普通未 Root 手机无法由此应用保证自动关闭蜂窝数据；它只能监控、提醒并引导手动断网。
