package com.cleannrooster.dungeons_iso.mixin;

import com.cleannrooster.dungeons_iso.mod.Mod;
import net.caffeinemc.mods.sodium.fabric.block.FabricBlockAccess;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FabricBlockAccess.class)
public class FabricBlockAccessMixin {
    @Inject(method = "shouldBlockEntityGlow", at = @At("RETURN"), cancellable = true)

    public void shouldBlockEntityGlowXIV(BlockEntity blockEntity, ClientPlayerEntity player, CallbackInfoReturnable<Boolean> info) {
        if(Mod.enabled && Mod.crosshairTarget != null &&  blockEntity.getPos().toCenterPos().distanceTo(Mod.crosshairTarget.getPos()) < 2) {
            info.setReturnValue(true);
        }
    }
    @Inject(method = "getLightEmission", at = @At("RETURN"), cancellable = true)
    public void getLightEmissionXIV(BlockState state, BlockRenderView level, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        if(Mod.enabled && Mod.crosshairTarget != null && pos.toCenterPos().distanceTo(Mod.crosshairTarget.getPos()) < 2 &&  MinecraftClient.getInstance().player != null && Mod.isInteractable(pos)) {

            cir.setReturnValue(Math.max(15,state.getLuminance()));
        }
    }

}
