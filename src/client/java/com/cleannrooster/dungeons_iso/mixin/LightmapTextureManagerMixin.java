package com.cleannrooster.dungeons_iso.mixin;

import com.cleannrooster.dungeons_iso.mod.Mod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(LightmapTextureManager.class)
public  class LightmapTextureManagerMixin  {


    // MC 26.1: getDarknessFactor was removed from LightmapTextureManager.

    @Inject(
            method = "getDarkness",
            at = @At(value = "RETURN"),
            cancellable = true
    )
    private void getDarknessFOG(LivingEntity entity, float factor, float delta, CallbackInfoReturnable<Float> cir) {
        if(Mod.enabled && Minecraft.getInstance().cameraEntity != null && Minecraft.getInstance().cameraEntity instanceof LivingEntity player && player.hasStatusEffect(MobEffects.DARKNESS) ) {
            cir.setReturnValue(1F);
        }
    }
}
