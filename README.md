# Slate

极简、纯黑、无广告的 Android TV 桌面。为低内存电视设计。

- **纯原生 View + Kotlin**，不引入 AndroidX UI、不使用 Compose
- 界面全黑，应用网格自动排布
- 长按管理应用：移动、隐藏、查看信息、卸载
- 应用安装或卸载后自动刷新
- 记住上次的焦点位置
- 中英文界面

## 架构

```
slate/
├── app/          # Android 应用：Activity、View、资源
├── core/
│   ├── domain/   # 纯 Kotlin：模型、排序、隐藏、焦点计算（可 JVM 单测）
│   └── data/     # Android library：PackageManager 查询、偏好存储
└── .github/      # CI
```

业务规则放在 `core:domain`，不依赖 Android，全部可以跑单元测试；`core:data` 负责与系统打交道；`app` 只做渲染和事件绑定。

## 构建

需要 JDK 17 与 Android SDK（compileSdk 35）。

```bash
./gradlew assembleRelease
```

产物：`app/build/outputs/apk/release/app-release.apk`

本地调试：

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## 安装到电视

```bash
adb connect <电视IP>:5555
adb install app/build/outputs/apk/release/app-release.apk
adb shell cmd package set-home-activity dev.minorcell.slate/dev.minorcell.slate.ui.HomeActivity
```

按 Home 键即可看到 Slate。若要停用电视自带的桌面：

```bash
adb shell pm disable-user --user 0 <自带桌面包名>
```

恢复：`adb shell pm enable <包名>`。

## 一键构建并替换

`script/replace.sh` 会自动构建、安装、切换默认桌面，并在确认新桌面成功启动后才删除旧桌面：

```bash
./script/replace.sh <电视IP>
# 例如
./script/replace.sh 192.168.1.71
```

带上旧桌面包名，替换成功后会删除它：

```bash
OLD_PACKAGE=dev.lite.launcher ./script/replace.sh 192.168.1.71
```

不传 `OLD_PACKAGE` 时只安装，不删除任何程序。

## 开发检查

```bash
./gradlew ktlintCheck detekt test
```

CI 在每次 push 和 PR 时执行以上检查并构建 debug APK。

## 许可证

[Apache-2.0](LICENSE)

设计方向参考了社区里的极简 TV 桌面思路，代码为本项目独立实现。
