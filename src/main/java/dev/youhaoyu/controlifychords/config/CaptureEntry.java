package dev.youhaoyu.controlifychords.config;

import me.shedaniel.clothconfig2.gui.AbstractConfigScreen;
import me.shedaniel.clothconfig2.gui.entries.TooltipListEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 一个“点一下 → 按下手柄按钮 → 自动填入”的配置条目。
 *
 * <p>点击时会先把当前屏幕的编辑提交（{@link AbstractConfigScreen#saveAll(boolean)}），
 * 再打开 {@link CaptureScreen}，避免用户刚输入的内容丢失。
 */
public final class CaptureEntry extends TooltipListEntry<String> {

    private final Button button;
    private String lastCaptured = "";

    public CaptureEntry(Component fieldName,
                        Component buttonLabel,
                        Component prompt,
                        Consumer<String> onCaptured,
                        Supplier<Optional<Component[]>> tooltipSupplier) {
        super(fieldName, tooltipSupplier);
        this.button = Button.builder(buttonLabel, pressed -> {
            AbstractConfigScreen screen = getConfigScreen();
            if (screen != null) {
                screen.saveAll(false);
            }
            Minecraft.getInstance().setScreenAndShow(new CaptureScreen(prompt, value -> {
                this.lastCaptured = value;
                onCaptured.accept(value);
            }));
        }).bounds(0, 0, 150, 20).build();
    }

    @Override
    public String getValue() {
        return lastCaptured;
    }

    @Override
    public Optional<String> getDefaultValue() {
        return Optional.empty();
    }

    @Override
    public boolean isEdited() {
        return false;
    }

    @Override
    public void save() {
        // 值在捕获时已经直接写入 chord 列表并保存
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int index, int y, int x,
                                   int entryWidth, int entryHeight, int mouseX, int mouseY,
                                   boolean isSelected, float delta) {
        super.extractRenderState(graphics, index, y, x, entryWidth, entryHeight, mouseX, mouseY,
                isSelected, delta);

        graphics.text(Minecraft.getInstance().font, getFieldName(), x, y + 6, 0xFFFFFFFF);

        button.setX(x + entryWidth - button.getWidth());
        button.setY(y);
        button.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public List<? extends GuiEventListener> children() {
        return List.of(button);
    }

    @Override
    public List<? extends NarratableEntry> narratables() {
        return List.of(button);
    }

    @Override
    public int getItemHeight() {
        return 22;
    }

    @SuppressWarnings("unused")
    private AbstractWidget widget() {
        return button;
    }
}
