package com.cleannrooster.dungeons_iso.mixin;

import com.cleannrooster.dungeons_iso.config.Config;
import com.cleannrooster.dungeons_iso.mod.Mod;
import com.mojang.blaze3d.audio.Listener;
import com.mojang.blaze3d.audio.ListenerTransform;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundEngineExecutor;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SoundEngine.class)
public class SoundSystemMixin {

    @Shadow private boolean started;
    @Shadow private Listener listener;
    @Shadow  private  SoundEngineExecutor taskQueue;


    @Inject(method = "updateListenerPosition", at = @At("HEAD"),cancellable = true)
    private void repositionListenerToPlayer(Camera camera, CallbackInfo ci) {
        if (!Mod.enabled || !started || !camera.isInitialized()) return;
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;
        var pt = camera.getPosition().add(client.player.getEyePosition().subtract(camera.getPosition()).scale(Config.GSON.instance().soundListenerBias));
        taskQueue.execute(() -> listener.setTransform(new ListenerTransform(
                pt,
                Vec3.directionFromRotation(0,  camera.getYRot()),
                new Vec3(0,1F,0F)
        )));
        ci.cancel();
    }
}
