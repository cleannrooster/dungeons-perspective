package com.cleannrooster.dungeons_iso.mixin;

import com.cleannrooster.dungeons_iso.config.Config;
import com.cleannrooster.dungeons_iso.mod.Mod;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSectionManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.joml.Vector2d;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.mojang.blaze3d.systems.RenderSystem.assertOnRenderThread;

@Mixin(RenderSystem.class)
public class RenderSystemMixin {
/*    @Shadow
    private static float shaderFogStart;
    @Shadow
    private static float shaderFogEnd;
    private static  float[] shaderFogColorXiv = new float[4];
    @Shadow
    private static  float[] shaderFogColor;

    static{
        shaderFogColorXiv[0] = 0;
        shaderFogColorXiv[1] = 0;
        shaderFogColorXiv[2] = 0;
        shaderFogColorXiv[3] = 1;
    }
    private static double scal;

    @Inject(
            method = "setShaderFogStart", at = @At("HEAD"),remap = false,cancellable = true
    )
    private static void setShaderFogStartXIV(float a, CallbackInfo ci) {
        if(Mod.enabled && MinecraftClient.getInstance().cameraEntity != null){
            assertOnRenderThread();
            method();
            shaderFogStart = (Mod.zoomMetric*Mod.getZoom() +(float)scal)*1.025F;
           ci.cancel();

       }
    }

*//*    @Inject(
            method = "setShaderColor", at = @At("HEAD"),remap = false,cancellable = true
    )
    private static void setShaderColorXIV(float x,float y, float z, float a,  CallbackInfo ci) {
        if(Mod.enabled){
            assertOnRenderThread();
            shaderFogColor[0] = 0;
            shaderFogColor[1] = 0;
            shaderFogColor[2] = 0;
            shaderFogColor[3] = 1;
            ci.cancel();
        }
    }*//*

*//*    @Inject(
            method = "getShaderFogColor", at = @At("HEAD"),remap = false,cancellable = true
    )
    private static void getShaderFogColorXIV(CallbackInfoReturnable<float[]> ci) {
        if(Mod.enabled) {
            ci.setReturnValue(shaderFogColorXiv);
        }
    }*//*
    @Inject(
            method = "clearColor", at = @At("HEAD"),remap = false,cancellable = true
    )
    private static void clearColorXIV(float red, float green, float blue, float alpha,CallbackInfo ci) {
        if(Mod.enabled) {

            GlStateManager._clearColor(0, 0, 0, 0);
            ci.cancel();
        }
    }
*//*    @Inject(
            method = "getShaderFogColor", at = @At("HEAD"),remap = false,cancellable = true
    )
    private static void getShaderFogColorXIV(CallbackInfoReturnable<float[]> ci) {
        if(Mod.enabled) {
            ci.setReturnValue(shaderFogColorXiv);
        }
    }*/
}
