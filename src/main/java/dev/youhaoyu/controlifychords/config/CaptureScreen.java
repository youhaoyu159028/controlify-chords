package dev.youhaoyu.controlifychords.config;

import dev.isxander.controlify.api.ControlifyApi;
import dev.isxander.controlify.controller.ControllerEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/**
 * 捕获界面：等用户按下手柄上的一个按钮，读到手就回调并返回配置界面。
 *
 * <p>打开后先记录“已经按住的按钮”，之后只接受新按下的按钮，避免进入界面的那一下被误捕。
 */
public final class CaptureScreen extends Screen {

    private final Component prompt;
    private final Consumer<String> onCaptured;

    private final Set<Identifier> heldAtOpen = new HashSet<>();
    private boolean initialised;
    private int openTicks;

    public CaptureScreen(Component prompt, Consumer<String> onCaptured) {
        super(Component.translatable("controlifychords.capture.title"));
        this.prompt = prompt;
        this.onCaptured = onCaptured;
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> close())
                .bounds(this.width / 2 - 60, this.height - 50, 120, 20)
                .build());
    }

    @Override
    public void tick() {
        openTicks++;
        if (!initialised) {
            heldAtOpen.addAll(currentButtons());
            initialised = true;
            return;
        }
        if (openTicks < 3) {
            return;
        }
        for (Identifier id : currentButtons()) {
            if (!heldAtOpen.contains(id)) {
                onCaptured.accept(id.toString());
                return;
            }
        }
    }

    private Set<Identifier> currentButtons() {
        Optional<ControllerEntity> controller = ControlifyApi.get().getCurrentController();
        if (controller.isEmpty()) {
            return Set.of();
        }
        return controller.get().input()
                .map(input -> input.stateNow().getButtons())
                .orElse(Set.of());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.text(this.font, this.title, this.width / 2 - this.font.width(this.title) / 2,
                this.height / 2 - 50, 0xFFFFFFFF);
        graphics.text(this.font, this.prompt, this.width / 2 - this.font.width(this.prompt) / 2,
                this.height / 2 - 24, 0xFFA0A0A0);
        Component hint = Component.translatable("controlifychords.capture.hint");
        graphics.text(this.font, hint, this.width / 2 - this.font.width(hint) / 2,
                this.height / 2 + 4, 0xFF808080);
    }

    @Override
    public void onClose() {
        close();
    }

    private void close() {
        Minecraft client = Minecraft.getInstance();
        client.setScreenAndShow(ChordsScreen.create(null));
    }
}
