# 📍 经纬度实时定位

一个轻量级安卓应用，实时显示设备的经纬度、海拔和速度信息，并自动记录移动轨迹。基于Kotlin+Jetpack合成构建。

##📥  下载

👉 [点射前往releases版面下载最新版apk](https://gitee.com/henmo/geocoords-test/releases)

## ✨ 功能特性

- **实时定位**：基于系统 GPS/网络定位服务，持续获取当前经纬度坐标
- **海拔与速度**：显示当前海拔高度和移动速度，采用动态平滑算法——静止时消除抖动，快速移动时（如骑行、乘车）保持实时响应
- **轨迹记录**：自动记录坐标变化历史，避免重复记录同一位置，并统计累计定位次数
- **一键复制**：点击坐标卡片即可复制当前经纬度到剪贴板，方便分享
- **权限友好**：自动引导用户授予定位权限，并对 GPS 未开启等异常情况给出明确提示

##🖥️  界面预览

深色主题界面，清晰展示纬度、经度、海拔、速度四项核心数据，底部为可滚动的轨迹记录列表。

##🛠️技术横档

- **语言**：Kotlin
- **UI框架**jetpack组合(材料3)
- **定位方案**：Android原生`locationManager`（GPS_PROVIDER / NETWORK_PROVIDER）
- **最低支持版本**：Android 8.0 (API 26)

## 📦 安装使用

> ⚠️ 说明：本项目在 Yima IDE（移动端 IDE）中开发，当前仓库仅上传了核心源码文件（`MainActivity.kt`、`MapActivity.kt`、`AndroidManifest.xml`），尚未包含完整的 Gradle 工程配置文件（如 `build.gradle`、`themes.xml`、图标资源等）。如果只是想使用本应用，推荐直接从上方 Releases 下载 APK 安装；如果想基于源码编译，需要自行搭建标准 Android Studio 工程结构并补全上述配置文件。

1.克隆本仓库
2.使用Android Studio或兼容IDE打开项目，并补全标准Gradle工程文件
3.确保`AndroidManifest.xml` 中已声明定位权限：
   ```XML
<uses-permission android:name="android。许可。access_FINE_LOCATION"/>
<uses-permission android:name="android。许可。access_COARSE_LOCATION"/>
   ```
4. 编译运行，首次启动时授予定位权限即可使用

## 📋 权限说明

本应用仅在设备本地读取定位信息用于展示，**不会将坐标数据上传至任何服务器**，所有数据仅保存在应用运行时内存中。

##📄 许可证

本项目基于[Apache许可证2.0](许可证) 开源协议。

##👤  作者

**痕墨**

##🙏  特别鸣谢

感谢 **双子座**、**克劳德**在开发过程中提供的协助与支持。

---

如果这个项目对你有帮助，欢迎点个 ⭐ Star！
