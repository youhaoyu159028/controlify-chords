package dev.youhaoyu.controlifychords.chord;

import dev.youhaoyu.controlifychords.ControlifyChords;
import dev.isxander.controlify.api.bind.ControlifyBindApi;
import dev.isxander.controlify.api.bind.InputBindingSupplier;
import dev.isxander.controlify.api.entrypoint.PreInitContext;
import dev.isxander.controlify.bindings.BindContext;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** 注册与控制器的绑定；所有 Controlify 相关代码集中在这里。 */
public final class ChordBindings {

    public static final int KEY_SLOTS = 8;

    private static final InputBindingSupplier[] KEYS = new InputBindingSupplier[KEY_SLOTS];
    private static InputBindingSupplier openConfig;
    private static final Map<String, InputBindingSupplier> EXTERNAL = new HashMap<>();
    private static final List<String> KNOWN_IDS = new ArrayList<>();
    private static boolean registered;

    private ChordBindings() {
    }

    public static void registerAll(PreInitContext context) {
        if (registered) {
            return;
        }
        ControlifyBindApi api = context.bindings();
        Component category = Component.translatable("controlifychords.binding.category");

        for (int i = 0; i < KEY_SLOTS; i++) {
            final int slot = i + 1;
            KEYS[i] = api.registerBinding(builder -> builder
                    .id(ControlifyChords.MOD_ID, "key_" + slot)
                    .name(Component.translatable("controlifychords.binding.key", slot))
                    .description(Component.translatable("controlifychords.binding.key.desc", slot))
                    .category(category)
                    .allowedContexts(BindContext.IN_GAME));
        }
        // 不依赖键盘的配置入口：在 Controlify 里把任意按钮绑到它，按下即可打开设置
        openConfig = api.registerBinding(builder -> builder
                .id(ControlifyChords.MOD_ID, "open_config")
                .name(Component.translatable("controlifychords.binding.open_config"))
                .description(Component.translatable("controlifychords.binding.open_config.desc"))
                .category(category)
                .allowedContexts(BindContext.IN_GAME));

        registered = true;

        try {
            KNOWN_IDS.clear();
            api.getAllBindIds().map(Identifier::toString).sorted().forEach(KNOWN_IDS::add);
            ControlifyChords.LOGGER.info("[Chords] Controlify 暴露了 {} 个绑定 ID", KNOWN_IDS.size());
        } catch (Throwable t) {
            ControlifyChords.LOGGER.warn("[Chords] 读取绑定 ID 列表失败", t);
        }
    }

    /** 物理按键 ID，例如 {@code controlify:button/dpad_up}。 */
    public static boolean isRawInput(String id) {
        return id != null && id.startsWith("controlify:button/");
    }

    /** “打开设置”的绑定；未注册时为 null。 */
    public static InputBindingSupplier openConfig() {
        return openConfig;
    }

    public static InputBindingSupplier key(int index) {
        return index >= 0 && index < KEYS.length ? KEYS[index] : null;
    }

    public static Optional<InputBindingSupplier> supplier(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        String key = id.trim();
        if (key.startsWith(ControlifyChords.MOD_ID + ":key_")) {
            try {
                int slot = Integer.parseInt(key.substring((ControlifyChords.MOD_ID + ":key_").length()));
                InputBindingSupplier own = key(slot - 1);
                return own == null ? Optional.empty() : Optional.of(own);
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }
        synchronized (EXTERNAL) {
            InputBindingSupplier cached = EXTERNAL.get(key);
            if (cached != null) {
                return Optional.of(cached);
            }
            Identifier rl = Identifier.tryParse(key);
            if (rl == null) {
                return Optional.empty();
            }
            InputBindingSupplier created = ControlifyBindApi.get().createSupplier(rl);
            EXTERNAL.put(key, created);
            return Optional.of(created);
        }
    }

    public static String idHint() {
        return "controlify:button/dpad_up, controlify:button/south, controlify:button/north, controlifychords:key_1 ... key_8";
    }

    public static List<String> knownIds() {
        return List.copyOf(KNOWN_IDS);
    }
}
