# 开发

构建、在模拟器或电视上运行、以及提交前的检查。产品说明见 [README](../README.md)，界面规范见 [ui.md](ui.md)，协作约定见 [CONTRIBUTING](../CONTRIBUTING.md)。

## 环境

- JDK 17
- Android SDK，`compileSdk 35`（`local.properties` 里的 `sdk.dir` 指向它）
- `sdkmanager`、`avdmanager`、`emulator` 默认不在 PATH 上：

```bash
export ANDROID_HOME="$HOME/Library/Android/sdk"
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/emulator:$ANDROID_HOME/platform-tools:$PATH"
```

根目录的 `Makefile` 用的是 SDK 绝对路径，不依赖以上设置。

## 架构

```
slate/
├── app/          # Android 应用：Activity、View、资源
├── core/
│   ├── domain/   # 纯 Kotlin：模型、排序、焦点计算（可 JVM 单测）
│   └── data/     # Android library：PackageManager 查询、偏好存储
├── docs/         # 文档
├── prototype/    # HTML 交互原型，仅用于推敲交互，不参与构建
└── .github/      # CI
```

业务规则放在 `core:domain`，不依赖 Android，全部可以跑单元测试；`core:data` 负责与系统打交道；`app` 只做渲染和事件绑定。

## 构建

```bash
./gradlew assembleRelease
```

产物：`app/build/outputs/apk/release/app-release.apk`

本地调试包：

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## 本地模拟器

首次需要在 SDK 里建一台 TV 模拟器（本项目用 `slate_tv`）：

```bash
sdkmanager "system-images;android-34;android-tv;arm64-v8a"
echo no | avdmanager create avd -n slate_tv -k "system-images;android-34;android-tv;arm64-v8a" -d tv_1080p
```

已经建过就不必重复，`emulator -list-avds` 能看到 `slate_tv` 即可。首次开机较慢，之后走快照通常在几秒内。

之后用根目录的 `Makefile`，不用再记 adb 与 Gradle 参数：

| 命令 | 作用 |
| --- | --- |
| `make emu` | 启动 `slate_tv` 并等到开机完成；已在运行则直接返回 |
| `make run` | 构建、安装、启动，并截图到 `build/screenshots/` |
| `make shot` | 只截当前画面 |
| `make logs` | 只看 Slate 相关的 logcat |
| `make emu-stop` | 关闭模拟器 |
| `make check` | ktlint、detekt、单元测试 |

`make run` 通过 `ANDROID_SERIAL` 只装到模拟器。即使电视也连着 adb，也不会被覆盖；装到电视请用下面的 `./script/replace.sh`。

改完代码跑一次 `make run`，模拟器上的应用会重启并留下截图，通常十秒内完成。可调参数：`make emu AVD=<名称>`、`make run SERIAL=<设备序列号>`。

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

或 `make check`。CI 在每次 push 和 PR 时执行以上检查并构建 debug APK。
