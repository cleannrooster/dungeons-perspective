package com.cleannrooster.dungeons_iso.fabric;

import com.cleannrooster.dungeons_iso.ClientInit;
import com.cleannrooster.dungeons_iso.config.ConfigScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;

@Environment(EnvType.CLIENT)
public class FabricClientMod implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Create key bindings then register them with Fabric's KeyBindingHelper
        ClientInit.registerKeyBindings();
        for (KeyBinding binding : ClientInit.getAllKeyBindings()) {
            KeyBindingHelper.registerKeyBinding(binding);
        }
        ClientInit.init();

        // Register /dperspective client command to open the config screen
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
            dispatcher.register(ClientCommandManager.literal("dperspective")
                .executes(ctx -> {
                    MinecraftClient.getInstance().execute(() ->
                        MinecraftClient.getInstance().setScreen(ConfigScreen.create(null))
                    );
                    return 1;
                }))
        );
    }
}
