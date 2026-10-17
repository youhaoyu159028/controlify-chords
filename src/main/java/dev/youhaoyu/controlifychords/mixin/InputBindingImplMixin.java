package dev.youhaoyu.controlifychords.mixin;

import dev.isxander.controlify.api.bind.InputBinding;
import dev.isxander.controlify.bindings.InputBindingImpl;
import dev.isxander.controlify.controller.input.ControllerStateView;
import dev.youhaoyu.controlifychords.chord.ChordManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 在 Controlify 计算完绑定状态后，强制把“和弦进行中”的物理按键绑定标记为 suppressed。
 *
 * <p>Controlify 的 DigitalOutput 会读取 StateAccess.isSuppressed()；为 true 时 digitalNow()
 * 恒为 false，于是普通绑定不会触发。我们只读不写 Controlify 的输入本身，避免破坏它的状态机。
 */
@Mixin(InputBindingImpl.class)
public abstract class InputBindingImplMixin {

    @Shadow
    private boolean suppressed;

    @Inject(method = "pushState", at = @At("TAIL"))
    private void controlifychords$suppressChordInputs(ControllerStateView state, CallbackInfo ci) {
        if (ChordManager.shouldSuppress((InputBinding) (Object) this)) {
            this.suppressed = true;
        }
    }
}
