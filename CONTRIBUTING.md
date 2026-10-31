# 参与开发

## 分支

- `main` 保持可构建、可发布
- 功能分支：`feat/<简短描述>`
- 修复分支：`fix/<简短描述>`

## 提交信息

采用 `type: summary` 形式，例如：

```
feat: 记住上次聚焦的应用
fix: 移动应用后焦点丢失
refactor: 把排序逻辑移入 core:domain
chore: 升级 AGP
```

## 提交前检查

```bash
./gradlew ktlintCheck detekt test
```

三项全部通过再提交。CI 会在 PR 上重复执行相同检查。

## 代码约定

- `core:domain` 不引入任何 Android 类型，保证可以在 JVM 上单测
- 业务规则写成纯函数，UI 层只负责渲染与事件
- 新增逻辑优先补单元测试，而不是靠手动点电视验证

## 新增模块

在 `settings.gradle.kts` 中 `include`，并在模块的 `build.gradle.kts` 中使用 Version Catalog 引用依赖版本；不要在模块里写死版本号。
