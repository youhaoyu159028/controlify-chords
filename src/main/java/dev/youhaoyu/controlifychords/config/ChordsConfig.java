package dev.youhaoyu.controlifychords.config;

import dev.youhaoyu.controlifychords.chord.ChordDefinition;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/** 配置根对象：不限和弦条数 + 一个通用按键池。 */
public final class ChordsConfig {

    /** 单条和弦里修饰键/触发键的软上限，防止误粘贴超长列表。 */
    public static final int MAX_MODIFIERS = 8;
    public static final int MAX_TRIGGERS = 8;

    /** 和弦条数上限（真正限制只用于防御异常配置，正常使用远远用不到）。 */
    public static final int MAX_CHORDS = 128;

    public int schema = 2;
    public boolean debug = false;

    /**
     * 通用按键池：这里列出的按钮才能被和弦使用。
     * 留空表示不限制（兼容旧配置）。
     */
    public List<String> keyPool = new ArrayList<>();

    public List<ChordDefinition> chords = new ArrayList<>();

    public static ChordsConfig createDefault() {
        ChordsConfig config = new ChordsConfig();

        // 编号就是列表顺序，默认把标准手柄按钮全部预填，用户不需要手输 ID
        config.keyPool.add("controlify:button/dpad_up");        // 1
        config.keyPool.add("controlify:button/dpad_down");      // 2
        config.keyPool.add("controlify:button/dpad_left");      // 3
        config.keyPool.add("controlify:button/dpad_right");     // 4
        config.keyPool.add("controlify:button/south");          // 5  A
        config.keyPool.add("controlify:button/east");           // 6  B
        config.keyPool.add("controlify:button/west");           // 7  X
        config.keyPool.add("controlify:button/north");          // 8  Y
        config.keyPool.add("controlify:button/left_shoulder");  // 9  LB
        config.keyPool.add("controlify:button/right_shoulder"); // 10 RB
        config.keyPool.add("controlify:button/left_stick");     // 11 L3
        config.keyPool.add("controlify:button/right_stick");    // 12 R3
        config.keyPool.add("controlify:button/start");          // 13
        config.keyPool.add("controlify:button/back");           // 14
        config.keyPool.add("controlify:button/guide");          // 15
        config.keyPool.add("controlify:button/touchpad");       // 16

        // 一条示例（默认关闭），其余留空，由界面里的“新增”页自行添加
        ChordDefinition example = new ChordDefinition();
        example.enabled = false;
        example.name = "LB + A 交换副手";
        example.modifiers.add("#9");
        example.triggers.add("#5");
        example.actionType = ChordDefinition.ActionType.KEY_MAPPING;
        example.actionValue = "key.swapOffhand";
        config.chords.add(example);

        config.chords.add(new ChordDefinition());
        return config;
    }

    public void normalize() {
        keyPool = clean(keyPool, Integer.MAX_VALUE);

        if (chords == null) {
            chords = new ArrayList<>();
        }
        // 不再补齐/截断到固定 8 条；只做去空与上限保护
        chords.removeIf(chord -> chord == null);
        if (chords.isEmpty()) {
            chords.add(new ChordDefinition());
        }
        if (chords.size() > MAX_CHORDS) {
            chords = new ArrayList<>(chords.subList(0, MAX_CHORDS));
        }

        for (ChordDefinition chord : chords) {
            chord.modifiers = clean(chord.modifiers, MAX_MODIFIERS);
            chord.triggers = clean(chord.triggers, MAX_TRIGGERS);

            // 0.3.x → 0.4+ 迁移：旧的单个 trigger 并入 triggers
            if (chord.triggers.isEmpty() && chord.trigger != null && !chord.trigger.isBlank()) {
                chord.triggers.add(chord.trigger.trim());
            }
            chord.trigger = "";

            if (chord.actionType == null) {
                chord.actionType = ChordDefinition.ActionType.KEY_MAPPING;
            }
            if (chord.actionValue == null) {
                chord.actionValue = "";
            }
            chord.minModifierHoldTicks = clamp(chord.minModifierHoldTicks, 0, 40);
            chord.maxTriggerTapTicks = clamp(chord.maxTriggerTapTicks, 1, 40);
            chord.cooldownTicks = clamp(chord.cooldownTicks, 0, 200);
        }
    }

    private static List<String> clean(List<String> input, int max) {
        LinkedHashSet<String> set = new LinkedHashSet<>();
        if (input != null) {
            for (String id : input) {
                if (id == null) {
                    continue;
                }
                String trimmed = id.trim();
                if (!trimmed.isEmpty()) {
                    set.add(trimmed);
                }
            }
        }
        List<String> out = new ArrayList<>(set);
        return out.size() > max ? new ArrayList<>(out.subList(0, max)) : out;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
