package dev.youhaoyu.controlifychords.chord;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * 一个“有序和弦”定义。
 *
 * <p>语义（与 README 一致）：
 * <ul>
 *   <li>modifiers 是**有序**的修饰键绑定 ID，必须按这个顺序依次按下并保持按住；</li>
 *   <li>trigger 是触发键，必须在修饰键全部按住后**轻按**（按下再松开）；</li>
 *   <li>松开任意修饰键会取消本次和弦；</li>
 *   <li>触发键在松开时执行动作，一次按住只执行一次。</li>
 * </ul>
 *
 * <p>成员是 Controlify 的绑定 ID。默认使用本模组注册的专用键
 * {@code controlifychords:key_1..8}，也可以填 {@code controlify:jump} 这类现有绑定
 * （但那样会同时触发原动作，见 README 的“重复触发”一节）。
 */
public final class ChordDefinition {

    public boolean enabled = false;

    /** 仅用于界面显示。 */
    public String name = "";

    /** 有序修饰键，1..4 个，不能重复。 */
    public List<String> modifiers = new ArrayList<>();

    /** 触发键。不能同时出现在 modifiers 里。 */
    public String trigger = "";

    public ActionType actionType = ActionType.KEY_MAPPING;

    /**
     * KEY_MAPPING: 原版按键的 translation key（如 key.swapOffhand）
     * COMMAND: 命令（可带 /）
     * CUSTOM: 注册在 ChordActionRegistry 的动作 ID
     */
    public String actionValue = "";

    /** 修饰键至少按住多少 tick 才算“长按修饰键”。 */
    public int minModifierHoldTicks = 3;

    /** 触发键从按下到松开最多多少 tick，超过视为长按而不是轻按。 */
    public int maxTriggerTapTicks = 8;

    /** 两次触发之间的冷却。 */
    public int cooldownTicks = 10;

    public ChordDefinition copy() {
        ChordDefinition c = new ChordDefinition();
        c.enabled = enabled;
        c.name = name;
        c.modifiers = new ArrayList<>(modifiers == null ? List.of() : modifiers);
        c.trigger = trigger;
        c.actionType = actionType;
        c.actionValue = actionValue;
        c.minModifierHoldTicks = minModifierHoldTicks;
        c.maxTriggerTapTicks = maxTriggerTapTicks;
        c.cooldownTicks = cooldownTicks;
        return c;
    }

    public enum ActionType {
        @SerializedName("key_mapping")
        KEY_MAPPING,
        @SerializedName("command")
        COMMAND,
        @SerializedName("custom")
        CUSTOM
    }
}
