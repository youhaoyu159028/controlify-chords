package dev.youhaoyu.controlifychords.chord;

import dev.isxander.controlify.api.ControlifyApi;
import dev.isxander.controlify.api.bind.InputBinding;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * 和弦状态机：<b>修饰键集合</b> + <b>触发键集合</b>，成员只能是物理按键
 * {@code controlify:button/xxx}。
 *
 * <p>语义：
 * <ul>
 *   <li>修饰键全部按住即成立，顺序默认不重要；</li>
 *   <li>如果同一组修饰键被配置成多种顺序（{@code LB+RB} 与 {@code RB+LB}），
 *       这几条会自动按实际按下顺序区分；</li>
 *   <li>触发键集合里任意一个轻按都会触发同一动作；</li>
 *   <li>松开时执行，一次按住只执行一次。</li>
 * </ul>
 */
public final class ChordManager {

    private static final List<Runtime> RUNTIMES = new ArrayList<>();
    private static final List<Identifier> WATCH_RAW = new ArrayList<>();
    private static final Map<Identifier, Integer> RAW_PRESS_TICKS = new HashMap<>();
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
            RAW_PRESS_TICKS.clear();
            RUNTIMES.forEach(Runtime::resetTransient);
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
            runtime.tick(buttons);
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
        WATCH_RAW.clear();
        RAW_PRESS_TICKS.clear();

        ChordsConfig config = ConfigManager.get();
        Set<String> pool = new HashSet<>(config.keyPool);
        Set<String> signatures = new LinkedHashSet<>();

        // 同一组修饰键如果配了多种顺序，这几条必须按顺序区分
        Map<String, Set<String>> modifierOrders = new HashMap<>();
        for (ChordDefinition candidate : config.chords) {
            if (candidate == null || !candidate.enabled
                    || candidate.modifiers == null || candidate.modifiers.isEmpty()) {
                continue;
            }
            String setKey = new TreeSet<>(candidate.modifiers).toString();
            modifierOrders.computeIfAbsent(setKey, k -> new HashSet<>())
                    .add(String.join(">", candidate.modifiers));
        }

        for (int i = 0; i < config.chords.size(); i++) {
            ChordDefinition chord = config.chords.get(i);
            if (chord == null || !chord.enabled) {
                continue;
            }
            if (chord.modifiers.isEmpty() || chord.triggers.isEmpty()) {
                ControlifyChords.LOGGER.warn("[Chords] #{} 已启用但修饰键或触发键列表为空，跳过", i + 1);
                continue;
            }
            if (new HashSet<>(chord.modifiers).size() != chord.modifiers.size()
                    || new HashSet<>(chord.triggers).size() != chord.triggers.size()) {
                ControlifyChords.LOGGER.warn("[Chords] #{} 的列表里有重复项，跳过", i + 1);
                continue;
            }
            if (chord.modifiers.stream().anyMatch(chord.triggers::contains)) {
                ControlifyChords.LOGGER.warn("[Chords] #{} 的同一个键同时出现在两个列表里，跳过", i + 1);
                continue;
            }
            boolean nonRaw = chord.modifiers.stream().anyMatch(id -> !ChordBindings.isRawInput(id))
                    || chord.triggers.stream().anyMatch(id -> !ChordBindings.isRawInput(id));
            if (nonRaw) {
                ControlifyChords.LOGGER.warn("[Chords] #{} 含有非物理按键 ID（旧版 key_N / controlify:jump 已不支持），跳过", i + 1);
                continue;
            }
            if (!pool.isEmpty()) {
                boolean outside = chord.modifiers.stream().anyMatch(id -> !pool.contains(id))
                        || chord.triggers.stream().anyMatch(id -> !pool.contains(id));
                if (outside) {
                    ControlifyChords.LOGGER.warn("[Chords] #{} 用了不在“按键池”里的按钮，跳过（先在通用页把按钮加进池）", i + 1);
                    continue;
                }
            }

            String signature = String.join(">", chord.modifiers) + "|" + String.join(">", chord.triggers);
            if (!signatures.add(signature)) {
                ControlifyChords.LOGGER.warn("[Chords] #{} 与前面的和弦完全相同，跳过", i + 1);
                continue;
            }

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
                ControlifyChords.LOGGER.warn("[Chords] #{} 的物理按键 ID 非法，跳过", i + 1);
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

            String modifierSetKey = new TreeSet<>(chord.modifiers).toString();
            boolean requiresOrder = chord.ordered
                    || modifierOrders.getOrDefault(modifierSetKey, Set.of()).size() > 1;

            RUNTIMES.add(new Runtime(i, chord, modifiers, triggers, requiresOrder));
        }

        ControlifyChords.LOGGER.info("[Chords] 已装载 {} 条和弦，监听 {} 个按钮（按键池 {} 个）",
                RUNTIMES.size(), WATCH_RAW.size(), config.keyPool.size());
    }

    private static final class Runtime {

        private final int index;
        private final ChordDefinition definition;
        private final List<Identifier> rawModifiers;
        private final List<Identifier> rawTriggers;
        /** 同一组修饰键配置了多种顺序时为 true。 */
        private final boolean requiresOrder;

        /** 触发键按下状态，避免多个和弦共享触发键时互相吞掉 justPressed。 */
        private final Map<Identifier, Boolean> triggerDown = new HashMap<>();

        private Identifier pendingTrigger;
        private int pendingSince = -1;
        private int cooldownUntil = -1;

        private Runtime(int index, ChordDefinition definition,
                        List<Identifier> rawModifiers, List<Identifier> rawTriggers,
                        boolean requiresOrder) {
            this.index = index;
            this.definition = definition;
            this.rawModifiers = rawModifiers;
            this.rawTriggers = rawTriggers;
            this.requiresOrder = requiresOrder;
        }

        private void resetTransient() {
            triggerDown.clear();
            clearPending();
        }

        private void clearPending() {
            pendingTrigger = null;
            pendingSince = -1;
        }

        private void tick(Set<Identifier> buttons) {
            // 先更新触发键状态：冷却期间也要更新，否则冷却结束会把“一直按着”误判成刚按下
            Identifier justPressed = null;
            for (Identifier id : rawTriggers) {
                boolean down = buttons.contains(id);
                boolean wasDown = Boolean.TRUE.equals(triggerDown.get(id));
                triggerDown.put(id, down);
                if (down && !wasDown) {
                    justPressed = id;
                }
            }

            if (tickCounter < cooldownUntil) {
                return;
            }

            boolean armed = buttons.containsAll(rawModifiers);
            int firstPressTick = Integer.MAX_VALUE;
            int lastPressTick = -1;
            if (armed) {
                for (Identifier id : rawModifiers) {
                    Integer pressedAt = RAW_PRESS_TICKS.get(id);
                    if (pressedAt == null) {
                        armed = false;
                        break;
                    }
                    firstPressTick = Math.min(firstPressTick, pressedAt);
                    lastPressTick = Math.max(lastPressTick, pressedAt);
                }
            }
            if (armed && requiresOrder && !orderMatches()) {
                armed = false;
            }

            if (armed || pendingTrigger != null) {
                SUPPRESSED.addAll(rawModifiers);
                if (pendingTrigger != null) {
                    SUPPRESSED.add(pendingTrigger);
                } else {
                    SUPPRESSED.addAll(rawTriggers);
                }
            }

            if (pendingTrigger != null) {
                if (!armed) {
                    clearPending();
                    return;
                }
                if (!buttons.contains(pendingTrigger)) {
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
            int holdReference = requiresOrder ? firstPressTick : lastPressTick;
            if (holdReference < 0 || tickCounter - holdReference < definition.minModifierHoldTicks) {
                return;
            }
            if (justPressed != null) {
                pendingTrigger = justPressed;
                pendingSince = tickCounter;
            }
        }

        /** 有序模式下，实际按下顺序是否与配置列表一致。同一 tick 按下按列表顺序视为一致。 */
        private boolean orderMatches() {
            List<Identifier> sorted = new ArrayList<>(rawModifiers);
            sorted.sort(Comparator
                    .comparingInt((Identifier id) -> RAW_PRESS_TICKS.getOrDefault(id, Integer.MAX_VALUE))
                    .thenComparingInt(rawModifiers::indexOf));
            return sorted.equals(rawModifiers);
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
                ControlifyChords.LOGGER.debug("[Chords] 触发 #{} {} + {} -> {}",
                        index + 1, definition.modifiers, definition.triggers, definition.actionValue);
            }
        }
    }
}
