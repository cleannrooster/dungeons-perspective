package com.cleannrooster.dungeons_iso.mixin.client;

import com.cleannrooster.dungeons_iso.mod.Mod;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public class PlayerMixin {

    @Inject(method = "canInteractWithBlockAt", at = @At("RETURN"), cancellable = true)
    public void canInteractWithBlockAtXIV(BlockPos pos, double additionalRange, CallbackInfoReturnable<Boolean> cir) {
        if (Mod.enabled) {
            cir.setReturnValue(true);
            cir.cancel();
        }
    }
}
