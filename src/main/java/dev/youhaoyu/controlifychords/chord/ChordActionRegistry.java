package dev.youhaoyu.controlifychords.chord;

import dev.youhaoyu.controlifychords.ControlifyChords;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** CUSTOM 动作注册表。 */
public final class ChordActionRegistry {

    private static final Map<String, ChordAction> ACTIONS = new ConcurrentHashMap<>();

    private ChordActionRegistry() {
    }

    public static void register(String id, ChordAction action) {
        if (id == null || id.isBlank() || action == null) {
            throw new IllegalArgumentException("id/action 不能为空");
        }
        if (ACTIONS.putIfAbsent(id.trim(), action) != null) {
            ControlifyChords.LOGGER.warn("[Chords] 自定义动作 {} 已存在，忽略", id);
        }
    }

    public static ChordAction get(String id) {
        return id == null ? null : ACTIONS.get(id.trim());
    }
}
