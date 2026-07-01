package com.cleannrooster.dungeons_iso.neoforge;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod("dungeons_iso")
public class NeoForgeMod {
    public NeoForgeMod(IEventBus modBus) {
        modBus.addListener(FMLCommonSetupEvent.class, NeoForgeMod::onCommonSetup);
    }

    private static void onCommonSetup(FMLCommonSetupEvent event) {
    }
}
