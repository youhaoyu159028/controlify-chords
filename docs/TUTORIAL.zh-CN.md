# 新手设置教程（0.3.0）

> 这份教程假设你**完全没配过**，从「装了什么」一路写到「按下去有反应」。
> 全程不需要写代码；只有一处需要改文本，我会把整段 JSON 直接给你复制。

---

## 0. 先花 30 秒看懂它怎么工作

和弦 = **先按顺序按住几个键，再轻按最后一个键**。

```
按住 十字键上  →  保持按住  →  轻按 A（按下立刻松开）  →  执行你要的动作
```

- 顺序有意义：`十字上 → A` 和 `A → 十字上` 是两条不同的和弦。
- 中途松开任何一个键 = 取消，不会触发。
- 同一个键可以在这个和弦里当“按住的那个”，在另一个和弦里当“最后轻按的那个”。

---

## 1. 确认 mods 里到底该有哪些文件

进游戏前，先看你的 `mods` 文件夹。Zalith 的隔离版本路径是：

```
/storage/emulated/0/mc/.mc/versions/26.2/mods/
```

里面必须**同时**有这 5 个（缺一个都会在加载界面直接失败）：

| 文件（名字大致长这样） | 作用 | 缺了会怎样 |
|---|---|---|
| `fabric-api-0.161.0+26.2.jar` | Fabric 基础 API | 模组加载失败 |
| `controlify-3.5.3+mc26.2-universal.jar` | 手柄支持本体 | 报 `depends controlify` |
| `cloth-config-26.2.155-fabric.jar` | 配置界面库 | 报 `depends cloth-config` |
| `yet_another_config_lib_v3-3.9.7+26.2-fabric.jar` | Controlify 依赖的配置库 | Controlify 自己起不来 |
| `controlify-chords-0.3.0.jar` | 本模组 | 没有和弦功能 |

可选：`modmenu-20.0.3.jar`（有了才能在游戏里点开配置界面，**强烈建议装**）。

> 版本号不用一模一样，但主版本要对得上：MC 26.2、Controlify 3.5.3+、Cloth 26.2+。

**怎么确认装对了**：启动游戏 → 主界面 → `Mods`（Mod Menu）→ 列表里能看到 `Controlify Chords`，版本 `0.3.0`。

---

## 2. 打开配置界面

三条路，任选一条。**不需要 Mod Menu 也能用**。

### 路线 A：游戏内按 F9（最省事，0.3.1 起）

进世界后按 `F9`，直接弹出设置界面。

按键可以在 `选项 → 控制 → 按键绑定 → 杂项` 里改成别的（条目名“打开和弦设置”）。

> 注意：`F9` 只在**没有其他界面打开**时生效；在主菜单按没用，必须进世界。

### 路线 B：有 Mod Menu

```
主界面 → Mods → 找到 Controlify Chords → 点它右边的齿轮/配置按钮
```

> “模组目录 / Mod Directory”不是 Mod Menu，它只是列出模组文件，不会显示配置按钮。需要真正的 **Mod Menu** 才有齿轮。

打开后你会看到：左边是 `通用` 和 `和弦 1` … `和弦 8` 共 9 页。

### 路线 C：直接改文件（没有 Mod Menu 也没有 F9 时）

直接改文件：

```
/storage/emulated/0/mc/.mc/versions/26.2/config/controlify-chords.json
```

用任意文本编辑器打开（MT 管理器、ES 文件浏览器都行）。**改完必须重启游戏**，游戏不会自动读取外部修改。

第一次启动时这个文件会自动生成，里面已经有一条示例（默认是关闭状态）。

---

## 3. 手把手配第一条和弦

我们用最典型的例子：**按住十字键上，再轻按 A，触发“交换副手”**。

### 3.1 在配置界面里填这 6 个框

进入 `和弦 1` 这一页，从上往下依次是：

| 界面上的文字 | 填什么 | 说明 |
|---|---|---|
| 启用 | **打开** | 不打开这一条完全不生效 |
| 显示名称 | `十字上 + A 交换副手` | 随便写，只用于你自己看 |
| 修饰键 1（绑定 ID） | `controlify:button/dpad_up` | 十字键上 |
| 修饰键 2 | **留空** | 这条只需要一个修饰键 |
| 修饰键 3 | **留空** | 同上 |
| 修饰键 4 | **留空** | 同上 |
| 触发键（绑定 ID） | `controlify:button/south` | A 键（Xbox 布局） |
| 动作类型 | `KEY_MAPPING` | 意思是“模拟按一个原版按键” |
| 动作参数 | `key.swapOffhand` | 交换副手的按键名 |
| 修饰键最短按住（tick） | `3` | 默认值，不用改 |
| 触发键最长轻按（tick） | `8` | 默认值，不用改 |
| 冷却（tick） | `10` | 防止手抖连发 |

填完点右下角 **Save & Quit**（保存并退出）。

> Cloth Config 26.2 没有自带中文语言文件，所以底部按钮显示英文：`Save & Quit` = 保存并退出，`Discard Changes` = 放弃修改。**点错会丢配置**。

> 在配置界面保存是**立即生效**的（会重建和弦）；只有你手动改 `config/controlify-chords.json` 才需要重启游戏。

> 界面里是**文本框**，不是“按一下手柄自动录键”。所以你得把 ID 手动打进去，或者复制粘贴。这是目前的设计，不是坏了。

### 3.2 这个例子为什么不用去 Controlify 里绑键

因为 `controlify:button/dpad_up` 是**手柄物理按键**，不是 Controlify 的“绑定”。

- 十字键上在 Controlify 里的原功能**原封不动**；
- 模组直接读手柄的原始按键状态，所以它既能当原功能用，也能当和弦的“按住键”；
- 和弦成立期间，模组会临时屏蔽这两个按钮的普通动作，避免按 A 的时候跳跃也一起触发。

这就是 0.3.0 相对 0.2.0 最重要的变化，也是你之前“十字上被抢走”的原因——那时候用的是另一种模式（见第 7 节）。

### 3.3 实际按法

```
按住十字上不放（约 0.15 秒以上）→ 轻按一下 A（按下马上松开）→ 副手物品交换
```

注意是**轻按**：按下后要在 8 tick（约 0.4 秒）内松开。按住 A 不放不会触发。

---

## 4. 动作类型怎么选

`动作类型` 有三档，90% 的情况用第一档。

| 动作类型 | 用途 | 动作参数写什么 |
|---|---|---|
| `KEY_MAPPING` | 模拟按一个普通按键（小地图、背包、丢弃、交换副手…） | 按键名，如 `key.swapOffhand` |
| `COMMAND` | 执行一条命令 | 如 `/spawn`，带不带 `/` 都行 |
| `CUSTOM` | 其他模组注册的动作 | 动作 ID，如 `mymod:drink_potion` |

### 怎么找 `KEY_MAPPING` 要填的按键名

**方法一（最准）**：打开

```
/storage/emulated/0/mc/.mc/versions/26.2/options.txt
```

搜索 `key_`，会看到这种行：

```
key_key.jump:key.keyboard.space
key_key.swapOffhand:key.keyboard.f
key_key.xaero.minimap:key.keyboard.m
```

规则：**去掉行首的 `key_`，取冒号前的那一段。**

- `key_key.jump` → `key.jump`
- `key_key.swapOffhand` → `key.swapOffhand`
- `key_key.xaero.minimap` → `key.xaero.minimap`

**方法二**：游戏里 `选项 → 控制 → 按键绑定`，找到功能名；大部分原版功能的按键名就是 `key.` + 功能名。

常用原版按键名：

| 功能 | 按键名 |
|---|---|
| 跳跃 | `key.jump` |
| 潜行 | `key.sneak` |
| 疾跑 | `key.sprint` |
| 攻击 | `key.attack` |
| 使用 / 放置 | `key.use` |
| 打开背包 | `key.inventory` |
| 丢弃物品 | `key.drop` |
| 交换副手 | `key.swapOffhand` |
| 打开聊天 | `key.chat` |
| 切换视角 | `key.togglePerspective` |

> 小地图（Xaero / JourneyMap）的按键名**各版本可能不同**，一定要用上面的方法自己去 `options.txt` 里确认，别照抄。标了 `xaero` 只是示例。

---

## 5. 三条可以直接抄的配置

改文件的话，把 `chords` 数组里的某一条换成下面任意一个。改完重启游戏。

### 5.1 十字上 + A → 交换副手

```json
{
  "enabled": true,
  "name": "十字上 + A 交换副手",
  "modifiers": ["controlify:button/dpad_up"],
  "trigger": "controlify:button/south",
  "actionType": "key_mapping",
  "actionValue": "key.swapOffhand",
  "minModifierHoldTicks": 3,
  "maxTriggerTapTicks": 8,
  "cooldownTicks": 10
}
```

### 5.2 LB + RB 按住，再轻按 A → 打开小地图

```json
{
  "enabled": true,
  "name": "LB + RB + A 打开小地图",
  "modifiers": ["controlify:button/left_shoulder", "controlify:button/right_shoulder"],
  "trigger": "controlify:button/south",
  "actionType": "key_mapping",
  "actionValue": "key.xaero.minimap",
  "minModifierHoldTicks": 3,
  "maxTriggerTapTicks": 8,
  "cooldownTicks": 10
}
```

> `key.xaero.minimap` 必须换成你自己 `options.txt` 里的真实名字。

### 5.3 十字下长按 + 十字右 → 执行命令

```json
{
  "enabled": true,
  "name": "十字下 + 十字右 回城",
  "modifiers": ["controlify:button/dpad_down"],
  "trigger": "controlify:button/dpad_right",
  "actionType": "command",
  "actionValue": "/spawn",
  "minModifierHoldTicks": 5,
  "maxTriggerTapTicks": 8,
  "cooldownTicks": 20
}
```

---

## 6. 物理按键 ID 速查表

Xbox 布局为准，其他布局（PS/Switch）位置对应、ID 不变。

| 手柄上的键 | 填这个 |
|---|---|
| 十字上 / 下 / 左 / 右 | `controlify:button/dpad_up` / `dpad_down` / `dpad_left` / `dpad_right` |
| A（下）/ B（右）/ X（左）/ Y（上） | `controlify:button/south` / `east` / `west` / `north` |
| LB / RB | `controlify:button/left_shoulder` / `right_shoulder` |
| L3（左摇杆按下）/ R3（右摇杆按下） | `controlify:button/left_stick` / `right_stick` |
| Start / Back | `controlify:button/start` / `back` |
| Guide（西瓜键/PS 键） | `controlify:button/guide` |

暂时**不支持**摇杆方向和扳机行程（axis），只支持“按下/没按下”的普通按钮。

---

## 7. 两种模式，别混用

| | 物理按键模式（推荐） | 绑定 ID 模式（老） |
|---|---|---|
| 写法 | `controlify:button/dpad_up` | `controlifychords:key_1` |
| 要去 Controlify 绑键吗 | **不用** | 要，先把按钮绑到“和弦键 1” |
| 按钮原功能 | **保留** | 被占用，原功能没了 |
| 防双触发 | 有（自动屏蔽） | 没有 |

**规则**：同一条和弦里，要么全用物理按键，要么全用绑定 ID；混着写这条会被跳过（日志里有提示）。

老模式的写法长这样，只在你想“彻底占用某个按钮”时才用：

```json
"modifiers": ["controlifychords:key_1"],
"trigger": "controlifychords:key_2"
```

注意 `modifiers` 是数组（可以有多个），`trigger` 是单个字符串。

---

## 8. 配完不生效？按这个顺序查

### 第一步：看日志有没有装进去

打开

```
/storage/emulated/0/mc/.mc/versions/26.2/logs/latest.log
```

搜索 `[Chords]`。正常会有一行：

```
[Chords] 已装载 1 个和弦（物理按键 2 个，绑定 0 个）
```

- 显示 `0 个和弦`：说明你的和弦没启用，或被判定为非法。
- 出现 `已启用但缺少修饰键或触发键，跳过`：有框没填。
- 出现 `混用了物理按键与绑定 ID，跳过`：见第 7 节。
- 出现 `触发键同时是修饰键，跳过`：触发键不能和修饰键相同。
- 出现 `与前面的和弦完全相同，跳过`：两条配置一模一样。

### 第二步：对照症状

| 症状 | 最可能的原因 | 怎么修 |
|---|---|---|
| 按了完全没反应 | 和弦没勾“启用” | 打开启用并点右下角保存（界面保存立即生效，不用重启） |
| 按了没反应 | 修饰键没按够 3 tick | 按住稍久一点，或把“最短按住”改成 0 |
| 按了没反应 | 触发键按太久 | 轻按，0.4 秒内松开；或把“最长轻按”改成 15 |
| 按了没反应 | 顺序反了 | 严格按 `修饰键 1 → 2 → 3` 的顺序按 |
| 按了没反应 | 两个键同一瞬间按下 | 错开一点点，先按住修饰键再按触发键 |
| 触发了两次 | 触发键本身还绑着别的动作 | 物理按键模式下已自动屏蔽；仍出现就检查是不是有两条相同和弦 |
| 按下修饰键时原动作先触发一次 | 已知限制：起手那一帧还没判定成和弦 | 一次性动作（开地图）会明显；持续动作（视角）基本无感 |
| 配置界面打不开 | 没装 Mod Menu | 装 Mod Menu，或直接改 JSON |
| 改了 JSON 没变化 | 游戏不会热重载 | 重启游戏 |
| 游戏崩在加载界面 | 缺依赖 | 回第 1 节对一遍 5 个 jar |
| 手柄没反应，键鼠正常 | Controlify 没识别到手柄 | 先去 Controlify 设置里确认手柄已连接、能操作菜单 |

### 第三步：开调试日志

配置界面 → `通用` → `调试日志` 打开 → 保存 → 重启。

之后每次和弦状态变化都会写进 `latest.log`，能直接看出“卡在哪一步”。

---

## 9. 想删掉/回退

- 临时关掉某条：配置里取消“启用”，不用删文件。
- 全部关掉：把 `controlify-chords-0.3.0.jar` 移出 `mods`。
- 回退到 0.2.0（只有绑定 ID 模式、没有 Mixin）：删掉 0.3.0 的 jar，放回 0.2.0，配置里改用 `controlifychords:key_N` 写法。

---

## 10. 三句话总结

1. 物理按键模式：**配置里直接写 `controlify:button/xxx`，不用去 Controlify 绑任何东西**，原功能还在。
2. 一条和弦 = 1~4 个修饰键（按顺序按住）+ 1 个触发键（轻按），动作在触发键松开时执行。
3. 不生效先看 `latest.log` 里的 `[Chords]` 那几行，它会明确告诉你哪条被跳过、为什么。
