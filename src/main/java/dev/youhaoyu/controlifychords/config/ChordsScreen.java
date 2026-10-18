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

/** Cloth Config 配置界面：一个通用页 + 每个和弦一页（两个列表：修饰键 / 触发键）。 */
public final class ChordsScreen {

    private ChordsScreen() {
    }

    public static Screen create(Screen parent) {
        ChordsConfig config = ConfigManager.get();

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("controlifychords.config.title"))
                .setSavingRunnable(() -> {
                    ConfigManager.save();
                    ChordManager.invalidate();
                });

        ConfigEntryBuilder entries = builder.entryBuilder();

        ConfigCategory general = builder.getOrCreateCategory(Component.translatable("controlifychords.config.general"));
        general.addEntry(entries
                .startBooleanToggle(Component.translatable("controlifychords.config.debug"), config.debug)
                .setDefaultValue(false)
                .setTooltip(Component.translatable("controlifychords.config.debug.tip"))
                .setSaveConsumer(value -> config.debug = value)
                .build());
        general.addEntry(entries
                .startTextDescription(Component.translatable("controlifychords.config.help"))
                .build());

        for (int i = 0; i < config.chords.size(); i++) {
            buildChord(builder, entries, config.chords.get(i), i);
        }

        return builder.build();
    }

    private static void buildChord(ConfigBuilder builder, ConfigEntryBuilder entries,
                                   ChordDefinition chord, int index) {
        ConfigCategory category = builder.getOrCreateCategory(
                Component.translatable("controlifychords.config.chord", index + 1));

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

        // 修饰键：一个列表，顺序无所谓，全部按住即可
        category.addEntry(entries
                .startStrList(Component.translatable("controlifychords.config.modifiers"),
                        new ArrayList<>(chord.modifiers))
                .setDefaultValue(new ArrayList<>())
                .setExpanded(true)
                .setTooltip(Component.translatable("controlifychords.config.modifiers.tip"))
                .setSaveConsumer(list -> chord.modifiers = clean(list))
                .build());

        // 触发键：一个列表，任意一个轻按都会触发同一动作
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

        category.addEntry(entries
                .startTextDescription(Component.translatable("controlifychords.config.available",
                        ChordBindings.idHint()))
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
