package com.cleannrooster.dungeons_iso.neoforge.client;

import com.cleannrooster.dungeons_iso.ClientInit;
import com.cleannrooster.dungeons_iso.config.ConfigScreen;
import com.mojang.brigadier.Command;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.server.command.CommandManager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

@EventBusSubscriber(modid = "dungeons_iso", bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class NeoForgeClientMod {

    /**
     * Key bindings must be registered here on NeoForge — this event fires before
     * FMLClientSetupEvent, and NeoForge marks key mappings as "already processed"
     * if you try to register them any later.
     */
    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        ClientInit.registerKeyBindings();
        for (KeyBinding binding : ClientInit.getAllKeyBindings()) {
            event.register(binding);
        }
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(ClientInit::init);
    }
}

/** Separate subscriber on the FORGE bus for game-phase events (commands). */
@EventBusSubscriber(modid = "dungeons_iso", bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
class NeoForgeClientGameEvents {

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(
            CommandManager.literal("dperspective")
                .executes(ctx -> {
                    MinecraftClient.getInstance().execute(() ->
                        MinecraftClient.getInstance().setScreen(ConfigScreen.create(null))
                    );
                    return Command.SINGLE_SUCCESS;
                })
        );
    }
}
