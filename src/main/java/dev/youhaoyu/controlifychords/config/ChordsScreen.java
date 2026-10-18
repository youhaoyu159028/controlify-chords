package dev.youhaoyu.controlifychords.config;

import dev.youhaoyu.controlifychords.chord.ChordBindings;
import dev.youhaoyu.controlifychords.chord.ChordDefinition;
import dev.youhaoyu.controlifychords.chord.ChordManager;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 配置界面：通用页（按键池 + 调试）+ 每条和弦一页 + 最后“新增和弦”一页。
 * 和弦条数不设固定上限，保存时把填了内容的新增页追加进配置。
 */
public final class ChordsScreen {

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
                .startStrList(Component.translatable("controlifychords.config.key_pool"),
                        new ArrayList<>(config.keyPool))
                .setDefaultValue(new ArrayList<>())
                .setExpanded(true)
                .setTooltip(Component.translatable("controlifychords.config.key_pool.tip"))
                .setSaveConsumer(list -> config.keyPool = clean(list))
                .build());

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
                category.addEntry(entries
                        .startTextDescription(Component.literal(chord.name))
                        .build());
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

            addChordEntries(category, entries, chord);

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
        addChordEntries(addPage, entries, extra);

        return builder.build();
    }

    private static void addChordEntries(ConfigCategory category, ConfigEntryBuilder entries, ChordDefinition chord) {
        category.addEntry(entries
                .startStrList(Component.translatable("controlifychords.config.modifiers"),
                        new ArrayList<>(chord.modifiers))
                .setDefaultValue(new ArrayList<>())
                .setExpanded(true)
                .setTooltip(Component.translatable("controlifychords.config.modifiers.tip"))
                .setSaveConsumer(list -> chord.modifiers = clean(list))
                .build());

        category.addEntry(entries
                .startStrList(Component.translatable("controlifychords.config.triggers"),
                        new ArrayList<>(chord.triggers))
                .setDefaultValue(new ArrayList<>())
                .setExpanded(true)
                .setTooltip(Component.translatable("controlifychords.config.triggers.tip"))
                .setSaveConsumer(list -> chord.triggers = clean(list))
                .build());

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
