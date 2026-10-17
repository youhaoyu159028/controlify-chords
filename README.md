# Controlify Chords

给 [Controlify](https://modrinth.com/mod/controlify) 增加**有序和弦（ordered chord）**的 Fabric 客户端附属模组。

> 跟原仓库的 skeleton 相比，这一版补上了：和弦状态机、8 个专用和弦键、Cloth Config 配置界面、命令/按键/自定义动作分发，以及修好的构建链。

## 目标基线

| 项目 | 版本 |
|---|---|
| Minecraft | **26.2**（非混淆版本，代码用 Mojang 官方名） |
| Fabric Loader | **0.19.3**（最低，向上兼容） |
| Controlify | **3.5.3+mc26.2** |
| Java | **25** |
| Gradle | **9.8.1** |
| Loom | **1.18.4**，插件 id `net.fabricmc.fabric-loom` |

`minecraft` 依赖写的是 `>=26.2`，为后续版本留出向上兼容空间；但**新版本必须经过 CI + 实机验证**才敢宣称支持。

---

## 和弦语义

1. **修饰键有序**：`LB → RB → A` 与 `RB → LB → A` 是两条不同的和弦。
2. **修饰键要长按**：至少按住 `minModifierHoldTicks`（默认 3 tick）。
3. **触发键要轻按**：在修饰键全部按住后按下触发键，再在 `maxTriggerTapTicks`（默认 8 tick）内松开。
4. **松开时执行**：动作在触发键松开的那一刻执行，一次按住只执行一次。
5. **松开任意修饰键 = 取消**：不会触发，也不会残留状态。
6. **同一个键可以兼任**：在 A 和弦里当修饰键、在 B 和弦里当触发键，互不影响；但**同一个和弦内**触发键不能同时是修饰键（物理上无法“先按住再轻按同一个键”）。

状态机：

```
IDLE
  └─ 修饰键按配置顺序依次按下 → 顺序匹配
      └─ 全部按住且 ≥ minHold
          └─ 触发键 justPressed → PENDING
              ├─ 触发键 justReleased 且 ≤ maxTap → 执行动作
              ├─ 触发键按住 > maxTap → 取消
              └─ 任意修饰键松开 → 取消
```

---

## 安装

1. 安装 Fabric Loader 0.19.3+、Fabric API、Controlify 3.5.3+、Cloth Config 26.2+。
2. 把构建出的 `controlify-chords-0.2.0.jar` 放进 `mods/`。
3. 启动游戏，进入 **Controlify → 手柄设置 → 组合键分类**，把 `和弦键 1..8` 绑到你想用的按钮。
4. 打开本模组配置（Mod Menu → Controlify Chords，或直接改 `config/controlify-chords.json`）。

---

## 配置

`config/controlify-chords.json`：

```json
{
  "schema": 1,
  "debug": false,
  "chords": [
    {
      "enabled": true,
      "name": "LB → RB → A 交换副手",
      "modifiers": ["controlifychords:key_1", "controlifychords:key_2"],
      "trigger": "controlifychords:key_3",
      "actionType": "key_mapping",
      "actionValue": "key.swapOffhand",
      "minModifierHoldTicks": 3,
      "maxTriggerTapTicks": 8,
      "cooldownTicks": 10
    }
  ]
}
```

外层固定 8 个槽位，少写会自动补齐，多写会被截断。

### 字段

| 字段 | 说明 |
|---|---|
| `modifiers` | 有序修饰键，1–4 个，不能重复；顺序即按下顺序 |
| `trigger` | 触发键；不能出现在同一和弦的 `modifiers` 里 |
| `actionType` | `key_mapping` / `command` / `custom` |
| `actionValue` | 按键名（如 `key.swapOffhand`）、命令（如 `/spawn`）或自定义动作 ID |
| `minModifierHoldTicks` | 修饰键最短按住时间，0–40 |
| `maxTriggerTapTicks` | 触发键最长轻按时间，1–40 |
| `cooldownTicks` | 冷却，0–200 |

### 可用绑定 ID

- 本模组：`controlifychords:key_1` … `controlifychords:key_8`（推荐，专用键）
- Controlify：`controlify:jump`、`controlify:sneak`、`controlify:attack` 等
- 查看全部：日志开启 `debug` 后，启动时会打印 Controlify 暴露的绑定 ID 数量；也可以用 `ChordBindings.knownIds()` 扩展成界面列表。

---

## 已知限制（重要）

### 1. 普通绑定仍会触发（没有 Mixin）

Controlify 的公开 API **没有**“吞掉某个绑定输出”的能力（`InputBinding` 只有读取状态；`suppressed` 是内部字段，没有 setter）。因此：

- 如果你把 LB 同时绑到 `controlify:sneak` 和 `controlifychords:key_1`，按 LB 时**潜行和和弦会同时生效**；
- 想要严格“不重复触发”，请把和弦用的按钮**只**绑到 `controlifychords:key_N`，不要同时绑普通动作。

为什么不在 v0.1 用 Mixin：Mixin 需要改 Controlify 内部类，和“向上兼容”的目标直接冲突。等状态机稳定、确定要支持“复用普通按键”时，再单独做实验分支（会在 README 标注版本）。

### 2. 同一 tick 内的顺序无法区分

两个修饰键在同一 tick 按下时，顺序按内部遍历顺序决定；需要严格顺序时请错开至少 1 tick。

### 3. 只处理当前控制器

`ControlifyApi.getCurrentController()` 只返回当前活跃手柄；双手柄各按一半不会组成和弦。

### 4. 只在游戏内生效

和弦键声明了 `BindContext.IN_GAME`，且管理器在 `screen != null` 时跳过。

### 5. 版本兼容

`minecraft >=26.2` 只是声明，不代表 26.3/26.4 一定可用。升级流程：改 `gradle.properties` → CI → 实机验证 → 再改元数据。未来会用单独 PR 加多版本矩阵。

---

## 构建

```bash
gradle build        # 需要 Gradle 9.8.1 + JDK 25
```

产物：`build/libs/controlify-chords-0.2.0.jar`。

> 为什么不是 `./gradlew`：仓库目前没有 wrapper。CI 用 `gradle/actions/setup-gradle` 指定 9.5；本地请装 Gradle 9.5 或自己 `gradle wrapper --gradle-version 9.8.1`。

### 之前 CI 为什么失败

| 问题 | 后果 | 修复 |
|---|---|---|
| JDK 25 + Gradle 8.14.3 | Gradle 8.14 不支持运行在 Java 25 上（需要 9.1+），构建在启动阶段就失败 | 升到 Gradle 9.8.1 |
| `build.gradle` 没有 `mappings` | Loom 在混淆版本上会报错；26.x 是非混淆版本，本就不需要 mappings | 换用 `net.fabricmc.fabric-loom` 插件，去掉 mappings |
| 26.1–26.3 没有 Yarn 映射 | 无法按 Yarn 名写代码 | 26.x 用 Mojang 官方名（`Component`、`Identifier`、`KeyMapping`） |
| `loom 1.15-SNAPSHOT` | 只有 alpha，且旧 Loom 不认非混淆流程 | 用 Loom 1.18.4 + 新插件 id |

---

## 开发

```
src/main/java/dev/youhaoyu/controlifychords/
├── ControlifyChords.java              # 客户端入口
├── ControlifyChordsEntrypoint.java    # Controlify pre-init，注册 8 个键
├── chord/
│   ├── ChordBindings.java             # 绑定注册 / ID 解析
│   ├── ChordManager.java              # 有序状态机
│   ├── ChordDefinition.java
│   ├── ChordAction.java
│   └── ChordActionRegistry.java
├── config/
│   ├── ChordsConfig.java
│   ├── ConfigManager.java
│   └── ChordsScreen.java              # Cloth Config
├── compat/ModMenuIntegration.java
└── util/KeyMappingHelper.java
```

扩展自定义动作：

```java
ChordActionRegistry.register("mymod:drink_potion", client -> {
    // 你的逻辑
});
```

然后在配置里用 `actionType: "custom"`、`actionValue: "mymod:drink_potion"`。

---

## 许可证

MIT（见 `LICENSE`）。Controlify 为 LGPL-3.0，本模组只通过公开 API 调用、不打包其代码；HMCL 与本项目无关。
