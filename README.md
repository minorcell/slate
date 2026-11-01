<div align="center">

# Slate

**给旧电视的干净桌面。**  
时间、壁纸、一行应用。没有广告，没有会员入口，打开就能用。

<p>
  <a href="https://github.com/minorcell/slate/actions/workflows/ci.yml">
    <img alt="CI" src="https://img.shields.io/github/actions/workflow/status/minorcell/slate/ci.yml?branch=main&label=CI">
  </a>
  <a href="https://kotlinlang.org/">
    <img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-2.1-7F52FF?logo=kotlin&logoColor=white">
  </a>
  <a href="https://developer.android.com/tv">
    <img alt="Android TV" src="https://img.shields.io/badge/Android-TV-3DDC84?logo=android&logoColor=white">
  </a>
  <a href="https://github.com/minorcell/slate/releases/latest">
    <img alt="下载" src="https://img.shields.io/badge/%E4%B8%8B%E8%BD%BD-APK-1A304C">
  </a>
</p>

<img src="docs/home.jpg" width="840" alt="电视上的 Slate：左上是时间，底部是一行应用">

</div>

---

国产电视的桌面，广告、通知、会员入口经常占满屏幕。  
已有的开源桌面里有不少好作品，但在系统老、内存小的电视上，往往卡顿、掉帧、响应慢。

**Slate 是为此专门重写的桌面。**  
它只做三件事：显示时间、铺开壁纸、把应用排成一行。  
打开电视，就是这一幅画面。没有多余动画，没有后台推荐，按遥控器立刻有反应。

## 它长什么样

左上角是时间和日期，当前选中的应用名会显示在时间下方。  
应用排成一行，贴在屏幕底部。  
右上角是电视自己的设置入口。

确定键直接打开应用。  
长按可以移动位置、查看信息或卸载。  
安装或卸载应用后，这一行会自动刷新。  
顺序和上次停留的位置都会被记住，下次打开还在原来的地方。

设置、应用详情、卸载这些操作，都会直接跳到系统自己的页面，不会在 Slate 里再套一层。

## 为什么是它

- **干净**：没有广告、没有会员弹窗、没有推荐行。
- **轻**：专为系统版本旧、内存小的电视优化，打开快、切换顺。
- **简单**：遥控器就能完成所有操作，不需要额外学习。
- **稳定**：应用顺序和焦点会被记住，不会每次都重新排列。

## 开始使用

下载最新 APK，安装后设为默认桌面即可。

- [开发文档](docs/development.md)：如何构建、跑模拟器、装到电视上
- [界面说明](docs/ui.md)：桌面结构、焦点规则和文案
- [参与开发](CONTRIBUTING.md)：分支、提交信息和代码约定
- [交互原型](prototype/index.html)

## 许可证

[Apache-2.0](LICENSE)

设计方向参考了社区里的极简电视桌面，代码为本项目独立实现。