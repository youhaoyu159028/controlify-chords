package dev.youhaoyu.controlifychords.config;

import dev.youhaoyu.controlifychords.chord.ChordDefinition;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/** 配置根对象：不限和弦条数 + 修饰键池 / 触发键池。 */
public final class ChordsConfig {

    public static final int MAX_MODIFIERS = 4;
    public static final int MAX_TRIGGERS = 4;
    public static final int MAX_CHORDS = 128;
    public static final int MAX_POOL = 64;

    public int schema = 3;
    public boolean debug = false;

    /** 修饰键池：和弦里的“按住键”只能从这里选。 */
    public List<String> modifierPool = new ArrayList<>();

    /** 触发键池：和弦里的“轻按键”只能从这里选。 */
    public List<String> triggerPool = new ArrayList<>();

    /** 0.5–0.7 的旧字段，仅用于迁移。 */
    @Deprecated
    public List<String> keyPool = new ArrayList<>();

    public List<ChordDefinition> chords = new ArrayList<>();

    public static ChordsConfig createDefault() {
        ChordsConfig config = new ChordsConfig();

        // 修饰键池：适合“一直按住”的键
        config.modifierPool.add("controlify:button/dpad_up");
        config.modifierPool.add("controlify:button/dpad_down");
        config.modifierPool.add("controlify:button/dpad_left");
        config.modifierPool.add("controlify:button/dpad_right");
        config.modifierPool.add("controlify:button/left_shoulder");
        config.modifierPool.add("controlify:button/right_shoulder");
        config.modifierPool.add("controlify:button/left_stick");
        config.modifierPool.add("controlify:button/right_stick");

        // 触发键池：适合“轻按一下”的键
        config.triggerPool.add("controlify:button/south");
        config.triggerPool.add("controlify:button/east");
        config.triggerPool.add("controlify:button/west");
        config.triggerPool.add("controlify:button/north");
        config.triggerPool.add("controlify:button/start");
        config.triggerPool.add("controlify:button/back");

        ChordDefinition example = new ChordDefinition();
        example.enabled = false;
        example.name = "LB + A 交换副手";
        example.modifiers.add("controlify:button/left_shoulder");
        example.triggers.add("controlify:button/south");
        example.actionType = ChordDefinition.ActionType.KEY_MAPPING;
        example.actionValue = "key.swapOffhand";
        config.chords.add(example);

        config.chords.add(new ChordDefinition());
        return config;
    }

    public void normalize() {
        modifierPool = clean(modifierPool, MAX_POOL);
        triggerPool = clean(triggerPool, MAX_POOL);

        // 0.5–0.7 → 0.8 迁移：旧的单一 keyPool 同时并入两个池
        List<String> legacy = clean(keyPool, MAX_POOL);
        if (modifierPool.isEmpty() && !legacy.isEmpty()) {
            modifierPool = new ArrayList<>(legacy);
        }
        if (triggerPool.isEmpty() && !legacy.isEmpty()) {
            triggerPool = new ArrayList<>(legacy);
        }
        keyPool = new ArrayList<>();

        if (chords == null) {
            chords = new ArrayList<>();
        }
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
