package com.cleannrooster.dungeons_iso.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import com.cleannrooster.dungeons_iso.mod.Mod;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;

@Mixin(KeyboardHandler.class)
public class KeyboardMixin {
    @Redirect(
            method = "onKey",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;openGameMenu(Z)V")
    )
    void openGameMenuXIV(Minecraft instance, boolean pauseOnly) {
        if (Mod.lockOnTarget != null) {
            Mod.lockOnTarget = null;
        } else {
            instance.pauseGame(pauseOnly);
        }
    }
}
