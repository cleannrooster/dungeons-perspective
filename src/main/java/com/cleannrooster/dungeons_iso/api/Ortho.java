package com.cleannrooster.dungeons_iso.api;

import com.cleannrooster.dungeons_iso.mod.Mod;
import net.minecraft.client.MinecraftClient;
import org.joml.Matrix4f;

public class Ortho {

    public static Matrix4f createOrthoMatrix(float delta, float minScale) {
        MinecraftClient client = MinecraftClient.getInstance();
        float width = Math.max(minScale, Mod.zoom*2
                * client.getWindow().getFramebufferWidth() / client.getWindow().getFramebufferHeight());
        float height = Math.max(minScale, Mod.zoom*2);
        return new Matrix4f().setOrtho(
                -width, width,
                -height, height,
                -4*Mod.zoom/4, 1000
        );
    }
}