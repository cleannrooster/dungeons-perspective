package com.cleannrooster.dungeons_iso.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.cleannrooster.dungeons_iso.ClientInit;
import com.cleannrooster.dungeons_iso.mod.Mod;

@Mixin(InGameHud.class)
public class InGameHudMixin {
    @Shadow
    @Final
    private MinecraftClient client;


    @Inject(
            method = "renderCrosshair", at = @At("HEAD"), cancellable = true
    )
    private void crosshairPreXIV(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (Mod.enabled) {
            ci.cancel();

        }
    }

    @Inject(
            method = "renderCrosshair", at = @At("RETURN")
    )
    private void crosshairPostXIV(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (Mod.enabled) {
        }
    }
}
