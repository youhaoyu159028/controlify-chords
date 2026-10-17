package dev.youhaoyu.controlifychords;

import dev.isxander.controlify.api.ControlifyApi;
import dev.isxander.controlify.api.entrypoint.ControlifyEntrypoint;
import dev.isxander.controlify.api.entrypoint.InitContext;
import dev.isxander.controlify.api.entrypoint.PreInitContext;
import dev.youhaoyu.controlifychords.chord.ChordBindings;
import dev.youhaoyu.controlifychords.chord.ChordManager;
import dev.youhaoyu.controlifychords.config.ConfigManager;

/**
 * Controlify 扩展入口。
 *
 * <p>绑定注册必须在 pre-init 完成，之后 Controlify 会锁定注册表。
 */
public final class ControlifyChordsEntrypoint implements ControlifyEntrypoint {

    @Override
    public void onControlifyPreInit(PreInitContext context) {
        ConfigManager.load();
        ChordBindings.registerAll(context);
        ChordManager.invalidate();
        ControlifyChords.LOGGER.info("[Chords] 已在 Controlify pre-init 注册 {} 个和弦键", ChordBindings.KEY_SLOTS);
    }

    @Override
    public void onControlifyInit(InitContext context) {
        ChordManager.invalidate();
    }

    @Override
    public void onControllersDiscovered(ControlifyApi api) {
        ChordManager.invalidate();
    }
}
