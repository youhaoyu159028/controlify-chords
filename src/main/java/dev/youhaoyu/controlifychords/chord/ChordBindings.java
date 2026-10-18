package dev.youhaoyu.controlifychords.chord;

import dev.isxander.controlify.api.bind.ControlifyBindApi;
import dev.isxander.controlify.api.bind.InputBindingSupplier;
import dev.isxander.controlify.api.entrypoint.PreInitContext;
import dev.isxander.controlify.bindings.BindContext;
import dev.youhaoyu.controlifychords.ControlifyChords;
import net.minecraft.network.chat.Component;

/**
 * 只注册“打开设置”这一个 Controlify 绑定。
 *
 * <p>0.5.0 起移除了 key_1..key_8 与“绑定 ID 模式”：和弦成员统一使用物理按键
 * {@code controlify:button/xxx}，因此不需要占用按钮，也不需要固定的 8 个槽位。
 */
public final class ChordBindings {

    private static InputBindingSupplier openConfig;

    private ChordBindings() {
    }

    public static void registerAll(PreInitContext context) {
        if (openConfig != null) {
            return;
        }
        ControlifyBindApi api = context.bindings();
        Component category = Component.translatable("controlifychords.binding.category");

        openConfig = api.registerBinding(builder -> builder
                .id(ControlifyChords.MOD_ID, "open_config")
                .name(Component.translatable("controlifychords.binding.open_config"))
                .description(Component.translatable("controlifychords.binding.open_config.desc"))
                .category(category)
                .allowedContexts(BindContext.IN_GAME));

        ControlifyChords.LOGGER.info("[Chords] 已注册“打开和弦设置”绑定（不再注册固定和弦键）");
    }

    /** “打开设置”的绑定；未注册时为 null。 */
    public static InputBindingSupplier openConfig() {
        return openConfig;
    }

    /** 物理按键 ID，例如 {@code controlify:button/dpad_up}。 */
    public static boolean isRawInput(String id) {
        return id != null && id.startsWith("controlify:button/");
    }

    public static String idHint() {
        return "controlify:button/dpad_up, controlify:button/dpad_down, controlify:button/south, "
                + "controlify:button/north, controlify:button/left_shoulder, controlify:button/right_shoulder";
    }
}
