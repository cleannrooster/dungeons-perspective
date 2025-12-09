package com.cleannrooster.dungeons_iso.mixin;

import com.cleannrooster.dungeons_iso.mod.Mod;
import net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer;
import net.minecraft.client.render.*;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(LightmapTextureManager.class)
public  class LightmapTextureManagerMixin  {


    @Inject(
            method = "getDarknessFactor",
            at = @At(value = "RETURN"),
            cancellable = true
    )
    private  void getDarknessFactorFOG(float delta, CallbackInfoReturnable<Float> cir) {
        if(Mod.enabled ) {
            cir.setReturnValue(0F);
        }
    }
    @Inject(
            method = "getDarkness",
            at = @At(value = "RETURN"),
            cancellable = true
    )
    private void getDarknessFOG(LivingEntity entity, float factor, float delta, CallbackInfoReturnable<Float> cir) {
        if(Mod.enabled ) {
            cir.setReturnValue(1F);
        }
    }
}
