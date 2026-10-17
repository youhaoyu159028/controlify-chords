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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 有序和弦状态机。
 *
 * <p>支持两种成员写法：
 * <ul>
 *   <li><b>物理按键</b>：{@code controlify:button/dpad_up}。直接读手柄状态，
 *       不需要把这个按钮绑到 {@code controlifychords:key_N}，所以它原来的功能保留。
 *       和弦进行时通过 Mixin 抑制这些按钮的普通绑定，避免双触发。</li>
 *   <li><b>绑定 ID</b>：{@code controlifychords:key_1}、{@code controlify:jump}。
 *       老模式，需要用户自己在 Controlify 里把按钮绑到该绑定。</li>
 * </ul>
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

        List<Identifier> rawOrder = new ArrayList<>(RAW_PRESS_TICKS.keySet());
        rawOrder.sort((a, b) -> Integer.compare(RAW_PRESS_TICKS.get(a), RAW_PRESS_TICKS.get(b)));

        for (Runtime runtime : RUNTIMES) {
            runtime.tick(controller, buttons, rawOrder);
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
            if (chord.modifiers.size() < 1 || chord.trigger == null || chord.trigger.isBlank()) {
                ControlifyChords.LOGGER.warn("[Chords] 和弦 #{} 已启用但缺少修饰键或触发键，跳过", i + 1);
                continue;
            }
            if (new LinkedHashSet<>(chord.modifiers).size() != chord.modifiers.size()) {
                ControlifyChords.LOGGER.warn("[Chords] 和弦 #{} 的修饰键有重复，跳过", i + 1);
                continue;
            }
            if (chord.modifiers.contains(chord.trigger)) {
                ControlifyChords.LOGGER.warn("[Chords] 和弦 #{} 的触发键同时是修饰键，跳过", i + 1);
                continue;
            }

            boolean raw = ChordBindings.isRawInput(chord.trigger);
            boolean mixed = chord.modifiers.stream().anyMatch(id -> ChordBindings.isRawInput(id) != raw);
            if (mixed) {
                ControlifyChords.LOGGER.warn("[Chords] 和弦 #{} 混用了物理按键与绑定 ID，跳过", i + 1);
                continue;
            }

            String signature = chord.modifiers + "|" + chord.trigger;
            if (!signatures.add(signature)) {
                ControlifyChords.LOGGER.warn("[Chords] 和弦 #{} 与前面的和弦完全相同，跳过", i + 1);
                continue;
            }

            if (raw) {
                List<Identifier> modifiers = new ArrayList<>();
                boolean invalid = false;
                for (String id : chord.modifiers) {
                    Identifier rl = Identifier.tryParse(id);
                    if (rl == null) {
                        invalid = true;
                        break;
                    }
                    modifiers.add(rl);
                }
                Identifier trigger = Identifier.tryParse(chord.trigger);
                if (invalid || trigger == null) {
                    ControlifyChords.LOGGER.warn("[Chords] 和弦 #{} 的物理按键 ID 非法，跳过", i + 1);
                    continue;
                }
                for (Identifier id : modifiers) {
                    if (!WATCH_RAW.contains(id)) {
                        WATCH_RAW.add(id);
                    }
                }
                if (!WATCH_RAW.contains(trigger)) {
                    WATCH_RAW.add(trigger);
                }
                RUNTIMES.add(new Runtime(i, chord, modifiers, trigger, null, null));
            } else {
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
                    addWatch(supplier.get());
                }
                if (missing) {
                    continue;
                }
                Optional<InputBindingSupplier> trigger = ChordBindings.supplier(chord.trigger);
                if (trigger.isEmpty()) {
                    ControlifyChords.LOGGER.warn("[Chords] 和弦 #{} 的触发键 '{}' 非法，跳过", i + 1, chord.trigger);
                    continue;
                }
                addWatch(trigger.get());
                RUNTIMES.add(new Runtime(i, chord, List.of(), null, modifiers, trigger.get()));
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
        private final Identifier rawTrigger;
        private final List<InputBindingSupplier> bindModifiers;
        private final InputBindingSupplier bindTrigger;

        private int pendingSince = -1;
        private int cooldownUntil = -1;
        private boolean rawTriggerWasDown;

        private Runtime(int index, ChordDefinition definition,
                        List<Identifier> rawModifiers, Identifier rawTrigger,
                        List<InputBindingSupplier> bindModifiers, InputBindingSupplier bindTrigger) {
            this.index = index;
            this.definition = definition;
            this.rawModifiers = rawModifiers;
            this.rawTrigger = rawTrigger;
            this.bindModifiers = bindModifiers;
            this.bindTrigger = bindTrigger;
        }

        private boolean isRaw() {
            return rawTrigger != null;
        }

        private void tick(ControllerEntity controller, Set<Identifier> buttons, List<Identifier> rawOrder) {
            if (tickCounter < cooldownUntil) {
                return;
            }

            boolean allHeld;
            boolean orderMatches;
            boolean justPressed;
            boolean justReleased;
            int firstPressTick;

            if (isRaw()) {
                allHeld = buttons.containsAll(rawModifiers);
                List<Identifier> current = new ArrayList<>();
                for (Identifier id : rawOrder) {
                    if (rawModifiers.contains(id)) {
                        current.add(id);
                    }
                }
                orderMatches = current.equals(rawModifiers);
                firstPressTick = current.isEmpty() ? Integer.MAX_VALUE : RAW_PRESS_TICKS.get(current.get(0));

                boolean down = buttons.contains(rawTrigger);
                justPressed = down && !rawTriggerWasDown;
                justReleased = !down && rawTriggerWasDown;
                rawTriggerWasDown = down;
            } else {
                InputBinding trigger = bindTrigger.onOrNull(controller);
                if (trigger == null) {
                    return;
                }
                allHeld = true;
                for (InputBindingSupplier supplier : bindModifiers) {
                    InputBinding binding = supplier.onOrNull(controller);
                    if (binding == null || !binding.digitalNow()) {
                        allHeld = false;
                        break;
                    }
                }
                List<String> current = new ArrayList<>();
                for (Map.Entry<String, Integer> e : BINDING_PRESS_TICKS.entrySet()) {
                    if (definition.modifiers.contains(e.getKey())) {
                        current.add(e.getKey());
                    }
                }
                orderMatches = current.equals(definition.modifiers);
                firstPressTick = current.isEmpty() ? Integer.MAX_VALUE : BINDING_PRESS_TICKS.get(current.get(0));
                justPressed = trigger.justPressed();
                justReleased = trigger.justReleased();
            }

            boolean armed = allHeld && orderMatches;

            if (isRaw() && (armed || pendingSince >= 0)) {
                SUPPRESSED.addAll(rawModifiers);
                SUPPRESSED.add(rawTrigger);
            }

            if (pendingSince >= 0) {
                if (!armed) {
                    pendingSince = -1;
                    return;
                }
                if (justReleased) {
                    if (tickCounter - pendingSince <= definition.maxTriggerTapTicks) {
                        fire();
                    }
                    pendingSince = -1;
                } else if (tickCounter - pendingSince > definition.maxTriggerTapTicks) {
                    pendingSince = -1;
                }
                return;
            }

            if (!armed) {
                return;
            }
            if (tickCounter - firstPressTick < definition.minModifierHoldTicks) {
                return;
            }
            if (justPressed) {
                pendingSince = tickCounter;
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
                ControlifyChords.LOGGER.debug("[Chords] 触发 #{} (raw={}) {} -> {}",
                        index + 1, isRaw(), definition.modifiers, definition.actionValue);
            }
        }
    }
}
