package dev.youhaoyu.controlifychords.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.youhaoyu.controlifychords.ControlifyChords;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** JSON 配置读写。纯 JSON，方便手改和粘贴绑定 ID。 */
public final class ConfigManager {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private static Path path;
    private static ChordsConfig config = ChordsConfig.createDefault();
    private static boolean loaded;

    private ConfigManager() {
    }

    public static void load() {
        if (loaded) {
            return;
        }
        loaded = true;
        path = FabricLoader.getInstance().getConfigDir().resolve("controlify-chords.json");
        if (!Files.exists(path)) {
            save();
            return;
        }
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            ChordsConfig parsed = GSON.fromJson(reader, ChordsConfig.class);
            if (parsed != null) {
                config = parsed;
            }
        } catch (Exception e) {
            ControlifyChords.LOGGER.error("[Chords] 读取配置失败，使用默认值：{}", path, e);
        }
        config.normalize();
    }

    public static void save() {
        if (path == null) {
            path = FabricLoader.getInstance().getConfigDir().resolve("controlify-chords.json");
        }
        config.normalize();
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(config, writer);
            }
        } catch (Exception e) {
            ControlifyChords.LOGGER.error("[Chords] 保存配置失败：{}", path, e);
        }
    }

    public static ChordsConfig get() {
        if (!loaded) {
            load();
        }
        return config;
    }
}
