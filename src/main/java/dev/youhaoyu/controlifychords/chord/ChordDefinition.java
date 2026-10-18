package dev.youhaoyu.controlifychords.chord;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * 一个“和弦”定义：修饰键集合 + 触发键集合。
 *
 * <p>语义：
 * <ul>
 *   <li>{@link #modifiers} 是**修饰键集合**，顺序不重要，只要全部按住即可；</li>
 *   <li>{@link #triggers} 是**触发键集合**，其中任意一个被“轻按”（按下再松开）都会触发同一个动作；</li>
 *   <li>松开任意修饰键会取消本次和弦；</li>
 *   <li>触发键在松开时执行动作，一次按住只执行一次。</li>
 * </ul>
 *
 * <p>成员是 Controlify 的绑定 ID 或物理按键 ID（{@code controlify:button/xxx}）。
 * 物理按键模式保留按钮原功能，绑定模式会占用按钮。
 */
public final class ChordDefinition {

    public boolean enabled = false;

    /** 仅用于界面显示。 */
    public String name = "";

    /** 修饰键集合，1..8 个，不能重复，顺序不重要。 */
    public List<String> modifiers = new ArrayList<>();

    /** 触发键集合，1..8 个；任意一个轻按即触发。 */
    public List<String> triggers = new ArrayList<>();

    /**
     * 0.3.x 的旧字段，只用于迁移。
     * {@link ChordsConfig#normalize()} 会把它并入 {@link #triggers} 并清空。
     */
    @Deprecated
    public String trigger = "";

    public ActionType actionType = ActionType.KEY_MAPPING;

    /**
     * KEY_MAPPING: 原版按键的 translation key（如 key.swapOffhand）
     * COMMAND: 命令（可带 /）
     * CUSTOM: 注册在 ChordActionRegistry 的动作 ID
     */
    public String actionValue = "";

    /** 所有修饰键都按住后，至少再保持多少 tick 才允许触发。 */
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
        c.triggers = new ArrayList<>(triggers == null ? List.of() : triggers);
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
