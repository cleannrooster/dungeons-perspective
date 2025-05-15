package com.cleannrooster.dungeons_iso;


import com.cleannrooster.dungeons_iso.config.Config;
import com.cleannrooster.dungeons_iso.network.Capabilities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.util.ArrayList;
import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
public class ClientInit implements ClientModInitializer {
    public static ClientInit instance;
    private static final ArrayList<Consumer<Capabilities>> capabilityListeners = new ArrayList<>();
    public static KeyBinding toggleBinding;
    public static KeyBinding isoBinding;
    public static KeyBinding lockOn;

    public static KeyBinding moveCameraBinding;
    public static KeyBinding zoomInBinding;
    public static KeyBinding zoomOutBinding;
    public static KeyBinding cycleTargetBinding;
    public static KeyBinding rotateCounterClockwise;
    public static KeyBinding rotateClockwase;

    @Nullable
    public static Capabilities capabilities;

    public static boolean serverSupportsCapabilities() {
        return capabilities != null;
    }

    public static boolean isConnectedToServer() {
        ClientPlayNetworkHandler clientPlayNetworkHandler = MinecraftClient.getInstance().getNetworkHandler();
        return clientPlayNetworkHandler != null && clientPlayNetworkHandler.getConnection().isOpen();
    }

    public static boolean canChangeCapabilities() {
        MinecraftClient client = MinecraftClient.getInstance();
        // We need to have received capabilities from the server already, and have adequate permissions to change them
        return serverSupportsCapabilities() && client.player != null && client.player.hasPermissionLevel(2);
    }

    public static Capabilities getCapabilities() {
        if (!isConnectedToServer()) {
            return Capabilities.all();
        } else if (!serverSupportsCapabilities()) {
            return Capabilities.none();
        }
        return capabilities;
    }

    public static void submitCapabilities(Capabilities capabilities) {
        ClientPlayNetworking.send(capabilities);
    }

    public static void listenCapabilities(Consumer<Capabilities> listener) {
        capabilityListeners.add(listener);
    }

    public static void unlistenCapabilities(Consumer<Capabilities> listener) {
        capabilityListeners.removeIf(l -> l == listener);
    }

    public static void notifyCapabilityListeners() {
        // Iterate backwards in case it decides to remove itself during execution
        // Rust wouldn't have let me make this mistake >:c
        for (int i = capabilityListeners.size() - 1; i >= 0; i--) {
            capabilityListeners.get(i).accept(getCapabilities());
        }
    }

    @Override
    public void onInitializeClient() {
        instance = this;
        Config.GSON.load();

        KeyBindingHelper.registerKeyBinding(toggleBinding = new KeyBinding(
                "dungeons_iso.binds.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F4,
                "dungeons_iso.binds.category"
        ));
        KeyBindingHelper.registerKeyBinding(isoBinding = new KeyBinding(
                "dungeons_iso.binds.iso",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_HOME,
                "dungeons_iso.binds.category"
        ));
        KeyBindingHelper.registerKeyBinding(moveCameraBinding = new KeyBinding(
                "dungeons_iso.binds.moveCamera",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_MOUSE_BUTTON_3,
                "dungeons_iso.binds.category"
        ));

        KeyBindingHelper.registerKeyBinding(lockOn = new KeyBinding(
                "dungeons_iso.binds.lockOn",
                InputUtil.Type.MOUSE,
                GLFW.GLFW_KEY_H,
                "dungeons_iso.binds.category"
        ));
        KeyBindingHelper.registerKeyBinding(zoomInBinding = new KeyBinding(
                "dungeons_iso.binds.zoomIn",
                InputUtil.Type.MOUSE,
                GLFW.GLFW_KEY_UP,
                "dungeons_iso.binds.category"
        ));
        KeyBindingHelper.registerKeyBinding(zoomOutBinding = new KeyBinding(
                "dungeons_iso.binds.zoomOut",
                InputUtil.Type.MOUSE,
                GLFW.GLFW_KEY_DOWN,
                "dungeons_iso.binds.category"
        ));
        KeyBindingHelper.registerKeyBinding(rotateClockwase = new KeyBinding(
                "dungeons_iso.binds.rotateClockwise",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT,
                "dungeons_iso.binds.category"
        )); KeyBindingHelper.registerKeyBinding(rotateCounterClockwise = new KeyBinding(
                "dungeons_iso.binds.rotateCounterClockwise",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_LEFT,
                "dungeons_iso.binds.category"
        ));




        // Client side stuff

        PayloadTypeRegistry.playC2S().register(Capabilities.ID, Capabilities.CODEC);
        PayloadTypeRegistry.playS2C().register(Capabilities.ID, Capabilities.CODEC);

        ClientPlayNetworking.registerGlobalReceiver(Capabilities.ID, (payload, context) -> {
            capabilities = payload;
            notifyCapabilityListeners();
        });

        ClientPlayConnectionEvents.DISCONNECT.register((networkHandler, minecraftClient) -> capabilities = null);

        // Server side stuff

        ServerLifecycleEvents.SERVER_STARTED.register((minecraftServer) -> capabilities = Capabilities.none());

        ServerPlayConnectionEvents.JOIN.register((networkHandler, packetSender, minecraftServer) -> {
            if (networkHandler.player.hasPermissionLevel(2)) {
                capabilities = Capabilities.load();
            }
            packetSender.sendPacket(capabilities);
        });

        ServerPlayNetworking.registerGlobalReceiver(Capabilities.ID, (payload, context) -> {
            if (!context.player().hasPermissionLevel(2)) {
                return;
            }

            if (!payload.equals(capabilities)) {
                capabilities = payload;
                try {
                    capabilities.save();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                for (ServerPlayerEntity player : context.server().getPlayerManager().getPlayerList()) {
                    ServerPlayNetworking.send(player, capabilities);
                }
            }
        });
    }
}