package dev.youhaoyu.controlifychords.compat;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.youhaoyu.controlifychords.config.ChordsScreen;

/** Mod Menu 配置入口；未安装 Mod Menu 时该类不会被加载。 */
public final class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ChordsScreen::create;
    }
}
