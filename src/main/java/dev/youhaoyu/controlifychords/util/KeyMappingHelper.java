package dev.youhaoyu.controlifychords.util;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 原版按键模拟。
 *
 * <p>MC 26.x 是非混淆版本，这里直接用 Mojang 官方名：
 * {@link KeyMapping}、{@link InputConstants.Key}、{@code Options#keyMappings}。
 */
public final class KeyMappingHelper {

    private static final List<PendingRelease> PENDING = new ArrayList<>();
    private static int tickCounter;

    private KeyMappingHelper() {
    }

    public static boolean tap(String translationKey, int holdTicks) {
        KeyMapping mapping = find(translationKey);
        if (mapping == null) {
            return false;
        }
        InputConstants.Key key = boundKey(mapping);
        if (key == null) {
            return false;
        }
        KeyMapping.set(key, true);
        KeyMapping.click(key);
        mapping.setDown(true);
        PENDING.add(new PendingRelease(mapping, tickCounter + Math.max(1, holdTicks)));
        return true;
    }

    public static void tick() {
        tickCounter++;
        if (PENDING.isEmpty()) {
            return;
        }
        Iterator<PendingRelease> it = PENDING.iterator();
        while (it.hasNext()) {
            PendingRelease pending = it.next();
            if (tickCounter < pending.releaseTick) {
                continue;
            }
            InputConstants.Key key = boundKey(pending.mapping);
            if (key != null) {
                KeyMapping.set(key, false);
            }
            pending.mapping.setDown(false);
            it.remove();
        }
    }

    /**
     * 26.2 的 KeyMapping 没有 getKey()，用 saveString() 取当前绑定键名再转成 Key。
     * 未绑定时 saveString() 返回 "key.keyboard.unknown"，InputConstants.getKey 会抛异常。
     */
    private static InputConstants.Key boundKey(KeyMapping mapping) {
        String saved = mapping.saveString();
        if (saved == null || "key.keyboard.unknown".equals(saved)) {
            return null;
        }
        try {
            InputConstants.Key key = InputConstants.getKey(saved);
            return key == InputConstants.UNKNOWN ? null : key;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static KeyMapping find(String translationKey) {
        if (translationKey == null || translationKey.isBlank()) {
            return null;
        }
        for (KeyMapping mapping : Minecraft.getInstance().options.keyMappings) {
            if (translationKey.equals(mapping.getName())) {
                return mapping;
            }
        }
        return null;
    }

    private record PendingRelease(KeyMapping mapping, int releaseTick) {
    }
}
