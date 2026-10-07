package com.cleannrooster.dungeons_iso.fabric;

import com.cleannrooster.dungeons_iso.ClientInit;
import com.cleannrooster.dungeons_iso.config.Config;
import com.cleannrooster.dungeons_iso.config.ConfigScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
public class FabricClientMod implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        FabricTerrainCullingModels.register();

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
                    // ConfigScreen cannot load without YACL, so the guard stays outside it.
                    if (!Config.GSON.hasScreen()) {
                        ctx.getSource().sendError(
                                Text.translatable("dungeons_iso.config.requires_yacl"));
                        return 0;
                    }
                    MinecraftClient.getInstance().execute(() ->
                        MinecraftClient.getInstance().setScreen(ConfigScreen.create(null))
                    );
                    return 1;
                }))
        );
    }
}
