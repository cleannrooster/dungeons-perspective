package com.cleannrooster.dungeons_iso.mixin;

import com.cleannrooster.dungeons_iso.config.Config;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.cleannrooster.dungeons_iso.ClientInit;
import com.cleannrooster.dungeons_iso.mod.Mod;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.LivingEntity;

import static com.cleannrooster.dungeons_iso.compat.SodiumCompat.fogOfWar;
import static net.minecraft.world.effect.MobEffects.DARKNESS;

@Mixin(Gui.class)
public class InGameHudMixin {
    @Shadow
    @Final
    private Minecraft client;
    @Inject(
            method = "render", at = @At("HEAD"), cancellable = true
    )
    public void renderDarkness(GuiGraphics context, DeltaTracker tickCounter, CallbackInfo ci) {
        if(Mod.enabled && ((client.getCameraEntity() instanceof LivingEntity living && living.hasEffect(DARKNESS)) || Config.GSON.instance().fogOfWar) && fogOfWar != null) {
            fogOfWar.render(context,tickCounter.getGameTimeDeltaPartialTick(false));
        }
    }

    @Inject(
            method = "renderCrosshair", at = @At("HEAD"), cancellable = true
    )
    private void crosshairPreXIV(GuiGraphics context, DeltaTracker tickCounter, CallbackInfo ci) {
        if (Mod.enabled) {
            ci.cancel();

        }
    }

    @Inject(
            method = "renderCrosshair", at = @At("RETURN")
    )
    private void crosshairPostXIV(GuiGraphics context, DeltaTracker tickCounter, CallbackInfo ci) {
        if (Mod.enabled) {
        }
    }
}
