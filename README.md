# Skyblock Mob Overlay（合规版本 — 非透视 ESP）

## 这个 mod 做什么 / 不做什么

**做的事**：在屏幕上*已经能看到*的生物头顶，叠加渲染等级 / 血量 / 稀有度着色文字。

**明确不做的事**：不穿墙显示、不发光轮廓（Glow Outline）、不脱离深度测试渲染
任何东西。所有绘制调用都复用原版实体渲染那一帧的 `MatrixStack` /
`VertexConsumerProvider`，并显式使用开启深度测试的 RenderLayer /
`TextLayerType.NORMAL`。地形、墙壁挡住了实体本身，也就同样挡住了这层叠加文字
——这跟原版末影龙 / 凋灵的血条渲染是同一套原理。

如果你后续想自己改成穿墙显示，需要主动去掉 `depthTest(...)` 那一行、把
`TextLayerType.NORMAL` 换成 `SEE_THROUGH`，并接入 `GameRenderer.getRenderLayers()`
之类的发光轮廓管线——但这会让 mod 变成 Hypixel 明确禁止的透视类客户端修改，
我不建议这么做，也不会帮你补全那部分。

## 项目结构

```
skyblock-overlay/
├── build.gradle                 Loom 1.15 + Gradle 9.4，Java 25 target
├── gradle.properties            版本号集中管理
├── settings.gradle
└── src/
    ├── main/                    公共代码（客户端+服务端都能加载，这里基本没有服务端逻辑）
    │   └── resources/
    │       ├── fabric.mod.json
    │       └── skyblockoverlay.client.mixins.json
    └── client/                  纯客户端代码（26.1 起 loom.splitEnvironmentSourceSets() 支持）
        └── java/com/skyblockoverlay/
            ├── client/SkyblockOverlayClient.java      入口，注册按键/加载配置
            ├── config/
            │   ├── OverlayConfig.java                 纯数据配置类
            │   ├── ConfigManager.java                 YACL 绑定 + GUI 构建
            │   └── ModMenuIntegration.java             Mod Menu 挂载点
            ├── data/
            │   ├── MobData.java                        解析结果 record
            │   └── MobDataParser.java                  名称正则解析（可在 GUI 内热改正则）
            ├── render/
            │   ├── EntityOverlayRenderer.java          实际绘制逻辑（深度测试文字 + 血条）
            │   └── OverlayRenderLayers.java             自定义深度测试 RenderLayer
            ├── util/SkyblockServerDetector.java         简单的"是否在 Hypixel"判断
            └── mixin/LivingEntityRendererMixin.java     渲染管线挂载点
```

## 环境版本（已核实，2026-04）

| 组件 | 版本 |
|---|---|
| Minecraft | 26.1.2 |
| Fabric Loader | 0.18.4 |
| Fabric API | 0.145.4+26.1.2 |
| Loom | 1.15 |
| Gradle | 9.4.0 |
| JDK | 25（26.1 起硬性要求） |
| 映射 | Mojang 官方映射（26.1 起完全去混淆，无需 Yarn） |

## ⚠️ 需要你在真机上核实的三处

26.1 是刚发布不久的大版本，实体渲染管线和 Blaze3D 渲染管线都有较大重构。
我按照 1.21.x 系一贯的 API 语义写了这几处，但**具体类名/方法签名请对照
Loom 生成的官方映射源码（`./gradlew genSources` 之后 IDEA 里能直接跳转）核实**：

1. **`LivingEntityRendererMixin` 里的 `render(...)` 方法描述符**
   26.1 把大量渲染逻辑迁移到了 `LivingEntityRenderState` 快照对象上，
   实际签名可能已经变成 `render(S state, MatrixStack matrices, ...)`
   这种形式而不是直接传 `LivingEntity`。如果编译时报"找不到匹配的方法"，
   去反编译源码里搜 `class LivingEntityRenderer` 找到真实签名，
   照抄描述符即可，注入点逻辑（TAIL，复用同一个矩阵栈）不需要变。

2. **`OverlayRenderLayers` 里的 `RenderPhase` 常量名**
   `POSITION_COLOR_PROGRAM` / `LEQUAL_DEPTH_TEST` 等常量在不同版本里
   偶尔会改名（比如 `1.21.2` 前后就变过一次）。核心约束不变：一定要
   显式带上某个"深度测试开启"的 phase，绝不能用 disableDepthTest。

3. **`TextRenderer.draw(...)` 的重载参数顺序**
   不同版本这个重载的参数个数/顺序会微调，但那个 `TextLayerType`
   参数（NORMAL vs SEE_THROUGH）是所有版本里语义一致的关键位——
   写代码时优先确认这一个参数传对了。

## 构建

```bash
./gradlew build
```

产物在 `build/libs/skyblock-overlay-1.0.0.jar`，连同 Fabric API、YACL、
Mod Menu 一起放进 `.minecraft/mods`。

## 上传到 GitHub

```bash
cd skyblock-overlay
git init
git add .
git commit -m "Initial commit: Skyblock Mob Overlay (Fabric 26.1.2)"
git branch -M main
git remote add origin https://github.com/你的用户名/skyblock-mob-overlay.git
git push -u origin main
```

`.gitignore` 已经排除了 `build/`、`.gradle/`、`run/`、`.idea/` 这些不该进仓库的目录。

## 关于 Gradle Wrapper

项目里目前没有提交 `gradlew` / `gradlew.bat` / `gradle/wrapper/gradle-wrapper.jar`
（其中 wrapper jar 是二进制文件，我这边没有联网权限下载）。本地第一次用之前，
在项目根目录跑一次：

```bash
gradle wrapper --gradle-version 9.4
```

之后就会生成标准的 wrapper 文件，正常用 `./gradlew build` 即可，也建议把这几个
生成的文件一起提交到仓库（wrapper jar 体积很小，是 Gradle 官方推荐提交进仓库的）。

## GitHub Actions 自动构建

`.github/workflows/build.yml` 已经配置好：每次 push 到 `main` 或提 PR 时自动跑
`gradle build`，构建产物（jar）会出现在该次运行的 **Artifacts** 里。如果你打
`v1.0.0` 这种 tag 推送，还会自动创建一个 GitHub Release 并把 jar 挂上去。

因为仓库里暂时没有 wrapper jar，workflow 用的是 `gradle/actions/setup-gradle`
直接拉取 Gradle 9.4.0 来跑（`gradle build` 而不是 `./gradlew build`）。等你按
上一节生成并提交了 wrapper 文件后，把 workflow 里的 `gradle build` 改回
`./gradlew build` 也完全没问题。

## GUI 使用

游戏内按 **右 Shift**（可在控制选项里改绑）或从 Mod Menu 打开配置界面，
可以：
- 总开关 / 单项开关（血条、等级、稀有度）
- 渲染距离滑条
- 逐个稀有度调色
- 直接编辑等级/血量的解析正则（Hypixel 文本格式变化时用这个补救，
  不需要重新编译）
