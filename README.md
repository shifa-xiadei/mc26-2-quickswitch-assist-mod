# quickswitch-assist

**秒切。**
**Instant weapon swap.**

纯客户端 Fabric 模组：不改攻击流程，只在条件满足时**动态把目标槽位的按键改成攻击键**，
由原版按键系统先切槽再攻击，因此换槽包与真人按数字键完全一致。

## 版本要求

| 项目 | 值 |
| --- | --- |
| Minecraft | **26.2**（Fabric） |
| Fabric Loader | 0.19.5+ |
| Fabric API | 0.161.0+26.2（必需） |
| ModMenu | 20.0.3（可选，仅用于打开配置界面） |
| Java | **25** |
| Fabric Loom | 1.17.20 |

> 26.1 起官方不再混淆 Minecraft，Fabric 也不再维护 Yarn 映射：
> 本项目使用游戏里的官方名字，`build.gradle` 里**没有** `mappings` 依赖，
> 依赖写法是 `implementation` 而不是 `modImplementation`。

## 构建

需要 **JDK 25 或更高版本**（26.2 的游戏本体就跑在 Java 25 上）。

**Windows**（脚本会自己找 JDK 25+ 和本机可用的 Gradle）：

```powershell
.\dev.ps1 build        # 用户版：quickswitch-assist-0.1.0+26.2.jar
.\dev.ps1 dev          # 开发版：quickswitch-assist-0.1.0+26.2-dev.jar（HUD 默认开、输出诊断日志）
.\dev.ps1 runClient    # 开发版直接跑客户端
.\dev.ps1 clean
```

**Linux / macOS / 任意平台**：

```sh
./gradlew build                              # 用户版
./gradlew build -Pedition=dev                # 开发版
./gradlew runClient -Pedition=dev            # 跑客户端
```

产物在 `build/libs/`。首次构建会由 Gradle 自动下载 Minecraft、Fabric Loader / API 等依赖；
如果所在网络需要代理，请在 `~/.gradle/gradle.properties` 或环境变量里配置，**不要**写进本仓库。

## 功能

- **秒切**：按情境（目标举盾 / 空中 / 下落中 / 距离 / 有无护甲 …）和候选武器自身的
  属性与附魔打分，选出这一 tick 该拿的武器，然后把那一格的快捷键临时换成攻击键。
- **优先级表**可在 ModMenu 的配置界面里点着改（情境 → 武器分值、手持 → 候选权重），
  存在 `config/quickswitch-assist.properties`，也可以直接编辑该文件。
- **破盾后续**：斧头把盾打掉后自动把重锤切到手上，之后不再干预。
- **调试 HUD**：开发版默认打开，显示当前情境与武装槽位。

## 目录结构

```
src/main/java/com/quickswitchassist/
├── QuickSwitchAssistClient.java      # 客户端入口：配置加载 + tick 事件 + 注册调试 HUD
├── compat/                      # ModMenu 配置界面
├── config/                      # 配置文件读写
├── mixin/                       # 两个 @Invoker（同步选中槽、刷新属性）
├── module/                      # 模块接口 + 注册表 + 秒切实现 + 优先级表
└── util/                        # 调试 HUD
```

## 许可证与来源 / License & Origin

### 本项目是衍生作品

**本项目基于 [CombatAssist](https://github.com/peirooden/combatassist/releases/tag/v0.1.0-1.21.11) 修改而来。**
原项目作者：**peirooden**，原版本：`v0.1.0-1.21.11`。

- **原项目采用的许可证：MIT License**
- 原许可证原文与原始版权声明（`Copyright (c) 2026 peirooden`）完整保留在
  [LICENSE](LICENSE) 中，**未作任何修改**；原始版权声明的副本另见 [NOTICE](NOTICE)。
- 原项目中与许可证/版权相关的声明文件（原项目仅有以下一个）：
  - `LICENSE` — MIT License，`Copyright (c) 2026 peirooden`

> This project is a modified version of **CombatAssist** by **peirooden**
> (<https://github.com/peirooden/combatassist/releases/tag/v0.1.0-1.21.11>).
> The original project is licensed under the **MIT License**. Its license text
> and copyright notice are kept verbatim in [LICENSE](LICENSE) (unmodified);
> a copy of the original notice is in [NOTICE](NOTICE).

### 我自己的版权声明（仅针对我修改的部分）

**Copyright (c) 2026 ShiFa** —— 仅针对本仓库中由我作出的修改部分，
同样以 **MIT License** 发布；来自原项目的部分仍归 `Copyright (c) 2026 peirooden` 所有。

### 修改概要

1. **版本移植**：从 Minecraft 1.21.11（Yarn 映射）移植到 **Minecraft 26.2**。
   26.1 起官方不再混淆，因此改用游戏官方名字，依赖写法由 `modImplementation`
   改为 `implementation`，并移除 `mappings` 依赖。
2. **API 适配**：适配 26.2 的 HUD、Screen/Gui、按键映射、物品数据组件、
   向量/包围盒 API，以及两个 Mixin 目标方法；语言级别与 mixin 兼容级别 21 → 25。
3. **更名与重新署名**：模组 id/名称 `combatassist` → `quickswitch-assist`，
   Java 包 `com.combatassist` → `com.quickswitchassist`，类名 `CombatAssist*`
   → `QuickSwitchAssist*`，资源命名空间与 mixin 配置同步更名，
   配置文件 `combatassist.properties` → `quickswitch-assist.properties`。
4. **构建工具链**：Loom 1.17.x、Gradle 9.7.1 wrapper、Java 25、
   Fabric API 0.161.0+26.2、ModMenu 20.0.3；补充跨平台构建说明与 `.gitattributes`。
5. **仓库清理**：移除源码/脚本中的本机路径与个人环境信息，改为可移植的通用配置。

除以上修改外，功能逻辑沿用原项目实现。

## 许可

- 原项目部分：MIT License，`Copyright (c) 2026 peirooden`（原文见 [LICENSE](LICENSE)，未修改）
- 本项目的修改部分：MIT License，`Copyright (c) 2026 ShiFa`（见 [NOTICE](NOTICE)）

## 1.21.11 → 26.2 移植要点

如果你也在做同样的迁移，这些是踩到的点：

- `MinecraftClient` → `Minecraft`，`PlayerInventory` → `Inventory`，`KeyBinding` → `KeyMapping`，
  `InputUtil` → `InputConstants`，`Box`/`Vec3d` → `AABB`/`Vec3`，`Text` → `Component`，
  `DrawContext` → `GuiGraphicsExtractor`，`DataComponentTypes` → `DataComponents`，
  `WeaponComponent` → `Weapon`。
- 26.2 把「当前界面」搬到 `Minecraft.gui`：`client.currentScreen` → `client.gui.screen()`，
  `client.setScreen(x)` → `client.gui.setScreen(x)`。
- 准星从 `client.crosshairTarget` 变成 `client.hitResult`。
- Screen 的渲染入口是 `extractRenderState(GuiGraphicsExtractor, …)`，关屏回调是 `onClose()`，
  重建控件是 `rebuildWidgets()`；画字用 `graphics.text(font, …)` / `graphics.centeredText(font, …)`。
- HUD 改用 `HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, id, (graphics, delta) -> …)`。
- `F1` 隐藏 HUD 的状态从 `options.hudHidden` 变成 `client.gui.hud.isHidden()`。
- `Vec3d.multiply(double)` → `Vec3.scale(double)`；`Box.stretch/expand` → `AABB.expandTowards/inflate`。
- 附魔 id 从 `entry.getIdAsString()` 改为 `Holder#getRegisteredName()`。
- 按钮尺寸：`ButtonWidget.builder(...).dimensions(...)` → `Button.builder(...).bounds(...)`。
- Mixin：`ClientPlayerInteractionManager#syncSelectedSlot` → `MultiPlayerGameMode#ensureHasSentCarriedItem`；
  `LivingEntity#updateAttributes` → `LivingEntity#refreshDirtyAttributes`。
- 语言级别与 mixin 兼容级别：`JAVA_21` → `JAVA_25`。

参考：[Porting to 26.2](https://docs.fabricmc.net/develop/porting/)、
[Migrating Mappings](https://docs.fabricmc.net/develop/porting/mappings/)。
