package dev.youhaoyu.controlifychords;

import com.mojang.blaze3d.platform.InputConstants;
import dev.youhaoyu.controlifychords.chord.ChordManager;
import dev.youhaoyu.controlifychords.config.ChordsScreen;
import dev.youhaoyu.controlifychords.config.ConfigManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 客户端入口：加载配置 + 挂 tick 驱动状态机 + 注册“打开配置”按键。 */
public final class ControlifyChords implements ClientModInitializer {

    public static final String MOD_ID = "controlify_chords";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /**
     * 不依赖 Mod Menu 的配置入口：默认 F9，可在“选项 → 控制 → 按键绑定”里改。
     * Mod Menu 只是另一种入口，两者打开的是同一个界面。
     */
    private static final KeyMapping OPEN_CONFIG = new KeyMapping(
            "key.controlifychords.open_config",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_F9,
            KeyMapping.Category.MISC);

    @Override
    public void onInitializeClient() {
        ConfigManager.load();
        KeyMappingHelper.registerKeyMapping(OPEN_CONFIG);

        ClientTickEvents.END_CLIENT_TICK.register(ChordManager::tick);
        ClientTickEvents.END_CLIENT_TICK.register(ControlifyChords::handleOpenConfig);

        LOGGER.info("[Chords] 已加载，配置中有 {} 个和弦槽位；按 F9 打开设置（可在按键绑定里改）",
                ConfigManager.get().chords.size());
    }

    private static void handleOpenConfig(Minecraft client) {
        // 原版只会在没有界面打开时把按键记为 click，所以这里不需要再判断 screen
        if (OPEN_CONFIG.consumeClick()) {
            client.setScreenAndShow(ChordsScreen.create(null));
        }
    }
}
