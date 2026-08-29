package com.cleannrooster.dungeons_iso.neoforge.client;

import com.cleannrooster.dungeons_iso.ClientInit;
import com.cleannrooster.dungeons_iso.config.ConfigScreen;
import com.mojang.brigadier.Command;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.commands.Commands;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

// 26.2 / NeoForge: EventBusSubscriber no longer takes a `bus` parameter — the target bus is
// inferred from each handler's event type.
@EventBusSubscriber(modid = "dungeons_iso", value = Dist.CLIENT)
public class NeoForgeClientMod {

    /**
     * Key bindings must be registered here on NeoForge — this event fires before
     * FMLClientSetupEvent, and NeoForge marks key mappings as "already processed"
     * if you try to register them any later.
     */
    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        ClientInit.registerKeyBindings();
        for (KeyMapping binding : ClientInit.getAllKeyBindings()) {
            event.register(binding);
        }
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(ClientInit::init);
    }
}

/** Separate subscriber for game-phase events (commands). */
@EventBusSubscriber(modid = "dungeons_iso", value = Dist.CLIENT)
class NeoForgeClientGameEvents {

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(
            Commands.literal("dperspective")
                .executes(ctx -> {
                    if (!com.cleannrooster.dungeons_iso.config.Config.GSON.hasScreen()) {
                        ctx.getSource().sendFailure(Component.translatable("dungeons_iso.config.requires_yacl"));
                        return 0;
                    }
                    Minecraft.getInstance().execute(() ->
                        Minecraft.getInstance().setScreenAndShow(ConfigScreen.create(null))
                    );
                    return Command.SINGLE_SUCCESS;
                })
        );
    }
}
