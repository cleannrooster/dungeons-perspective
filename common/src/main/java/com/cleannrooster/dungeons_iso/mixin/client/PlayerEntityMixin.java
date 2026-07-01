package com.cleannrooster.dungeons_iso.mixin.client;

import com.cleannrooster.dungeons_iso.mod.Mod;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public class PlayerEntityMixin {

    // NOTE (26.2 port): Entity/Player#canInteractWithBlockAt(BlockPos, double) was removed in 26.2 —
    // block reach is now governed by the blockInteractionRange() attribute and checked in the
    // picking/gamemode code, with no single override point. The "unlimited reach" feature needs
    // reimplementing against that (e.g. an attribute modifier) rather than a descriptor conversion.
    /*
    @Inject(method = "canInteractWithBlockAt", at = @At("RETURN"), cancellable = true)
    public void canInteractWithBlockAtXIV(BlockPos pos, double additionalRange, CallbackInfoReturnable<Boolean> cir) {
        if (Mod.enabled) {
            cir.setReturnValue(true);
            cir.cancel();
        }
    }
    */
}
