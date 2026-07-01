package com.cleannrooster.dungeons_iso.mixin;

import com.cleannrooster.dungeons_iso.config.Config;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.cleannrooster.dungeons_iso.mod.Mod;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.world.entity.LivingEntity;

import static com.cleannrooster.dungeons_iso.compat.SodiumCompat.fogOfWar;
import static net.minecraft.world.effect.MobEffects.DARKNESS;

// 26.2: the HUD render path moved from Gui#render to Hud's render-state extraction methods.
@Mixin(Hud.class)
public class InGameHudMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(
            method = "extractCameraOverlays", at = @At("HEAD"), cancellable = true
    )
    public void renderDarkness(GuiGraphicsExtractor context, DeltaTracker tickCounter, CallbackInfo ci) {
        if(Mod.enabled && ((minecraft.getCameraEntity() instanceof LivingEntity living && living.hasEffect(DARKNESS)) || Config.GSON.instance().fogOfWar) && fogOfWar != null) {
            fogOfWar.render(context,tickCounter.getGameTimeDeltaPartialTick(false));
        }
    }

    @Inject(
            method = "extractCrosshair", at = @At("HEAD"), cancellable = true
    )
    private void crosshairPreXIV(GuiGraphicsExtractor context, DeltaTracker tickCounter, CallbackInfo ci) {
        if (Mod.enabled) {
            ci.cancel();
        }
    }
}
