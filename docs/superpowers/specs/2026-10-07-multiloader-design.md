# XP Orb Trails：Forge 与 NeoForge 适配设计

状态：等待用户审阅，尚未开始修改产品代码。

## 目标与范围

在 Minecraft 26.2 和 26.3 上增加 Forge、NeoForge 支持，保留现有 Fabric 功能。
每种加载器使用独立 JAR；功能、菜单分类、语言、默认值及已保存方案保持一致。
本阶段交付本地测试包、测试结果和截图；GitHub 同步与 Release 发布在用户验收后另行执行。

源码基线：

| Minecraft | 分支 | 已发布基线提交 |
| --- | --- | --- |
| 26.2 | `26.2` | `1fe2a0a4ef2d890e8b265213981b31c74213dc2f` |
| 26.3 | `26.3` | `476792dbb2316e2ee7aa91dad48d5c1cd36aeaab` |

## 官方依赖核对

2026-10-07 查询官方版本清单，计划固定以下版本建立适配基线：

| Minecraft | Forge | NeoForge |
| --- | --- | --- |
| 26.2 | `26.2-65.1.3` | `26.2.0.88` |
| 26.3 | `26.3-66.0.9` | `26.3.0.52-beta` |

NeoForge 26.3 仍为 Beta，交付说明必须保留这一事实。
Forge 26.2 的官方 MDK 使用 Gradle 9.5.0，26.3 使用 9.7.1；两者要求 Java 25，ForgeGradle 范围为 `[7.0.17,8)`。
实施时固定构建插件的具体版本，避免最终构建依赖浮动版本。

来源：

- [Forge 官方发布清单](https://files.minecraftforge.net/net/minecraftforge/forge/promotions_slim.json)
- [NeoForge 官方 Maven 清单](https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml)
- [Forge 26.2 MDK](https://maven.minecraftforge.net/net/minecraftforge/forge/26.2-65.1.3/forge-26.2-65.1.3-mdk.zip)
- [Forge 26.3 MDK](https://maven.minecraftforge.net/net/minecraftforge/forge/26.3-66.0.9/forge-26.3-66.0.9-mdk.zip)

## 方案比较与选择

1. **共用源码、独立加载器模块（采用）**：菜单、设置、几何与资源只维护一份，各模块仅处理加载器差异。
2. 三份完整源码：首次迁移较直接，但以后每项修复需要重复修改，容易产生功能偏差。
3. 增加多加载器运行时框架：可以封装平台差异，但当前模组只需少量接入点，会增加额外依赖。

采用方案 1，不增加 Architectury 等运行时依赖。

## 目录与构建

结构参考 [Simple Voice Chat 的 26.3 分支](https://github.com/henkelmax/simple-voice-chat/tree/26.3)。
核对的参考提交为 `f8d8146e59630e9f70dc82e20eb2f81285353b39`。
其根 `settings.gradle` 注册不同加载器模块；`fabric`、`forge`、`neoforge` 分别有自己的构建文件、依赖和模组入口，并复用 `api`、`common`、`common-client`。
XP Orb Trails 采用相同的加载器分目录原则，只保留本项目需要的四个模块。

继续按 Minecraft 版本维护 `26.2`、`26.3` 两个分支，不新增长期维护的加载器分支。
每个分支整理为：

```text
common/
  build.gradle             共用模块的编译与测试
  src/main/java/           共用设置、持久化、菜单、预览、尾迹与闪光计算
  src/main/resources/      共用语言、图标和客户端 Mixin 配置
  src/test/java/           共用配置与几何回归测试
fabric/
  build.gradle             Fabric 构建与打包
  gradle.properties        Fabric 专属依赖版本
  src/main/                Fabric 入口、事件与 Mod Menu 接入、元数据
  src/gametest/            现有 Fabric 客户端回归
forge/
  build.gradle             Forge 构建与打包
  gradle.properties        Forge 专属依赖版本
  src/main/                Forge 入口、事件、设置界面接入、元数据
neoforge/
  build.gradle             NeoForge 构建与打包
  gradle.properties        NeoForge 专属依赖版本
  src/main/                NeoForge 入口、事件、设置界面接入、元数据
build.gradle               根构建、共用任务与产物检查
settings.gradle            注册 common、fabric、forge、neoforge
gradle.properties          Minecraft、模组和 Java 的共用版本信息
gradle/wrapper/            共用构建入口
```

`common` 是独立 Gradle 共用模块，不作为单独的运行时模组发布。
`fabric`、`forge`、`neoforge` 各自保留独立构建文件、元数据、初始化和事件接入。
三个加载器模块各自编译并打包共用源码与资源，避免将依赖某一加载器的公共二进制复制到其他平台；共用模块的输出不含加载器专属入口。
根项目统一组织构建、测试及产物检查，也能通过 `:fabric:build`、`:forge:build`、`:neoforge:build` 分别构建。
共用构建配置保存在本仓库，只引入本项目所需的插件和依赖。

以 Java 25 和本项目现有 Gradle 9.6.0 为首轮构建基线，保留现有 Fabric 依赖版本。
参考仓库的根 Wrapper 使用 9.5.1；官方 MDK 使用的 Wrapper 版本不能直接视为最低版本要求。
先验证 Fabric Loom、ForgeGradle 和 NeoForge 构建插件能共同配置和运行，再迁移源码。
如果工具兼容性检查失败，记录实际错误并修订构建方案，不提交无法构建的目录迁移。

## 代码边界

### 共用功能

- 保留 `TrailConfig`、配置修复与迁移、原子保存和损坏配置备份逻辑。
- 保留设置菜单、颜色编辑器、方案管理、预览与全部翻译。
- 共用客户端运行状态和配置存储，加载器入口传入配置目录；共用代码不引用 `FabricLoader`。
- `TrailRenderer` 接收 Minecraft 的世界、镜头、帧时间和渲染状态，去掉 Fabric 上下文类型。
- 保留经验球跟踪、拾取包确认、世界切换清理、透明排序、缓冲复用与截面算法。
- 通用 Mixin 按目标加载器正确注册，不在一个 JAR 中混装三种入口。

### 加载器接入

每个加载器模块只负责：客户端初始化、帧更新、按键注册、配置目录、渲染阶段和模组列表设置入口。
Fabric 保留 Mod Menu 的 Config 按钮；Forge、NeoForge 使用各自模组列表的配置入口。
三种加载器均保留 Open Trail Settings 按键。

渲染必须保持“提取状态”和“执行绘制”的先后关系，在允许的绘制阶段提交几何。
分别检查 26.2、26.3 的官方加载器源码，选择对应事件；确实缺少合适事件时使用平台专属 Mixin。
不在逐帧热路径中使用反射，也不改变已批准的尾迹视觉参数来回避接入问题。

## 配置与客户端行为

- 继续使用 `config/xp-orb-trails.json`；相同游戏目录中的既有设置与方案可沿用。
- 不强制改成 Forge 或 NeoForge 的 TOML 配置格式。
- 菜单继续使用常用、外观、高级、方案四页及现有页面重置范围。
- 跟随 Minecraft 语言，英文回退；现有英文和简体中文翻译保持同步。
- 默认 2 边朝向镜头、宽度 0.1；既有配置不被重置。
- 仅客户端安装，JAR 不向服务器要求安装该模组。
- Forge 与 NeoForge JAR 不依赖 Fabric API 或 Mod Menu。

## 版本与本地交付

拟使用下一功能版本 `1.3.0`，三种加载器共享功能版本，文件名区分加载器和游戏版本：

```text
xp-orb-trails-fabric-1.3.0+26.2.jar
xp-orb-trails-forge-1.3.0+26.2.jar
xp-orb-trails-neoforge-1.3.0+26.2.jar
xp-orb-trails-fabric-1.3.0+26.3.jar
xp-orb-trails-forge-1.3.0+26.3.jar
xp-orb-trails-neoforge-1.3.0+26.3.jar
```

内部版本使用对应加载器能正确解析的语义版本，并在元数据中明确游戏与加载器依赖。
不将文件名或 `+` 后缀视为启动器版本检测的唯一依据。
交付包含安装说明、SHA-256、实际验证结果及已知限制。既有 Release 与标签保持原样。

## 验证与完成标准

共六个客户端组合：两个 Minecraft 版本乘以三个加载器。

1. 每个加载器模块编译和打包成功；运行适用的配置、迁移和几何回归。
2. 检查 JAR 内元数据、入口、Mixin、语言、图标、许可证和游戏版本；不存在其他加载器的入口及必需依赖。
3. Fabric 两个版本运行现有完整客户端回归，确保共用代码迁移没有破坏已有行为。
4. Forge、NeoForge 四个组合实际启动客户端并进入隔离测试世界；检查模组列表与按键入口、英文与中文菜单、颜色和方案保存、各页面重置。
5. 检查移动经验球尾迹、2/3/8/32 边截面、两种混合模式、真实拾取闪光、无拾取消失时不闪光、断开与换世界清理。
6. 检查旧配置与已保存方案迁移后可继续读取和修改；保存后重新启动仍保留。
7. 为每个组合记录日志、截图、配置持久化结果、测试数与 JAR 摘要。未执行的场景明确标为未验证。

编译成功不等于客户端验证成功；截图和自动化回归也不替代用户的视觉与操作体验验收。
本任务不包含 FPS 提升承诺、其他 Minecraft 版本、菜单改版、新功能或 GitHub Actions。

## 设计自检

- 共用目录与加载器模块边界明确，没有要求三份完整源码。
- 客户端功能、既有配置和已发布版本均有保留要求。
- 已记录 Gradle 差异及兼容性前置检查，不假定现有 Fabric 事件可在其他加载器直接使用。
- 区分本地构建、客户端回归和人工验收，交付条件覆盖用户要求的四个新增组合。
