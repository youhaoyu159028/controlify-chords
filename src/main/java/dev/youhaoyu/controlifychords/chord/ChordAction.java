package dev.youhaoyu.controlifychords.chord;

import net.minecraft.client.Minecraft;

/** 自定义动作扩展点。 */
@FunctionalInterface
public interface ChordAction {
    void run(Minecraft client);
}
