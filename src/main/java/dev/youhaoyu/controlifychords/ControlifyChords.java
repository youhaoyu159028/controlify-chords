package dev.youhaoyu.controlifychords;

import dev.youhaoyu.controlifychords.chord.ChordManager;
import dev.youhaoyu.controlifychords.config.ConfigManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 客户端入口：加载配置 + 挂 tick 驱动状态机。 */
public final class ControlifyChords implements ClientModInitializer {

    public static final String MOD_ID = "controlify_chords";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        ConfigManager.load();
        ClientTickEvents.END_CLIENT_TICK.register(ChordManager::tick);
        LOGGER.info("[Chords] 已加载，配置中有 {} 个和弦槽位", ConfigManager.get().chords.size());
    }
}
