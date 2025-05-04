package com.cleannrooster.dungeons_iso.mixin.client;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.cleannrooster.dungeons_iso.ClientInit;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin {

    @Inject(method = "canInteractWithBlockAt", at = @At("RETURN"), cancellable = true)
    public void canInteractWithBlockAtXIV(BlockPos pos, double additionalRange, CallbackInfoReturnable<Boolean> cir) {
        if (ClientInit.getCapabilities().unlimitedReach()) {
            cir.setReturnValue(true);
            cir.cancel();

        }
    }

}