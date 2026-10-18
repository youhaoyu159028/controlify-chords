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
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 和弦状态机：<b>修饰键集合</b> + <b>触发键集合</b>。
 *
 * <p>支持两种成员写法：
 * <ul>
 *   <li><b>物理按键</b>：{@code controlify:button/dpad_up}。直接读手柄状态，
 *       不需要把这个按钮绑到 {@code controlifychords:key_N}，所以它原来的功能保留。
 *       和弦进行时通过 Mixin 抑制这些按钮的普通绑定，避免双触发。</li>
 *   <li><b>绑定 ID</b>：{@code controlifychords:key_1}、{@code controlify:jump}。
 *       老模式，需要用户自己在 Controlify 里把按钮绑到该绑定。</li>
 * </ul>
 *
 * <p>语义：修饰键不分先后，全部按住即可；触发键集合里任意一个轻按都会触发同一动作。
 */
public final class ChordManager {

    private static final List<Runtime> RUNTIMES = new ArrayList<>();
    private static final List<InputBindingSupplier> WATCH_BINDINGS = new ArrayList<>();
    private static final List<Identifier> WATCH_RAW = new ArrayList<>();

    private static final Map<String, Integer> BINDING_PRESS_TICKS = new LinkedHashMap<>();
    private static final Map<Identifier, Integer> RAW_PRESS_TICKS = new LinkedHashMap<>();
    private static final Set<Identifier> SUPPRESSED = new HashSet<>();

    private static String controllerUid;
    private static boolean dirty = true;
    private static int tickCounter;

    private ChordManager() {
    }

    public static void invalidate() {
        dirty = true;
    }

    /** 由 ClientTickEvents 调用：驱动状态机与按键释放。 */
    public static void tick(Minecraft client) {
        tickCounter++;
        KeyMappingHelper.tick();

        if (dirty) {
            rebuild();
        }

        SUPPRESSED.clear();
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
            BINDING_PRESS_TICKS.clear();
            RAW_PRESS_TICKS.clear();
            RUNTIMES.forEach(Runtime::resetTransient);
        }

        for (InputBindingSupplier supplier : WATCH_BINDINGS) {
            InputBinding binding = supplier.onOrNull(controller);
            String id = supplier.bindId().toString();
            if (binding != null && binding.digitalNow()) {
                BINDING_PRESS_TICKS.putIfAbsent(id, tickCounter);
            } else {
                BINDING_PRESS_TICKS.remove(id);
            }
        }

        Set<Identifier> buttons = controller.input()
                .map(input -> input.stateNow().getButtons())
                .orElse(Set.of());
        for (Identifier id : WATCH_RAW) {
            if (buttons.contains(id)) {
                RAW_PRESS_TICKS.putIfAbsent(id, tickCounter);
            } else {
                RAW_PRESS_TICKS.remove(id);
            }
        }

        for (Runtime runtime : RUNTIMES) {
            runtime.tick(controller, buttons);
        }
    }

    /** Mixin 用：这个绑定是否应该在当前 tick 被抑制。 */
    public static boolean shouldSuppress(InputBinding binding) {
        if (SUPPRESSED.isEmpty() || binding == null) {
            return false;
        }
        if (binding.id().getNamespace().equals(ControlifyChords.MOD_ID)) {
            return false;
        }
        if (binding.isUnbound()) {
            return false;
        }
        try {
            for (Identifier id : binding.boundInput().getRelevantInputs()) {
                if (SUPPRESSED.contains(id)) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
            // 无有效输入
        }
        return false;
    }

    private static void rebuild() {
        dirty = false;
        RUNTIMES.clear();
        WATCH_BINDINGS.clear();
        WATCH_RAW.clear();
        BINDING_PRESS_TICKS.clear();
        RAW_PRESS_TICKS.clear();

        ChordsConfig config = ConfigManager.get();
        Set<String> signatures = new LinkedHashSet<>();

        for (int i = 0; i < config.chords.size(); i++) {
            ChordDefinition chord = config.chords.get(i);
            if (chord == null || !chord.enabled) {
                continue;
            }
            if (chord.modifiers.isEmpty() || chord.triggers.isEmpty()) {
                ControlifyChords.LOGGER.warn("[Chords] 和弦 #{} 已启用但修饰键或触发键列表为空，跳过", i + 1);
                continue;
            }
            if (new HashSet<>(chord.modifiers).size() != chord.modifiers.size()
                    || new HashSet<>(chord.triggers).size() != chord.triggers.size()) {
                ControlifyChords.LOGGER.warn("[Chords] 和弦 #{} 的列表里有重复项，跳过", i + 1);
                continue;
            }
            boolean overlap = chord.modifiers.stream().anyMatch(chord.triggers::contains);
            if (overlap) {
                ControlifyChords.LOGGER.warn("[Chords] 和弦 #{} 的同一个键同时出现在修饰键和触发键里，跳过", i + 1);
                continue;
            }

            boolean raw = ChordBindings.isRawInput(chord.modifiers.get(0));
            boolean mixed = chord.modifiers.stream().anyMatch(id -> ChordBindings.isRawInput(id) != raw)
                    || chord.triggers.stream().anyMatch(id -> ChordBindings.isRawInput(id) != raw);
            if (mixed) {
                ControlifyChords.LOGGER.warn("[Chords] 和弦 #{} 混用了物理按键与绑定 ID，跳过", i + 1);
                continue;
            }

            String signature = new LinkedHashSet<>(chord.modifiers) + "|" + new LinkedHashSet<>(chord.triggers);
            if (!signatures.add(signature)) {
                ControlifyChords.LOGGER.warn("[Chords] 和弦 #{} 与前面的和弦完全相同，跳过", i + 1);
                continue;
            }

            if (raw) {
                List<Identifier> modifiers = new ArrayList<>();
                List<Identifier> triggers = new ArrayList<>();
                boolean invalid = false;
                for (String id : chord.modifiers) {
                    Identifier rl = Identifier.tryParse(id);
                    if (rl == null) {
                        invalid = true;
                        break;
                    }
                    modifiers.add(rl);
                }
                for (String id : chord.triggers) {
                    Identifier rl = Identifier.tryParse(id);
                    if (rl == null) {
                        invalid = true;
                        break;
                    }
                    triggers.add(rl);
                }
                if (invalid) {
                    ControlifyChords.LOGGER.warn("[Chords] 和弦 #{} 的物理按键 ID 非法，跳过", i + 1);
                    continue;
                }
                for (Identifier id : modifiers) {
                    if (!WATCH_RAW.contains(id)) {
                        WATCH_RAW.add(id);
                    }
                }
                for (Identifier id : triggers) {
                    if (!WATCH_RAW.contains(id)) {
                        WATCH_RAW.add(id);
                    }
                }
                RUNTIMES.add(new Runtime(i, chord, modifiers, triggers, List.of(), List.of()));
            } else {
                List<InputBindingSupplier> modifiers = new ArrayList<>();
                List<InputBindingSupplier> triggers = new ArrayList<>();
                boolean missing = false;
                for (String id : chord.modifiers) {
                    Optional<InputBindingSupplier> supplier = ChordBindings.supplier(id);
                    if (supplier.isEmpty()) {
                        ControlifyChords.LOGGER.warn("[Chords] 和弦 #{} 的修饰键 '{}' 非法，跳过", i + 1, id);
                        missing = true;
                        break;
                    }
                    modifiers.add(supplier.get());
                    addWatch(supplier.get());
                }
                if (missing) {
                    continue;
                }
                for (String id : chord.triggers) {
                    Optional<InputBindingSupplier> supplier = ChordBindings.supplier(id);
                    if (supplier.isEmpty()) {
                        ControlifyChords.LOGGER.warn("[Chords] 和弦 #{} 的触发键 '{}' 非法，跳过", i + 1, id);
                        missing = true;
                        break;
                    }
                    triggers.add(supplier.get());
                    addWatch(supplier.get());
                }
                if (missing) {
                    continue;
                }
                RUNTIMES.add(new Runtime(i, chord, List.of(), List.of(), modifiers, triggers));
            }
        }

        ControlifyChords.LOGGER.info("[Chords] 已装载 {} 个和弦（物理按键 {} 个，绑定 {} 个）",
                RUNTIMES.size(), WATCH_RAW.size(), WATCH_BINDINGS.size());
    }

    private static void addWatch(InputBindingSupplier supplier) {
        if (!WATCH_BINDINGS.contains(supplier)) {
            WATCH_BINDINGS.add(supplier);
        }
    }

    private static final class Runtime {

        private final int index;
        private final ChordDefinition definition;
        private final List<Identifier> rawModifiers;
        private final List<Identifier> rawTriggers;
        private final List<InputBindingSupplier> bindModifiers;
        private final List<InputBindingSupplier> bindTriggers;

        /** 每个运行时的触发键按下状态，避免多个和弦共享触发键时互相吞掉 justPressed。 */
        private final Map<Identifier, Boolean> rawTriggerDown = new HashMap<>();

        private Identifier pendingRaw;
        private InputBindingSupplier pendingBind;
        private int pendingSince = -1;
        private int cooldownUntil = -1;

        private Runtime(int index, ChordDefinition definition,
                        List<Identifier> rawModifiers, List<Identifier> rawTriggers,
                        List<InputBindingSupplier> bindModifiers, List<InputBindingSupplier> bindTriggers) {
            this.index = index;
            this.definition = definition;
            this.rawModifiers = rawModifiers;
            this.rawTriggers = rawTriggers;
            this.bindModifiers = bindModifiers;
            this.bindTriggers = bindTriggers;
        }

        private boolean isRaw() {
            return !rawTriggers.isEmpty();
        }

        private void resetTransient() {
            rawTriggerDown.clear();
            clearPending();
        }

        private void clearPending() {
            pendingRaw = null;
            pendingBind = null;
            pendingSince = -1;
        }

        private void tick(ControllerEntity controller, Set<Identifier> buttons) {
            // 先更新触发键的按下状态：冷却期间也要更新，否则冷却结束会把“一直按着”误判成刚按下
            Identifier justPressedRaw = null;
            if (isRaw()) {
                for (Identifier id : rawTriggers) {
                    boolean down = buttons.contains(id);
                    boolean wasDown = Boolean.TRUE.equals(rawTriggerDown.get(id));
                    rawTriggerDown.put(id, down);
                    if (down && !wasDown) {
                        justPressedRaw = id;
                    }
                }
            }

            if (tickCounter < cooldownUntil) {
                return;
            }

            boolean armed = true;
            int firstPressTick = Integer.MAX_VALUE;
            int lastPressTick = -1;
            if (isRaw()) {
                if (!buttons.containsAll(rawModifiers)) {
                    armed = false;
                } else {
                    for (Identifier id : rawModifiers) {
                        Integer pressedAt = RAW_PRESS_TICKS.get(id);
                        if (pressedAt == null) {
                            armed = false;
                            break;
                        }
                        firstPressTick = Math.min(firstPressTick, pressedAt);
                        lastPressTick = Math.max(lastPressTick, pressedAt);
                    }
                    if (armed && definition.ordered && !orderMatchesRaw()) {
                        armed = false;
                    }
                }
            } else {
                for (InputBindingSupplier supplier : bindModifiers) {
                    InputBinding binding = supplier.onOrNull(controller);
                    if (binding == null || !binding.digitalNow()) {
                        armed = false;
                        break;
                    }
                    Integer pressedAt = BINDING_PRESS_TICKS.get(supplier.bindId().toString());
                    if (pressedAt == null) {
                        armed = false;
                        break;
                    }
                    firstPressTick = Math.min(firstPressTick, pressedAt);
                    lastPressTick = Math.max(lastPressTick, pressedAt);
                }
                if (armed && definition.ordered && !orderMatchesBindings()) {
                    armed = false;
                }
            }

            // 有序：第一个修饰键按住足够久；无序：最后一个按下的修饰键也按住足够久
            int lastModifierPressTick = definition.ordered ? firstPressTick : lastPressTick;

            // 物理按键：和弦成立或已有待触发键时，抑制这些按钮的普通绑定
            if (isRaw() && (armed || pendingRaw != null)) {
                SUPPRESSED.addAll(rawModifiers);
                if (pendingRaw != null) {
                    SUPPRESSED.add(pendingRaw);
                } else {
                    SUPPRESSED.addAll(rawTriggers);
                }
            }

            boolean hasPending = isRaw() ? pendingRaw != null : pendingBind != null;
            if (hasPending) {
                if (!armed) {
                    clearPending();
                    return;
                }
                boolean released;
                if (isRaw()) {
                    released = !buttons.contains(pendingRaw);
                } else {
                    InputBinding binding = pendingBind.onOrNull(controller);
                    released = binding != null && binding.justReleased();
                }
                if (released) {
                    if (tickCounter - pendingSince <= definition.maxTriggerTapTicks) {
                        fire();
                    }
                    clearPending();
                } else if (tickCounter - pendingSince > definition.maxTriggerTapTicks) {
                    clearPending();
                }
                return;
            }

            if (!armed) {
                return;
            }
            if (lastModifierPressTick < 0
                    || tickCounter - lastModifierPressTick < definition.minModifierHoldTicks) {
                return;
            }

            if (isRaw()) {
                if (justPressedRaw != null) {
                    pendingRaw = justPressedRaw;
                    pendingSince = tickCounter;
                }
            } else {
                for (InputBindingSupplier supplier : bindTriggers) {
                    InputBinding binding = supplier.onOrNull(controller);
                    if (binding != null && binding.justPressed()) {
                        pendingBind = supplier;
                        pendingSince = tickCounter;
                        break;
                    }
                }
            }
        }

        /** 有序模式下，实际按下顺序是否与配置列表一致。同一 tick 按下按列表顺序视为一致。 */
        private boolean orderMatchesRaw() {
            List<Identifier> sorted = new ArrayList<>(rawModifiers);
            sorted.sort(Comparator
                    .comparingInt((Identifier id) -> RAW_PRESS_TICKS.getOrDefault(id, Integer.MAX_VALUE))
                    .thenComparingInt(rawModifiers::indexOf));
            return sorted.equals(rawModifiers);
        }

        private boolean orderMatchesBindings() {
            List<InputBindingSupplier> sorted = new ArrayList<>(bindModifiers);
            sorted.sort(Comparator
                    .comparingInt((InputBindingSupplier s) ->
                            BINDING_PRESS_TICKS.getOrDefault(s.bindId().toString(), Integer.MAX_VALUE))
                    .thenComparingInt(bindModifiers::indexOf));
            return sorted.equals(bindModifiers);
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
                ControlifyChords.LOGGER.debug("[Chords] 触发 #{} (raw={}) {} + {} -> {}",
                        index + 1, isRaw(), definition.modifiers, definition.triggers, definition.actionValue);
            }
        }
    }
}
