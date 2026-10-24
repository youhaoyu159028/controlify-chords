package dev.youhaoyu.controlifychords.config;

import dev.youhaoyu.controlifychords.chord.ChordBindings;
import dev.youhaoyu.controlifychords.chord.ChordDefinition;
import dev.youhaoyu.controlifychords.chord.ChordManager;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 配置界面：
 * <ul>
 *   <li>通用页：修饰键池、触发键池（各自可捕获按钮填入）；</li>
 *   <li>和弦 N 页：4 个修饰键下拉 + 4 个触发键下拉，选项来自两个池；</li>
 *   <li>最后一页：新增和弦。</li>
 * </ul>
 */
public final class ChordsScreen {

    private static final int SLOTS = 4;

    private ChordsScreen() {
    }

    public static Screen create(Screen parent) {
        ChordsConfig config = ConfigManager.get();

        List<Boolean> deleteFlags = new ArrayList<>();
        for (int i = 0; i < config.chords.size(); i++) {
            deleteFlags.add(false);
        }
        ChordDefinition extra = new ChordDefinition();

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("controlifychords.config.title"))
                .setSavingRunnable(() -> {
                    for (int i = config.chords.size() - 1; i >= 0; i--) {
                        if (i < deleteFlags.size() && Boolean.TRUE.equals(deleteFlags.get(i))) {
                            config.chords.remove(i);
                        }
                    }
                    if (!extra.modifiers.isEmpty() || !extra.triggers.isEmpty()) {
                        config.chords.add(extra);
                    }
                    if (config.chords.isEmpty()) {
                        config.chords.add(new ChordDefinition());
                    }
                    ConfigManager.save();
                    ChordManager.invalidate();
                });

        ConfigEntryBuilder entries = builder.entryBuilder();

        ConfigCategory general = builder.getOrCreateCategory(Component.translatable("controlifychords.config.general"));

        general.addEntry(entries
                .startTextDescription(Component.translatable("controlifychords.config.pool.modifier.desc"))
                .build());
        general.addEntry(entries
                .startStrList(Component.translatable("controlifychords.config.pool.modifier"),
                        new ArrayList<>(config.modifierPool))
                .setDefaultValue(new ArrayList<>())
                .setExpanded(true)
                .setTooltip(Component.translatable("controlifychords.config.pool.tip"))
                .setSaveConsumer(list -> config.modifierPool = clean(list))
                .build());
        general.addEntry(new CaptureEntry(
                Component.translatable("controlifychords.capture.pool.modifier"),
                Component.translatable("controlifychords.capture.button"),
                Component.translatable("controlifychords.capture.prompt.modifier"),
                id -> addToPool(config.modifierPool, id),
                () -> Optional.empty()));

        general.addEntry(entries
                .startTextDescription(Component.translatable("controlifychords.config.pool.trigger.desc"))
                .build());
        general.addEntry(entries
                .startStrList(Component.translatable("controlifychords.config.pool.trigger"),
                        new ArrayList<>(config.triggerPool))
                .setDefaultValue(new ArrayList<>())
                .setExpanded(true)
                .setTooltip(Component.translatable("controlifychords.config.pool.tip"))
                .setSaveConsumer(list -> config.triggerPool = clean(list))
                .build());
        general.addEntry(new CaptureEntry(
                Component.translatable("controlifychords.capture.pool.trigger"),
                Component.translatable("controlifychords.capture.button"),
                Component.translatable("controlifychords.capture.prompt.trigger"),
                id -> addToPool(config.triggerPool, id),
                () -> Optional.empty()));

        general.addEntry(entries
                .startBooleanToggle(Component.translatable("controlifychords.config.debug"), config.debug)
                .setDefaultValue(false)
                .setTooltip(Component.translatable("controlifychords.config.debug.tip"))
                .setSaveConsumer(value -> config.debug = value)
                .build());

        general.addEntry(entries
                .startTextDescription(Component.translatable("controlifychords.config.help"))
                .build());
        general.addEntry(entries
                .startTextDescription(Component.translatable("controlifychords.config.available",
                        ChordBindings.idHint()))
                .build());

        for (int i = 0; i < config.chords.size(); i++) {
            ChordDefinition chord = config.chords.get(i);
            final int slot = i;
            ConfigCategory category = builder.getOrCreateCategory(
                    Component.translatable("controlifychords.config.chord", i + 1));

            if (!chord.name.isBlank()) {
                category.addEntry(entries.startTextDescription(Component.literal(chord.name)).build());
            }

            category.addEntry(entries
                    .startBooleanToggle(Component.translatable("controlifychords.config.enabled"), chord.enabled)
                    .setDefaultValue(false)
                    .setSaveConsumer(value -> chord.enabled = value)
                    .build());

            category.addEntry(entries
                    .startStrField(Component.translatable("controlifychords.config.name"), safe(chord.name))
                    .setDefaultValue("")
                    .setSaveConsumer(value -> chord.name = value == null ? "" : value.trim())
                    .build());

            addChordEntries(category, entries, chord, config);

            category.addEntry(entries
                    .startBooleanToggle(Component.translatable("controlifychords.config.delete"), false)
                    .setDefaultValue(false)
                    .setTooltip(Component.translatable("controlifychords.config.delete.tip"))
                    .setSaveConsumer(value -> deleteFlags.set(slot, value))
                    .build());
        }

        ConfigCategory addPage = builder.getOrCreateCategory(
                Component.translatable("controlifychords.config.chord.new"));
        addPage.addEntry(entries
                .startTextDescription(Component.translatable("controlifychords.config.chord.new.tip"))
                .build());
        addPage.addEntry(entries
                .startBooleanToggle(Component.translatable("controlifychords.config.enabled"), extra.enabled)
                .setDefaultValue(false)
                .setSaveConsumer(value -> extra.enabled = value)
                .build());
        addPage.addEntry(entries
                .startStrField(Component.translatable("controlifychords.config.name"), "")
                .setDefaultValue("")
                .setSaveConsumer(value -> extra.name = value == null ? "" : value.trim())
                .build());
        addChordEntries(addPage, entries, extra, config);

        return builder.build();
    }

    private static void addChordEntries(ConfigCategory category, ConfigEntryBuilder entries,
                                        ChordDefinition chord, ChordsConfig config) {
        // 修饰键：4 个下拉，选项来自“修饰键池”
        for (int i = 0; i < SLOTS; i++) {
            addDropdown(category, entries,
                    Component.translatable("controlifychords.config.modifier.slot", i + 1),
                    chord.modifiers, i, config.modifierPool);
        }

        // 触发键：4 个下拉，选项来自“触发键池”
        for (int i = 0; i < SLOTS; i++) {
            addDropdown(category, entries,
                    Component.translatable("controlifychords.config.trigger.slot", i + 1),
                    chord.triggers, i, config.triggerPool);
        }

        category.addEntry(entries
                .startEnumSelector(Component.translatable("controlifychords.config.action_type"),
                        ChordDefinition.ActionType.class,
                        chord.actionType == null ? ChordDefinition.ActionType.KEY_MAPPING : chord.actionType)
                .setDefaultValue(ChordDefinition.ActionType.KEY_MAPPING)
                .setSaveConsumer(value -> chord.actionType = value)
                .build());

        category.addEntry(entries
                .startStrField(Component.translatable("controlifychords.config.action_value"), safe(chord.actionValue))
                .setDefaultValue("")
                .setTooltip(Component.translatable("controlifychords.config.action_value.tip"))
                .setSaveConsumer(value -> chord.actionValue = value == null ? "" : value.trim())
                .build());

        category.addEntry(entries
                .startIntSlider(Component.translatable("controlifychords.config.min_hold"),
                        chord.minModifierHoldTicks, 0, 40)
                .setDefaultValue(3)
                .setSaveConsumer(value -> chord.minModifierHoldTicks = value)
                .build());

        category.addEntry(entries
                .startIntSlider(Component.translatable("controlifychords.config.max_tap"),
                        chord.maxTriggerTapTicks, 1, 40)
                .setDefaultValue(8)
                .setSaveConsumer(value -> chord.maxTriggerTapTicks = value)
                .build());

        category.addEntry(entries
                .startIntSlider(Component.translatable("controlifychords.config.cooldown"),
                        chord.cooldownTicks, 0, 200)
                .setDefaultValue(10)
                .setSaveConsumer(value -> chord.cooldownTicks = value)
                .build());
    }

    /** 一个下拉框：选项 = (空) + 池内按钮 + 当前值（兼容旧配置里不在池里的键）。 */
    private static void addDropdown(ConfigCategory category, ConfigEntryBuilder entries,
                                    Component label, List<String> target, int index, List<String> pool) {
        String current = index < target.size() && target.get(index) != null ? target.get(index) : "";

        List<String> options = new ArrayList<>();
        options.add("");
        for (String id : pool) {
            if (!options.contains(id)) {
                options.add(id);
            }
        }
        if (!current.isEmpty() && !options.contains(current)) {
            options.add(current);
        }

        category.addEntry(entries
                .startDropdownMenu(label, current, value -> value,
                        value -> value == null || value.isEmpty()
                                ? Component.translatable("controlifychords.config.empty")
                                : Component.literal(value))
                .setSelections(options)
                .setDefaultValue("")
                .setSaveConsumer(value -> setSlot(target, index, value))
                .build());
    }

    /** 写回固定下标；空串保留占位，由 normalize 统一清理。 */
    private static void setSlot(List<String> target, int index, String value) {
        while (target.size() <= index) {
            target.add("");
        }
        target.set(index, value == null ? "" : value);
    }

    private static void addToPool(List<String> pool, String id) {
        if (!pool.contains(id)) {
            pool.add(id);
        }
        ConfigManager.save();
        ChordManager.invalidate();
        Minecraft.getInstance().setScreenAndShow(ChordsScreen.create(null));
    }

    private static List<String> clean(List<String> input) {
        List<String> out = new ArrayList<>();
        if (input != null) {
            for (String value : input) {
                if (value == null) {
                    continue;
                }
                String trimmed = value.trim();
                if (!trimmed.isEmpty() && !out.contains(trimmed)) {
                    out.add(trimmed);
                }
            }
        }
        return out;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
