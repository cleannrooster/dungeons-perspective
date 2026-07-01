package com.cleannrooster.dungeons_iso.fabric;

import com.cleannrooster.dungeons_iso.ClientInit;
import com.cleannrooster.dungeons_iso.config.ConfigScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

@Environment(EnvType.CLIENT)
public class FabricClientMod implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Create key bindings then register them with Fabric's KeyBindingHelper
        ClientInit.registerKeyBindings();
        for (KeyMapping binding : ClientInit.getAllKeyBindings()) {
            KeyMappingHelper.registerKeyMapping(binding);
        }
        ClientInit.init();

        // Register /dperspective client command to open the config screen
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
            dispatcher.register(ClientCommands.literal("dperspective")
                .executes(ctx -> {
                    Minecraft.getInstance().execute(() ->
                        Minecraft.getInstance().setScreenAndShow(ConfigScreen.create(null))
                    );
                    return 1;
                }))
        );
    }
}
