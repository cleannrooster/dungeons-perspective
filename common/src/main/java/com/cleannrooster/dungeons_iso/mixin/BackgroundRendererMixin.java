package com.cleannrooster.dungeons_iso.mixin;

import com.cleannrooster.dungeons_iso.config.Config;
import com.cleannrooster.dungeons_iso.api.cullers.room.CullingBackdrop;
import com.cleannrooster.dungeons_iso.mod.Mod;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BackgroundRenderer.class)
public class BackgroundRendererMixin {
    @Shadow private static float red;
    @Shadow private static float green;
    @Shadow private static float blue;

    @Inject(method = "render", at = @At("TAIL"))
    private static void shadeCulledCaveBackdrop(Camera camera, float tickDelta,
                                                net.minecraft.client.world.ClientWorld world,
                                                int viewDistance, float skyDarkness,
                                                CallbackInfo ci) {
        CullingBackdrop.update();
        float amount = CullingBackdrop.strength();
        if (amount <= 0.0F) {
            return;
        }

        // Preserve the dimension/biome hue, but pull its luminance down toward cave fog. When the
        // sky pass is suppressed this is the colour visible through the intentional cutaway.
        // Fully enclosed settles at true black. During the short transition the original
        // dimension/biome fog colour is retained and smoothly fades out.
        float multiplier = 1.0F - amount;
        red *= multiplier;
        green *= multiplier;
        blue *= multiplier;
        CullingBackdrop.setColor(red, green, blue);
        RenderSystem.setShaderFogColor(red, green, blue);
    }

    @Inject(method = "applyFog", at = @At("HEAD"), cancellable = true)
    private static void applyFogFogOfWar(Camera camera, BackgroundRenderer.FogType fogType, float viewDistance, boolean thickFog, float tickDelta, CallbackInfo ci) {
        if(Mod.enabled && MinecraftClient.getInstance().getCameraEntity() != null && MinecraftClient.getInstance().getCameraEntity() instanceof LivingEntity living && living.hasStatusEffect(StatusEffects.DARKNESS)){
            ci.cancel();
        }

    }
    @Inject(method = "applyFog", at = @At("TAIL"))
    private static void applyFogFogOfWarEND(Camera camera, BackgroundRenderer.FogType fogType, float viewDistance, boolean thickFog, float tickDelta, CallbackInfo ci) {
        if(Mod.enabled && Config.GSON.instance().distanceFog){
            var flot = Mod.zoomMetric * (Mod.getZoom() + 1);
            RenderSystem.setShaderFogStart(flot*1.5F);
            RenderSystem.setShaderFogEnd(flot*3F);


        }

    }
}
