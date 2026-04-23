package com.cleannrooster.dungeons_iso.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import org.spongepowered.asm.mixin.Mixin;

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
        if(Mod.enabled && Minecraft.getInstance().cameraEntity != null){
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
