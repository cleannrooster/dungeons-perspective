package com.cleannrooster.dungeons_iso.mixin;

import com.cleannrooster.dungeons_iso.mod.Mod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.fog.environment.DarknessFogEnvironment;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


// 26.2: the darkness dimming that used to live on LightTexture#getDarkness/getDarknessFactor
// moved into the fog-environment system. We neutralize the darkness darkening while the mod is
// active by forcing the modified-darkness factor to 0.
@Mixin(DarknessFogEnvironment.class)
public class LightmapTextureManagerMixin {

    @Inject(
            method = "getModifiedDarkness",
            at = @At(value = "RETURN"),
            cancellable = true
    )
    private void getModifiedDarknessFOG(LivingEntity entity, float darkness, float delta, CallbackInfoReturnable<Float> cir) {
        if (Mod.enabled && Minecraft.getInstance().getCameraEntity() != null
                && Minecraft.getInstance().getCameraEntity() instanceof LivingEntity player
                && player.hasEffect(MobEffects.DARKNESS)) {
            cir.setReturnValue(0F);
        }
    }
}
