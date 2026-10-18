# 新手设置教程（0.5.0）

> 这份教程假设你**完全没配过**，从「装了什么」一路写到「按下去有反应」。
> 0.5.0 没有固定槽位：和弦条数不限，按钮从「通用按键池」里取。

---

## 0. 先花 30 秒看懂

一条和弦 = **两个列表**：

```
修饰键列表：十字上、LB        ← 这些要一直按住
触发键列表：A、B              ← 任意一个轻按一下

按住 十字上 + LB（先后随便）→ 轻按 A（或 B）→ 执行同一个动作
```

- **修饰键列表**：全部按住就成立，顺序默认随便。
- **触发键列表**：里面任意一个轻按都会触发同一动作。
- 中途松开任何一个修饰键 = 取消。
- 和弦条数**没有上限**，想配 10 条、20 条都行。

---

## 1. 确认 mods 里该有哪些文件

路径（Zalith 隔离版本）：

```
/storage/emulated/0/mc/.mc/versions/26.2/mods/
```

必须**同时**有这 5 个：

| 文件 | 作用 | 缺了会怎样 |
|---|---|---|
| `fabric-api-0.161.0+26.2.jar` | Fabric 基础 API | 加载失败 |
| `controlify-3.5.3+mc26.2-universal.jar` | 手柄支持本体 | 报 `depends controlify` |
| `cloth-config-26.2.155-fabric.jar` | 配置界面库 | 报 `depends cloth-config` |
| `yet_another_config_lib_v3-3.9.7+26.2-fabric.jar` | Controlify 的依赖 | Controlify 起不来 |
| `controlify-chords-0.5.0.jar` | 本模组 | 没有和弦功能 |

可选：`modmenu-20.0.3.jar`（有它主界面才有配置齿轮，不装也能用 F9/手柄）。

> 只能放一个版本的 `controlify-chords`，旧的要删掉。

**确认装对**：主界面 Mod Menu 里看到 `Controlify Chords 0.5.0`。

---

## 2. 打开配置界面

### 路线 A：手柄（推荐，不用键盘）

```
Controlify 设置 → 组合键 分类 → 把 Back / 触摸板 / L3 绑到「打开和弦设置（手柄）」
```

进世界后按它，弹出配置界面。

### 路线 B：键盘

进世界后按 `F9`（可在 `选项 → 控制 → 按键绑定 → 杂项` 改成别的，条目名“打开和弦设置”）。

> 主菜单按没用，必须进世界。

### 路线 C：Mod Menu

主界面 → Mods → Controlify Chords → 齿轮。

> “模组目录 / Mod Directory”不是 Mod Menu，不会有齿轮。

### 路线 D：直接改文件

```
/storage/emulated/0/mc/.mc/versions/26.2/config/controlify-chords.json
```

改完必须重启游戏。

---

## 3. 手把手配第一条

### 3.1 先在「通用」页建按键池

打开「通用」页，第一项是 **通用按键池**。点“添加”，把你要用的按钮逐个加进去，例如：

```
controlify:button/dpad_up
controlify:button/dpad_down
controlify:button/south
controlify:button/east
controlify:button/left_shoulder
controlify:button/right_shoulder
```

规则：**和弦只能用池里的按钮**。池为空表示不限制，但建议填上，填错 ID 时日志会直接告诉你“不在池里”。

### 3.2 再填「和弦 1」页

| 界面文字 | 填什么 |
|---|---|
| 启用 | 打开 |
| 显示名称 | `十字上 + A 交换副手` |
| 修饰键列表 | 添加一行：`controlify:button/dpad_up` |
| 触发键列表 | 添加一行：`controlify:button/south` |
| 动作类型 | `KEY_MAPPING` |
| 动作参数 | `key.swapOffhand` |
| 修饰键最短按住 | 3（默认） |
| 触发键最长轻按 | 8（默认） |
| 冷却 | 10（默认） |

两个列表的操作：列表默认展开，最后一行是“添加”按钮；点一下多一个空输入框，右侧有删除按钮。

填完点右下角 **Save & Quit**。Cloth Config 26.2 没有中文语言文件，按钮是英文：`Save & Quit` = 保存退出，`Discard Changes` = 放弃修改。

保存**立即生效**，不用重启。

### 3.3 新增更多和弦

最后一页永远叫 **「新增和弦」**。在里面填好修饰键、触发键、动作，保存后它就变成一条真正的和弦，下次打开会多出一个新的「新增和弦」页。

不填内容就保存 = 什么也不加，不会产生空条目。

想删掉某条：进那一条，把最下面的 **「删除这条和弦」** 打开，保存即可。

### 3.4 让“LB+RB”和“RB+LB”是两个操作

不用开关，直接配两条：

| 和弦 | 修饰键列表 | 触发键列表 | 按法 | 动作 |
|---|---|---|---|---|
| 1 | `left_shoulder`、`right_shoulder` | `south` | LB → RB → 轻按 A | 动作 1 |
| 2 | `right_shoulder`、`left_shoulder` | `east` | RB → LB → 轻按 B | 动作 2 |

- 同一组键出现两种不同顺序 → 自动按顺序区分；
- 只有一条时 → `LB→RB` 和 `RB→LB` 都算它，顺序随便。

> 同一 tick 按下的两个键无法分辨先后，会按列表顺序视为正确；错开一帧即可。

### 3.5 为什么不用去 Controlify 绑键

因为 `controlify:button/dpad_up` 是**手柄物理按键**，不是 Controlify 的“绑定”。

- 十字上的原功能原封不动；
- 模组直接读手柄原始按键状态，所以它既能当原功能，也能当“按住键”；
- 和弦成立期间会临时屏蔽这些按钮的普通动作，避免按 A 时跳跃也一起触发。

### 3.6 实际按法

```
按住十字上不放（约 0.15 秒）→ 轻按一下 A（马上松开）→ 交换副手
```

按住 A 不放不算触发。

---

## 4. 动作类型

| 类型 | 用途 | 动作参数 |
|---|---|---|
| `KEY_MAPPING` | 模拟按普通按键（小地图、背包、丢弃…） | `key.swapOffhand` |
| `COMMAND` | 执行命令 | `/spawn` |
| `CUSTOM` | 其他模组注册的动作 | `mymod:drink_potion` |

### 找按键名

打开 `.../versions/26.2/options.txt`，搜 `key_`：

```
key_key.jump:key.keyboard.space
key_key.swapOffhand:key.keyboard.f
key_key.xaero.minimap:key.keyboard.m
```

**去掉行首 `key_`，取冒号前那段**：

- `key_key.jump` → `key.jump`
- `key_key.swapOffhand` → `key.swapOffhand`
- `key_key.xaero.minimap` → `key.xaero.minimap`

常用：`key.jump`、`key.sneak`、`key.sprint`、`key.attack`、`key.use`、`key.inventory`、`key.drop`、`key.swapOffhand`、`key.chat`、`key.togglePerspective`。

> 小地图的按键名各版本不同，一定要自己确认，别照抄 `key.xaero.minimap`。

---

## 5. 三条可直接抄的配置

把 `chords` 数组里的某一条换成下面任意一个（同时确保这些按钮在 `keyPool` 里）。改完重启。

### 5.1 十字上 + A → 交换副手

```json
{
  "enabled": true,
  "name": "十字上 + A 交换副手",
  "modifiers": ["controlify:button/dpad_up"],
  "triggers": ["controlify:button/south"],
  "actionType": "key_mapping",
  "actionValue": "key.swapOffhand",
  "minModifierHoldTicks": 3,
  "maxTriggerTapTicks": 8,
  "cooldownTicks": 10
}
```

### 5.2 LB + RB 按住，A 或 B 都能开小地图

```json
{
  "enabled": true,
  "name": "LB + RB → A/B 小地图",
  "modifiers": ["controlify:button/left_shoulder", "controlify:button/right_shoulder"],
  "triggers": ["controlify:button/south", "controlify:button/east"],
  "actionType": "key_mapping",
  "actionValue": "key.xaero.minimap",
  "minModifierHoldTicks": 3,
  "maxTriggerTapTicks": 8,
  "cooldownTicks": 10
}
```

### 5.3 有序：LB→RB→A 与 RB→LB→B 两条

```json
[
  {
    "enabled": true,
    "name": "LB → RB → A",
    "modifiers": ["controlify:button/left_shoulder", "controlify:button/right_shoulder"],
    "triggers": ["controlify:button/south"],
    "actionType": "key_mapping",
    "actionValue": "key.swapOffhand",
    "minModifierHoldTicks": 3,
    "maxTriggerTapTicks": 8,
    "cooldownTicks": 10
  },
  {
    "enabled": true,
    "name": "RB → LB → B",
    "modifiers": ["controlify:button/right_shoulder", "controlify:button/left_shoulder"],
    "triggers": ["controlify:button/east"],
    "actionType": "key_mapping",
    "actionValue": "key.drop",
    "minModifierHoldTicks": 3,
    "maxTriggerTapTicks": 8,
    "cooldownTicks": 10
  }
]
```

---

## 6. 物理按键 ID 速查

| 手柄上的键 | ID |
|---|---|
| 十字上 / 下 / 左 / 右 | `controlify:button/dpad_up` / `dpad_down` / `dpad_left` / `dpad_right` |
| A（下）/ B（右）/ X（左）/ Y（上） | `controlify:button/south` / `east` / `west` / `north` |
| LB / RB | `controlify:button/left_shoulder` / `right_shoulder` |
| L3 / R3 | `controlify:button/left_stick` / `right_stick` |
| Start / Back | `controlify:button/start` / `back` |
| Guide | `controlify:button/guide` |

全小写；`_` 不能写成 `-`。

---

## 7. 配完不生效？按这个顺序查

### 第一步：看日志

```
/storage/emulated/0/mc/.mc/versions/26.2/logs/latest.log
```

搜 `[Chords]`。正常：

```
[Chords] 已装载 3 条和弦，监听 6 个按钮（按键池 12 个）
```

显示 `0 条和弦` 时，紧跟着的 warn 会说明原因：

| 日志 | 含义 | 怎么修 |
|---|---|---|
| `修饰键或触发键列表为空` | 两个列表至少各填一个 | 补上 |
| `列表里有重复项` | 同一个键写了两次 | 删掉重复 |
| `同一个键同时出现在两个列表里` | 一个键不能在同一条里兼任 | 换一个 |
| `含有非物理按键 ID` | 用了 `controlifychords:key_N` 或 `controlify:jump` | 改成 `controlify:button/xxx` |
| `不在“按键池”里` | 池里没有这个按钮 | 通用页加进去 |
| `与前面的和弦完全相同` | 两条内容一样 | 删掉一条 |

### 第二步：对照症状

| 症状 | 原因 | 修法 |
|---|---|---|
| 完全没反应 | 没勾“启用” | 打开启用并保存 |
| 没反应 | 修饰键没按够 3 tick | 按久一点，或把“最短按住”改 0 |
| 没反应 | 触发键按太久 | 轻按，0.4 秒内松开 |
| 没反应 | 先按了触发键 | 触发键必须在修饰键之后按 |
| 没反应 | ID 拼错 | 对照第 6 节 |
| 触发了两次 | 有两条重复和弦 | 删掉一条 |
| 按下修饰键时原动作先触发一次 | 起手那一帧还没判定成和弦（已知限制） | 一次性动作会明显，持续动作基本无感 |
| 改 JSON 没变化 | 不会热重载 | 重启游戏 |
| 界面打不开 | 没进世界 / 已开着别的界面 | 进世界、关掉界面再按 |
| 崩在加载界面 | 缺依赖 | 回第 1 节 |

### 第三步：开调试日志

通用页 → `调试日志` 打开 → 保存。之后每次触发都会写进日志。

---

## 8. 从 0.4.x 升级

- **`controlifychords:key_1..8` 已经移除**，所有成员改成 `controlify:button/xxx`（第 6 节）。
- 用到旧写法 `controlify:jump`、`controlifychords:key_N` 的和弦会被跳过，日志提示“含有非物理按键 ID”。
- 和弦条数不再限制 8 条；旧的 8 条会原样保留，可以继续加。
- 旧配置的 `trigger` 字段仍会自动并入 `triggers`。

---

## 9. 想删掉/回退

- 关掉某条：取消“启用”。
- 删掉某条：打开“删除这条和弦”并保存。
- 全部关掉：把 `controlify-chords-0.5.0.jar` 移出 `mods`。

---

## 10. 三句话总结

1. 先往「通用按键池」里加按钮，再到各和弦页引用；和弦条数不限。
2. 修饰键全部按住 + 触发键任意一个轻按；同一组键配了两种顺序时自动按顺序区分。
3. 不生效先看 `latest.log` 的 `[Chords]` 行，它会明确说哪条被跳过、为什么。
