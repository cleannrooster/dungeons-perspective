package com.cleannrooster.dungeons_iso.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public class Gui implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        // The method reference bootstraps ConfigScreen, which cannot load without YACL — so the
        // check has to happen before it, not inside it.
        if (!Config.GSON.hasScreen()) {
            return parent -> null;
        }
        return ConfigScreen::create;
    }
}
