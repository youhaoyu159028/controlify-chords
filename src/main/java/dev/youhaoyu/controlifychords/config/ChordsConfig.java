package dev.youhaoyu.controlifychords.config;

import dev.youhaoyu.controlifychords.chord.ChordDefinition;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/** 配置根对象。固定槽位，避免运行中增删 Controlify 绑定。 */
public final class ChordsConfig {

    public static final int CHORD_SLOTS = 8;
    public static final int MAX_MODIFIERS = 8;
    public static final int MAX_TRIGGERS = 8;

    public int schema = 1;
    public boolean debug = false;
    public List<ChordDefinition> chords = new ArrayList<>();

    public static ChordsConfig createDefault() {
        ChordsConfig config = new ChordsConfig();
        for (int i = 0; i < CHORD_SLOTS; i++) {
            ChordDefinition chord = new ChordDefinition();
            if (i == 0) {
                // 示例：按住 key_1（LB）+ key_2（RB）→ 轻按 key_3（A）→ 交换副手
                chord.enabled = false;
                chord.name = "LB + RB → A";
                chord.modifiers.add("controlifychords:key_1");
                chord.modifiers.add("controlifychords:key_2");
                chord.triggers.add("controlifychords:key_3");
                chord.actionType = ChordDefinition.ActionType.KEY_MAPPING;
                chord.actionValue = "key.swapOffhand";
            }
            config.chords.add(chord);
        }
        return config;
    }

    public void normalize() {
        if (chords == null) {
            chords = new ArrayList<>();
        }
        while (chords.size() < CHORD_SLOTS) {
            chords.add(new ChordDefinition());
        }
        if (chords.size() > CHORD_SLOTS) {
            chords = new ArrayList<>(chords.subList(0, CHORD_SLOTS));
        }
        for (ChordDefinition chord : chords) {
            chord.modifiers = clean(chord.modifiers, MAX_MODIFIERS);
            chord.triggers = clean(chord.triggers, MAX_TRIGGERS);

            // 0.3.x → 0.4.x 迁移：旧的单个 trigger 并入 triggers；
            // 旧版语义是“有序”，所以同时把 ordered 打开，保证老配置行为不变。
            if (chord.triggers.isEmpty() && chord.trigger != null && !chord.trigger.isBlank()) {
                chord.triggers.add(chord.trigger.trim());
                chord.ordered = true;
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
