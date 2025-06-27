package com.cleannrooster.dungeons_iso.mixin;

import com.cleannrooster.dungeons_iso.api.BlockCuller;
import com.cleannrooster.dungeons_iso.compat.SodiumCompat;
import com.cleannrooster.dungeons_iso.mod.Mod;
import net.caffeinemc.mods.sodium.client.services.PlatformBlockAccess;
import net.caffeinemc.mods.sodium.fabric.block.FabricBlockAccess;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FabricBlockAccess.class)
public class FabricBlockAccessMixin {
    @Inject(method = "shouldBlockEntityGlow", at = @At("RETURN"), cancellable = true)

    public void shouldBlockEntityGlowXIV(BlockEntity blockEntity, ClientPlayerEntity player, CallbackInfoReturnable<Boolean> info) {
        try {

            if(Mod.enabled && Mod.mouseTarget instanceof BlockHitResult blockHitResult && blockHitResult.getBlockPos().equals(blockEntity.getPos())) {
            info.setReturnValue(true);
        }
        }
            catch(Exception ignored){

        }
    }




}
