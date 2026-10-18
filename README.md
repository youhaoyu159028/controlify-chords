# Controlify Chords

给 [Controlify](https://modrinth.com/mod/controlify) 增加**组合键**的 Fabric 客户端附属模组：按住一组修饰键，再轻按任意一个触发键，执行一个动作。

> **第一次用？先看 [新手设置教程](docs/TUTORIAL.zh-CN.md)。**

## 目标基线

| 项目 | 版本 |
|---|---|
| Minecraft | **26.2**（非混淆版本，代码用 Mojang 官方名） |
| Fabric Loader | **0.19.3**（最低，向上兼容） |
| Controlify | **3.5.3+mc26.2** |
| Java | **25** |
| Gradle | **9.8.1** |
| Loom | **1.18.4**，插件 id `net.fabricmc.fabric-loom` |

---

## 核心模型

一条和弦 = **修饰键列表** + **触发键列表**，成员都是物理按键 `controlify:button/xxx`。

1. 修饰键全部按住即成立，**顺序默认不重要**。
2. **自动顺序区分**：如果同一组修饰键被配置成多种顺序（`LB+RB` 和 `RB+LB` 各一条），这几条会自动按实际按下顺序区分，互不干扰。
3. 触发键列表里**任意一个**轻按都会触发同一动作。
4. 动作在触发键松开时执行，一次按住只执行一次；松开任意修饰键取消。
5. 触发键必须在修饰键全部按住之后按下。

例：

| 和弦 | modifiers | triggers | 结果 |
|---|---|---|---|
| 1 | `[LB, RB]` | `[A]` | 先按 LB 再按 RB，轻按 A → 动作 1 |
| 2 | `[RB, LB]` | `[B]` | 先按 RB 再按 LB，轻按 B → 动作 2 |

只配置和弦 1 时，`LB→RB→A` 与 `RB→LB→A` 都算和弦 1。

---

## 安装

1. 安装 Fabric Loader 0.19.3+、Fabric API、Controlify 3.5.3+、Cloth Config 26.2+（Controlify 还需要 YACL）。
2. 把 `controlify-chords-0.7.1.jar` 放进 `mods/`。
3. 打开配置：进世界后按 **F9**；或在 Controlify 里把按钮绑到「打开和弦设置（手柄）」；或 Mod Menu → Controlify Chords。
4. 在「通用」页的**按键池**里列出你要用的按钮，然后在各和弦页引用它们。

---

## 按键池与编号引用（0.6.0）

`通用` 页有一个“通用按键池”。默认已预填 16 个标准手柄按钮，**池的顺序就是编号**：

| 编号 | 按钮 | 编号 | 按钮 |
|---|---|---|---|
| 1–4 | 十字上/下/左/右 | 9–10 | LB / RB |
| 5–8 | A / B / X / Y | 11–12 | L3 / R3 |
| 13–16 | Start / Back / Guide / 触摸板 | | |

**也可以完全不输入**：每条和弦里有两个按钮「捕获修饰键」「捕获触发键」，点一下会进入捕获界面，按下手柄上的按钮就会自动写入列表（并自动加进按键池）。

和弦的 `modifiers` / `triggers` 也可以手写：

- `#9` 或 `9`：池里第 9 个按钮（不必手输长 ID）；
- `controlify:button/left_shoulder`：完整 ID，仍然支持。

规则与注意：

- 只有池里的按钮才能被和弦使用；池为空表示不限制。
- 和弦条数不限；界面最后一页永远是「新增和弦」，每条底部有「删除这条和弦」。
- **池顺序就是编号**：在中间插入或调整顺序，已有和弦的 `#N` 会指向别的按钮。只在末尾追加最安全；要重排就先改成完整 ID。

为什么要有池：它是“这台手柄上我允许用于和弦的按钮清单”，既能省掉手输，也能让日志明确指出“这个按钮不在池里”。

---

## 配置

`config/controlify-chords.json`：

```json
{
  "schema": 2,
  "debug": false,
  "keyPool": [
    "controlify:button/dpad_up",
    "controlify:button/dpad_down",
    "controlify:button/south",
    "controlify:button/east",
    "controlify:button/left_shoulder",
    "controlify:button/right_shoulder"
  ],
  "chords": [
    {
      "enabled": true,
      "name": "LB + A 交换副手",
      "modifiers": ["#9"],
      "triggers": ["#5"],
      "actionType": "key_mapping",
      "actionValue": "key.swapOffhand",
      "minModifierHoldTicks": 3,
      "maxTriggerTapTicks": 8,
      "cooldownTicks": 10
    }
  ]
}
```

### 字段

| 字段 | 说明 |
|---|---|
| `keyPool` | 通用按键池；只有池内按钮可用于和弦，留空表示不限制 |
| `chords` | 和弦数组，**条数不限**（防御上限 128） |
| `modifiers` | 修饰键列表，1–8 个，不能重复；可写 `#9` 或完整 ID |
| `triggers` | 触发键列表，1–8 个；任意一个轻按都会触发；可写 `#5` 或完整 ID |
| `ordered` | 可选覆盖。默认自动：同一组修饰键配置了多种顺序时才按顺序区分；`true` 强制按顺序，`false` 强制不分顺序 |
| `actionType` | `key_mapping` / `command` / `custom` |
| `actionValue` | 按键名（如 `key.swapOffhand`）、命令（如 `/spawn`）或自定义动作 ID |
| `minModifierHoldTicks` | 修饰键最短按住时间，0–40 |
| `maxTriggerTapTicks` | 触发键最长轻按时间，1–40 |
| `cooldownTicks` | 冷却，0–200 |

配置界面保存后立即生效；手动改 JSON 需要重启游戏。

---

## 动作类型

| 类型 | 用途 | `actionValue` |
|---|---|---|
| `KEY_MAPPING` | 模拟按普通按键 | `key.jump`、`key.xaero.minimap` 等 |
| `COMMAND` | 执行命令 | `/spawn` |
| `CUSTOM` | 其他模组注册的动作 | `mymod:drink_potion` |

找按键名：`.minecraft/options.txt` 里的 `key_key.jump:key.keyboard.space` → 去掉行首 `key_`，取冒号前 → `key.jump`。

---

## 已知限制

### 1. 抑制只覆盖“和弦进行中”的按钮

物理按键模式通过 Mixin 把和弦进行中的按钮对应绑定标记为 `suppressed`，避免按触发键时它自己的动作也触发。但：

- **起手那一帧**：修饰键刚按下时和弦还没成立，它自己的动作可能先触发一次。一次性动作（如打开地图）会明显，持续动作（视角）基本无感。彻底消除需要“按下缓冲 + 未成和弦时回放”，尚未实现。
- Mixin 目标是 Controlify 内部类 `InputBindingImpl#pushState`，属**版本敏感**实现：Controlify 大版本升级后若改动该方法，可能退化为双触发（不会崩）。

### 2. 只支持 button

`controlify:button/*` 是“按下/没按下”的按钮。摇杆方向（axis）、扳机行程、陀螺仪暂不支持。

### 3. 只处理当前控制器

`ControlifyApi.getCurrentController()` 只返回当前活跃手柄；双手柄各按一半不会组成和弦。

### 4. 只在游戏内生效

管理器要求 `client.player != null`，主菜单里不工作。

### 5. 同一 tick 内无法分辨先后

同一 tick 按下的两个修饰键，会按列表顺序视为正确。错开一帧即可。

### 6. 版本兼容

`minecraft >=26.2` 只是声明，不代表 26.3/26.4 一定可用。升级流程：改 `gradle.properties` → CI → 实机验证。

---

## 从 0.5.x / 0.4.x 升级

0.6.0 新增按键池编号，旧配置里的完整 ID 仍然有效，无需改动。

0.5.0 **移除了** 8 个专用和弦键 `controlifychords:key_1..8` 和“绑定 ID 模式”，成员统一使用 `controlify:button/xxx`。

- 旧配置里用到 `controlifychords:key_N` 或 `controlify:jump` 这类绑定 ID 的和弦会被跳过，日志提示“含有非物理按键 ID”。
- 需要把这些和弦的成员改成对应的物理按钮 ID（第 6 节有对照表）。
- 旧配置的 `trigger` 字段仍会自动并入 `triggers`；和弦条数不再被截断到 8 条。

---

## 构建

```bash
gradle build        # 需要 Gradle 9.8.1 + JDK 25
```

产物：`build/libs/controlify-chords-0.7.1.jar`。

> 仓库没有 wrapper。CI 用 `gradle/actions/setup-gradle` 指定 9.8.1；本地要么装 Gradle 9.8.1，要么先执行 `gradle wrapper --gradle-version 9.8.1`。

### 之前 CI 为什么失败

| 问题 | 后果 | 修复 |
|---|---|---|
| JDK 25 + Gradle 8.14.3 | Gradle 8.14 不支持运行在 Java 25 上（需要 9.1+） | 升到 Gradle 9.8.1 |
| `build.gradle` 没有 `mappings` | 混淆版本上会报错；26.x 非混淆版本本就不需要 | 换成 `net.fabricmc.fabric-loom` |
| 26.1–26.3 没有 Yarn 映射 | 无法按 Yarn 名写代码 | 26.x 用 Mojang 官方名 |
| 非混淆 Loom 没有 `modImplementation` | `Could not find method modImplementation()` | 改用 `implementation` / `compileOnly` / `api` |

---

## 开发

```
src/main/java/dev/youhaoyu/controlifychords/
├── ControlifyChords.java              # 客户端入口 + F9 开设置
├── ControlifyChordsEntrypoint.java    # Controlify pre-init，注册“打开设置”绑定
├── chord/
│   ├── ChordBindings.java             # 物理按键判定 / 打开设置绑定
│   ├── ChordManager.java              # 两列表状态机 + 自动顺序区分
│   ├── ChordDefinition.java
│   ├── ChordAction.java
│   └── ChordActionRegistry.java
├── config/
│   ├── ChordsConfig.java              # 不限条数 + 按键池
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

MIT（见 `LICENSE`）。Controlify 为 LGPL-3.0，本模组只通过公开 API 调用、不打包其代码。
