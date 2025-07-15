package com.cleannrooster.dungeons_iso;


import com.cleannrooster.dungeons_iso.config.Config;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
public class ClientInit implements ClientModInitializer {
    public static ClientInit instance;
    public static KeyBinding toggleBinding;
    public static KeyBinding isoBinding;
    public static KeyBinding lockOn;
    public static KeyBinding clickToMove;

    public static KeyBinding moveCameraBinding;
    public static KeyBinding zoomInBinding;
    public static KeyBinding zoomOutBinding;
    public static KeyBinding cycleTargetBinding;
    public static KeyBinding rotateCounterClockwise;
    public static KeyBinding rotateClockwase;
    public static KeyBinding interact;
    public static KeyBinding verticalBinding;


    public static boolean isConnectedToServer() {
        ClientPlayNetworkHandler clientPlayNetworkHandler = MinecraftClient.getInstance().getNetworkHandler();
        return clientPlayNetworkHandler != null && clientPlayNetworkHandler.getConnection().isOpen();
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

        KeyBindingHelper.registerKeyBinding(clickToMove = new KeyBinding(
                "dungeons_iso.binds.clickToMove",
                InputUtil.Type.MOUSE,
                InputUtil.UNKNOWN_KEY.getCode(),
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
        KeyBindingHelper.registerKeyBinding(verticalBinding = new KeyBinding(
                "dungeons_iso.binds.verticalBinding",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_ALT,
                "dungeons_iso.binds.category"
        ));
        KeyBindingHelper.registerKeyBinding(rotateClockwase = new KeyBinding(
                "dungeons_iso.binds.rotateClockwise",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT,
                "dungeons_iso.binds.category"
        ));
        KeyBindingHelper.registerKeyBinding(interact = new KeyBinding(
                "dungeons_iso.binds.interact",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                "dungeons_iso.binds.category"
        ));
        KeyBindingHelper.registerKeyBinding(rotateCounterClockwise = new KeyBinding(
                "dungeons_iso.binds.rotateCounterClockwise",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_LEFT,
                "dungeons_iso.binds.category"
        ));


        // Client side stuff



       /* WorldRenderEvents.BLOCK_OUTLINE.register(((worldRenderContext, blockOutlineContext) -> {
            if(Mod.enabled && Mod.crosshairTarget  instanceof BlockHitResult && blockOutlineContext.blockPos().equals(((BlockHitResult) Mod.crosshairTarget).getBlockPos())){
                drawCuboidShapeOutline(worldRenderContext.matrixStack(),worldRenderContext.consumers().getBuffer(RenderLayer.getSolid()), VoxelShapes.cuboid(new Box(blockOutlineContext.blockPos()).expand(0.7)), (double)blockOutlineContext.blockPos().getX() - worldRenderContext.camera().getPos().getX(), (double)blockOutlineContext.blockPos().getY() - worldRenderContext.camera().getPos().getY(), (double)blockOutlineContext.blockPos().getZ() - worldRenderContext.camera().getPos().getZ(), 0.0F, 0.0F, 0.0F, 0.4F);
            }
            return true;
        }));*/
    }

}