# Controlify Chords

给 [Controlify](https://modrinth.com/mod/controlify) 增加**有序和弦（ordered chord）**的 Fabric 客户端附属模组。

> 跟原仓库的 skeleton 相比，这一版补上了：和弦状态机、8 个专用和弦键、物理按键模式、Mixin 抑制、Cloth Config 配置界面、命令/按键/自定义动作分发，以及修好的构建链。
>
> **第一次用？直接看 [新手设置教程](docs/TUTORIAL.zh-CN.md)**（从安装到按键，含三条可抄的配置）。

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

1. 安装 Fabric Loader 0.19.3+、Fabric API、Controlify 3.5.3+、Cloth Config 26.2+（Controlify 还需要 YACL）。
2. 把构建出的 `controlify-chords-0.3.0.jar` 放进 `mods/`。
3. **物理按键模式（推荐）**：不需要在 Controlify 里绑任何东西，直接跳到第 4 步，在配置里写 `controlify:button/dpad_up` 这类 ID，按钮原功能保留。
   **绑定 ID 模式**：进入 **Controlify → 手柄设置 → 组合键分类**，把 `和弦键 1..8` 绑到按钮，再在配置里引用 `controlifychords:key_N`（该按钮会被占用）。
4. 打开本模组配置：**进世界后按 `F9`**（不需要 Mod Menu），或 Mod Menu → Controlify Chords，或直接改 `config/controlify-chords.json`。

详细步骤见 [新手设置教程](docs/TUTORIAL.zh-CN.md)。

---

## 两种成员写法（重要）

### A. 物理按键模式（推荐：保留原功能）

直接写 Controlify 的物理按键 ID，**不要**再把这个按钮绑到 `controlifychords:key_N`：

```json
"modifiers": ["controlify:button/dpad_up"],
"trigger": "controlify:button/north"
```

- 十字上仍然保留它原来的功能（Controlify 里原来的绑定不动）。
- 长按十字上 → 再轻按 north → 触发和弦。
- 和弦成立期间，这两个按钮的普通绑定会被 Mixin 临时抑制，避免双触发。
- 单独轻按十字上（没进入和弦）不会抑制，原功能正常。
- **注意**：开始和弦的那一帧，修饰键自己的原动作仍可能先触发一次（要先判断你是不是在起手和弦）。如果它是一次性动作（如打开地图），会感觉被误触；这种情况需要再做“按下缓冲 + 未成和弦时回放”，可以后续加。

常用按钮 ID：

| 按钮 | ID |
|---|---|
| 十字上/下/左/右 | `controlify:button/dpad_up` / `dpad_down` / `dpad_left` / `dpad_right` |
| A/B/X/Y（Xbox 布局） | `controlify:button/south` / `east` / `west` / `north` |
| LB / RB | `controlify:button/left_shoulder` / `right_shoulder` |
| L3 / R3 | `controlify:button/left_stick` / `right_stick` |
| Start / Back | `controlify:button/start` / `back` |

> 一个和弦里的修饰键和触发键必须全是物理按键，或全是绑定 ID，不能混用。

### B. 绑定 ID 模式（专用键）

```json
"modifiers": ["controlifychords:key_1"],
"trigger": "controlifychords:key_2"
```

需要先在 Controlify 里把两个按钮分别绑到 `和弦键 1`、`和弦键 2`。优点是完全不依赖 Mixin；缺点是按钮会被这个绑定占用。

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

- 物理按键：`controlify:button/dpad_up`、`controlify:button/south` 等（**推荐**，保留原功能，完整列表见教程第 6 节）
- 本模组专用键：`controlifychords:key_1` … `controlifychords:key_8`（需要在 Controlify 里手动绑定）
- Controlify 绑定：`controlify:jump`、`controlify:sneak`、`controlify:attack` 等（会与普通动作同时触发）

---

## 已知限制（重要）

### 1. 抑制只覆盖“和弦进行中”的按钮

0.3.0 起，物理按键模式会用 Mixin 把和弦进行中的按钮对应绑定标记为 `suppressed`，所以按触发键不会同时触发它自己的动作。但有两个前提：

- **起手那一帧**：修饰键刚按下时和弦还没成立，它自己的动作可能先触发一次。一次性动作（如“打开地图”）会明显，持续动作（视角）基本无感。彻底消除需要“按下缓冲 + 未成和弦时回放”，尚未实现。
- **绑定 ID 模式不抑制**：`controlifychords:key_N` 仍然依赖“这个按钮只服务于和弦”来避免双触发。

Mixin 目标是 Controlify 的内部类 `InputBindingImpl#pushState`，属于**版本敏感**实现：Controlify 大版本升级后若改动该方法，需要同步更新，否则可能不生效（不会崩，只是回到双触发）。

### 2. 同一 tick 内的顺序无法区分

两个修饰键在同一 tick 按下时，顺序按内部遍历顺序决定；需要严格顺序时请错开至少 1 tick。

### 3. 只处理当前控制器

`ControlifyApi.getCurrentController()` 只返回当前活跃手柄；双手柄各按一半不会组成和弦。

### 4. 只在游戏内生效

- 绑定 ID 模式：和弦键声明了 `BindContext.IN_GAME`。
- 物理按键模式：管理器要求 `client.player != null`，主菜单里不工作。

### 5. 物理按键模式只支持 button

`controlify:button/*` 是“按下/没按下”的按钮。摇杆方向（axis）、扳机行程（trigger）、手柄陀螺仪暂不支持；组合键成员也不能混用物理按键与绑定 ID。

### 6. 版本兼容

`minecraft >=26.2` 只是声明，不代表 26.3/26.4 一定可用。升级流程：改 `gradle.properties` → CI → 实机验证 → 再改元数据。未来会用单独 PR 加多版本矩阵。

---

## 构建

```bash
gradle build        # 需要 Gradle 9.8.1 + JDK 25
```

产物：`build/libs/controlify-chords-0.3.0.jar`。

> 为什么不是 `./gradlew`：仓库目前没有 wrapper。CI 用 `gradle/actions/setup-gradle` 指定 9.8.1；本地要么装 Gradle 9.8.1，要么先执行 `gradle wrapper --gradle-version 9.8.1`。

### 之前 CI 为什么失败

| 问题 | 后果 | 修复 |
|---|---|---|
| JDK 25 + Gradle 8.14.3 | Gradle 8.14 不支持运行在 Java 25 上（需要 9.1+），构建在启动阶段就失败 | 升到 Gradle 9.8.1 |
| `build.gradle` 没有 `mappings` | Loom 在混淆版本上会报错；26.x 是非混淆版本，本就不需要 mappings | 换用 `net.fabricmc.fabric-loom` 插件，去掉 mappings |
| 26.1–26.3 没有 Yarn 映射 | 无法按 Yarn 名写代码 | 26.x 用 Mojang 官方名（`Component`、`Identifier`、`KeyMapping`） |
| 非混淆 Loom 没有 `modImplementation` | 依赖配置报 `Could not find method modImplementation()` | 改用 `implementation` / `compileOnly` / `api` |
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
