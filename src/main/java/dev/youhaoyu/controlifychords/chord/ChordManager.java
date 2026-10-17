package dev.youhaoyu.controlifychords.chord;

import dev.isxander.controlify.api.ControlifyApi;
import dev.isxander.controlify.api.bind.InputBinding;
import dev.isxander.controlify.api.bind.InputBindingSupplier;
import dev.isxander.controlify.controller.ControllerEntity;
import dev.youhaoyu.controlifychords.ControlifyChords;
import dev.youhaoyu.controlifychords.config.ChordsConfig;
import dev.youhaoyu.controlifychords.config.ConfigManager;
import dev.youhaoyu.controlifychords.util.KeyMappingHelper;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 有序和弦状态机。
 *
 * <pre>
 * 1. 记录每个参与绑定的按下顺序（仅保留当前仍按住的键）
 * 2. 对每个和弦，取出“该和弦修饰键”的当前顺序，必须与配置完全一致
 * 3. 修饰键全部按住且超过 minModifierHoldTicks 后，触发键 justPressed → 进入 pending
 * 4. 触发键 justReleased，且按下时长 ≤ maxTriggerTapTicks → 执行动作
 * 5. 任意修饰键松开 / 触发键按太久 → 取消本次
 * </pre>
 */
public final class ChordManager {

    private static final List<Runtime> RUNTIMES = new ArrayList<>();
    private static final List<InputBindingSupplier> WATCH = new ArrayList<>();
    private static final Map<String, Integer> PRESS_TICKS = new LinkedHashMap<>();
    private static String controllerUid;
    private static boolean dirty = true;
    private static int tickCounter;

    private ChordManager() {
    }

    public static void invalidate() {
        dirty = true;
    }

    public static void tick(Minecraft client) {
        tickCounter++;
        KeyMappingHelper.tick();

        if (dirty) {
            rebuild();
        }
        if (client.player == null) {
            return;
        }

        Optional<ControllerEntity> current = ControlifyApi.get().getCurrentController();
        if (current.isEmpty()) {
            return;
        }
        ControllerEntity controller = current.get();

        if (!controller.uid().equals(controllerUid)) {
            controllerUid = controller.uid();
            PRESS_TICKS.clear();
        }

        // 跨 tick 保留“首次按下”的时间，松开即移除
        for (InputBindingSupplier supplier : WATCH) {
            InputBinding binding = supplier.onOrNull(controller);
            String id = supplier.bindId().toString();
            if (binding != null && binding.digitalNow()) {
                PRESS_TICKS.putIfAbsent(id, tickCounter);
            } else {
                PRESS_TICKS.remove(id);
            }
        }

        // 真正的按下顺序：按首次按下 tick 排序；同一 tick 时保持 WATCH 顺序
        List<String> pressOrder = new ArrayList<>(PRESS_TICKS.keySet());
        pressOrder.sort((a, b) -> Integer.compare(PRESS_TICKS.get(a), PRESS_TICKS.get(b)));

        for (Runtime runtime : RUNTIMES) {
            runtime.tick(controller, pressOrder, PRESS_TICKS);
        }
    }

    private static void rebuild() {
        dirty = false;
        RUNTIMES.clear();
        WATCH.clear();
        PRESS_TICKS.clear();

        ChordsConfig config = ConfigManager.get();
        Set<String> watchIds = new LinkedHashSet<>();
        Set<String> signatures = new LinkedHashSet<>();

        for (int i = 0; i < config.chords.size(); i++) {
            ChordDefinition chord = config.chords.get(i);
            if (chord == null || !chord.enabled) {
                continue;
            }
            if (chord.modifiers.size() < 1 || chord.trigger == null || chord.trigger.isBlank()) {
                ControlifyChords.LOGGER.warn("[Chords] 和弦 #{} 已启用但缺少修饰键或触发键，跳过", i + 1);
                continue;
            }
            if (new LinkedHashSet<>(chord.modifiers).size() != chord.modifiers.size()) {
                ControlifyChords.LOGGER.warn("[Chords] 和弦 #{} 的修饰键有重复，跳过", i + 1);
                continue;
            }
            if (chord.modifiers.contains(chord.trigger)) {
                ControlifyChords.LOGGER.warn("[Chords] 和弦 #{} 的触发键同时是修饰键，跳过（同一个键无法先按住再轻按）", i + 1);
                continue;
            }

            String signature = chord.modifiers + "|" + chord.trigger;
            if (!signatures.add(signature)) {
                ControlifyChords.LOGGER.warn("[Chords] 和弦 #{} 与前面的和弦修饰键/触发键完全相同，跳过", i + 1);
                continue;
            }

            List<InputBindingSupplier> modifiers = new ArrayList<>();
            boolean missing = false;
            for (String id : chord.modifiers) {
                Optional<InputBindingSupplier> supplier = ChordBindings.supplier(id);
                if (supplier.isEmpty()) {
                    ControlifyChords.LOGGER.warn("[Chords] 和弦 #{} 的修饰键 '{}' 非法，跳过", i + 1, id);
                    missing = true;
                    break;
                }
                modifiers.add(supplier.get());
                watchIds.add(id);
            }
            if (missing) {
                continue;
            }
            Optional<InputBindingSupplier> trigger = ChordBindings.supplier(chord.trigger);
            if (trigger.isEmpty()) {
                ControlifyChords.LOGGER.warn("[Chords] 和弦 #{} 的触发键 '{}' 非法，跳过", i + 1, chord.trigger);
                continue;
            }
            watchIds.add(chord.trigger);

            RUNTIMES.add(new Runtime(i, chord, modifiers, trigger.get()));
        }

        for (String id : watchIds) {
            ChordBindings.supplier(id).ifPresent(WATCH::add);
        }
        ControlifyChords.LOGGER.info("[Chords] 已装载 {} 个和弦，监视 {} 个绑定", RUNTIMES.size(), WATCH.size());
    }

    private static final class Runtime {

        private final int index;
        private final ChordDefinition definition;
        private final List<InputBindingSupplier> modifiers;
        private final InputBindingSupplier trigger;
        private int pendingSince = -1;
        private int cooldownUntil = -1;

        private Runtime(int index, ChordDefinition definition,
                        List<InputBindingSupplier> modifiers, InputBindingSupplier trigger) {
            this.index = index;
            this.definition = definition;
            this.modifiers = modifiers;
            this.trigger = trigger;
        }

        private void tick(ControllerEntity controller, List<String> pressOrder, Map<String, Integer> pressedAt) {
            if (tickCounter < cooldownUntil) {
                return;
            }

            InputBinding triggerBinding = trigger.onOrNull(controller);
            if (triggerBinding == null) {
                return;
            }

            // 修饰键是否仍全部按住
            boolean allHeld = true;
            for (InputBindingSupplier supplier : modifiers) {
                InputBinding binding = supplier.onOrNull(controller);
                if (binding == null || !binding.digitalNow()) {
                    allHeld = false;
                    break;
                }
            }

            // 当前顺序：只保留本和弦的修饰键，按按下先后排列
            List<String> current = new ArrayList<>();
            for (String id : pressOrder) {
                if (definition.modifiers.contains(id)) {
                    current.add(id);
                }
            }
            int firstPressTick = current.isEmpty() ? Integer.MAX_VALUE : pressedAt.get(current.get(0));
            boolean orderMatches = current.equals(definition.modifiers);

            if (pendingSince >= 0) {
                if (!allHeld || !orderMatches) {
                    pendingSince = -1;
                    return;
                }
                if (triggerBinding.justReleased()) {
                    int tapTicks = tickCounter - pendingSince;
                    if (tapTicks <= definition.maxTriggerTapTicks) {
                        fire();
                    }
                    pendingSince = -1;
                } else if (tickCounter - pendingSince > definition.maxTriggerTapTicks) {
                    // 按太久，不是“轻按”
                    pendingSince = -1;
                }
                return;
            }

            if (!allHeld || !orderMatches) {
                return;
            }
            if (tickCounter - firstPressTick < definition.minModifierHoldTicks) {
                return;
            }
            if (triggerBinding.justPressed()) {
                pendingSince = tickCounter;
                if (ControlifyChords.LOGGER.isDebugEnabled()) {
                    ControlifyChords.LOGGER.debug("[Chords] #{} 进入待触发，等待松开触发键", index + 1);
                }
            }
        }

        private void fire() {
            cooldownUntil = tickCounter + definition.cooldownTicks;
            Minecraft client = Minecraft.getInstance();
            if (definition.actionType == null) {
                return;
            }
            switch (definition.actionType) {
                case KEY_MAPPING -> {
                    if (!KeyMappingHelper.tap(definition.actionValue, 1)) {
                        ControlifyChords.LOGGER.warn("[Chords] #{} 无法触发按键 '{}'（不存在或未绑定）",
                                index + 1, definition.actionValue);
                    }
                }
                case COMMAND -> {
                    if (client.player != null) {
                        String command = definition.actionValue == null ? "" : definition.actionValue.trim();
                        if (command.startsWith("/")) {
                            command = command.substring(1);
                        }
                        if (!command.isEmpty()) {
                            client.player.connection.sendCommand(command);
                        }
                    }
                }
                case CUSTOM -> {
                    ChordAction action = ChordActionRegistry.get(definition.actionValue);
                    if (action != null) {
                        action.run(client);
                    } else {
                        ControlifyChords.LOGGER.warn("[Chords] 未找到自定义动作 '{}'", definition.actionValue);
                    }
                }
            }
            if (ControlifyChords.LOGGER.isDebugEnabled()) {
                ControlifyChords.LOGGER.debug("[Chords] 触发 #{} {} -> {}", index + 1,
                        definition.modifiers, definition.actionValue);
            }
        }
    }
}
