# XP Orb Trails 多加载器适配实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans or superpowers:subagent-driven-development to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 Minecraft 26.2、26.3 增加 Forge 和 NeoForge 客户端支持，并保留 Fabric 的现有行为。

**Architecture:** 每个游戏版本分支使用 `common`、`fabric`、`forge`、`neoforge` 四个 Gradle 模块。共用源码与资源进入各加载器独立 JAR，加载器模块只负责初始化、事件和设置入口。

**Tech Stack:** Java 25、Gradle 9.6.0、Fabric Loom、ForgeGradle、NeoForge ModDevGradle、Minecraft 原生界面、Mixin、JUnit Jupiter。

**Spec:** `docs/superpowers/specs/2026-10-07-multiloader-design.md`

## Global Constraints

- Minecraft 分支为 `26.2`、`26.3`；功能版本为 `1.3.0`，先交付本地测试包。
- 固定 Forge `26.2-65.1.3`、`26.3-66.0.9`；NeoForge `26.2.0.88`、`26.3.0.52-beta`。
- 构建插件固定具体版本；保留现有 Fabric 依赖，不引入多加载器运行时框架。
- 继续使用 `config/xp-orb-trails.json`，保留既有配置、方案、英文和简体中文翻译。
- 默认截面 2 边、宽度 0.1；菜单分类、页面重置与视觉算法保持现有行为。
- 三个加载器分别打包，不交叉依赖其他加载器；保持仅客户端使用。
- 既有 Release、标签与 GitHub Actions 设置保持原样；验收前不推送或发布。
- 源码、诊断和注释使用英文；交付说明和验证报告使用中文。

## Review Focus

1. 加载器元数据或 Mixin 缺失：JAR 必须能在对应加载器的客户端实际加载。
2. 提取与绘制阶段不匹配：尾迹必须在合适阶段绘制，透明排序和缓冲生命周期正确。
3. 设置菜单过早保存或读取错误目录：旧配置迁移后可编辑，重启后仍保留。
4. 世界切换和关闭渲染器：不得残留旧世界轨迹、错误闪光或未释放的 GPU 缓冲。
5. 共用目录迁移影响 Fabric：原有完整客户端回归必须在两个版本继续通过。

## Task 1：验证构建工具并建立四模块构建

**Files:** 修改根 `settings.gradle`、`build.gradle`、`gradle.properties`；创建四个模块的 `build.gradle` 和三个加载器的 `gradle.properties`。

**Interfaces:** 根任务 `build`、`test`、`verifyReleaseJar` 汇总适用模块；各平台支持 `:fabric:build`、`:forge:build`、`:neoforge:build`。共用版本来自根属性，平台依赖来自模块属性。

- [ ] 在隔离工作目录内运行原项目测试与 JAR 完整性检查，保存基线结果。
- [ ] 根据官方构建插件版本清单选择具体版本，验证三个插件在 Gradle 9.6.0 下能共同配置；不先迁移源码。
- [ ] 如果出现实际兼容性错误，记录报错并选择能共同使用的 Wrapper 版本，重新检查后再继续。
- [ ] 注册四个模块，配置 Java 25、资源合并、测试依赖、许可证打包与各加载器产物命名。
- [ ] 将现有源码按已批准设计迁移到 common 和 fabric；保持 Git 可识别的文件移动。
- [ ] 运行 Fabric 构建与现有配置测试；确认依赖与公共源码进入正确源集。
- [ ] 暂存明确的迁移文件列表、审查差异并提交。

## Task 2：分离平台入口与共用渲染输入

**Files:** `common/src/main/java/dev/goldeneggovo/xporbtrails/XpOrbTrailsClient.java`、`TrailRenderer.java`；`fabric/src/main/java/dev/goldeneggovo/xporbtrails/FabricXpOrbTrailsClient.java`、`ModMenuIntegration.java`；共用 Mixin 和 Fabric 元数据。

**Interfaces:** 共用入口提供 `initialize(Path configDirectory)`、`createConfigScreen(Screen parent)`、`saveConfig()`；配置状态继续通过 `XpOrbTrailsClient.CONFIG` 使用。渲染提取方法接收 `ClientLevel`、镜头位置与帧插值，绘制方法接收该帧的镜头位置；不再接收 Fabric 上下文类型。

- [ ] 在现有配置与客户端回归中增加配置目录、保存后重启读取的检查，先验证错误目录或未正确初始化会被检出。
- [ ] 将加载器配置目录改为入口传入，共用代码去掉 FabricLoader 和 ClientModInitializer 依赖。
- [ ] Fabric 入口负责渲染事件、按键和客户端更新注册，向共用逻辑传递 Minecraft 原生输入。
- [ ] 保留几何、透明排序、拾取包确认和世界清理算法，检查共用目录没有 Fabric/Forge/NeoForge 专属引用。
- [ ] 在 26.3 上运行全部 JUnit、Fabric 客户端回归、构建与 JAR 检查。
- [ ] 审查并提交这一独立可验证的共用代码迁移。

## Task 3：实现 Forge 与 NeoForge 26.3 接入

**Files:** `forge/src/main/java/dev/goldeneggovo/xporbtrails/ForgeXpOrbTrails.java`、`neoforge/src/main/java/dev/goldeneggovo/xporbtrails/NeoForgeXpOrbTrails.java`；各自元数据、必要的平台渲染适配类和测试源集。

**Interfaces:** 两个入口分别调用 Task 2 的共用初始化、菜单和渲染方法；平台专属代码接收本加载器事件并转换为共用输入。测试运行器只放在测试源集，不进入发布 JAR。

- [ ] 检查固定版本的官方源码，明确客户端生命周期、按键、模组列表配置入口、提取和绘制阶段的实际 API。
- [ ] 扩展 `verifyReleaseJar` 检查每个平台必需的入口、Mixin、语言、许可证及元数据，排除其他平台的入口和元数据；先验证不完整 JAR 会失败。
- [ ] 编写两个客户端接入并注册共用 Mixin，保持配置路径、菜单入口和快捷键一致。
- [ ] 为缺少合适渲染事件的平台增加最小专属 Mixin，保证提取先于绘制，不引入逐帧反射。
- [ ] 建立测试专用客户端验证：实际加载发布 JAR，进入隔离世界，检查菜单、持久化、尾迹、真实拾取、无拾取消失、世界清理和退出。
- [ ] 运行 Forge、NeoForge 26.3 构建、JAR 检查和客户端验证，保留截图与日志；NeoForge 26.3 标注 Beta。
- [ ] 审查并提交两个新平台接入。

## Task 4：适配 Minecraft 26.2 并完成六组合回归

**Files:** 将已验证的结构和共用改动应用到 26.2；分别调整 26.2 的 `TrailRenderer`、平台接入和版本属性。

**Interfaces:** 保持与 26.3 相同的共用入口和构建任务；26.2 使用自己的 Minecraft 渲染 API 与依赖，不复制 26.3 的 GPU 实现覆盖它。

- [ ] 将四模块迁移应用到 26.2，保留该版本已有渲染类型与生命周期差异。
- [ ] 固定 26.2 对应 Forge、NeoForge 和既有 Fabric 依赖，修正平台事件及元数据范围。
- [ ] 运行两个游戏版本的全部适用单元测试、三个平台构建和 JAR 检查，记录实际测试数和跳过项。
- [ ] Fabric 两个版本运行现有完整客户端回归；Forge、NeoForge 四个组合实际启动进入测试世界。
- [ ] 六个组合验证英文/中文菜单、两个设置入口、各页重置、颜色与方案保存后重启、2/3/8/32 边、两种混合模式、拾取确认和换世界清理。
- [ ] 审查跨分支差异并提交对应分支的兼容性改动。

## Task 5：本地交付与最终审查

**Files:** 更新 README、CONTRIBUTING、CHANGELOG；在工作区 `deliverables/`、`reports/` 保存安装包和实际验证记录。

**Interfaces:** 六个可安装 JAR 按 `xp-orb-trails-<loader>-1.3.0+<minecraft>.jar` 命名；交付 JSON 记录加载器、游戏版本、源码提交、测试结果、截图路径、SHA-256 和未验证事项。

- [ ] 检查三个产物各自独立加载，共用文件不重复、不缺失，测试运行器未进入发布包。
- [ ] 完成整批差异审查，重点核对 Review Focus 的五项与现有配置兼容性。
- [ ] 更新源码结构、各加载器安装要求、菜单入口和本地构建命令；更新记录保留 Unreleased 状态。
- [ ] 将六个验证通过的 JAR、摘要、截图及中文测试说明放入本地交付目录。
- [ ] 明确区分已自动验证、已启动客户端验证及待用户验收；不声称未经测量的 FPS 改善。
- [ ] 回报本地下载路径、各组合结果和已知限制，等待用户验收后再进行 GitHub 同步或发布。

## 执行记录

计划已对照设计自检：四个新增平台组合、两个 Fabric 回归、目录与构建、配置兼容、产物检查和本地交付均有对应任务。
当前尚未开始执行，执行方式待用户选择。
