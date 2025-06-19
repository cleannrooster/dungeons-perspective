package com.cleannrooster.dungeons_iso.mixin.compat.sodium;

import com.cleannrooster.dungeons_iso.mod.Mod;
import com.mojang.blaze3d.systems.RenderSystem;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSectionManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(RenderSectionManager.class)
public class RenderSectionManagerMixin {
    @Shadow
    private @Nullable BlockPos lastCameraPosition;


    @Inject(at = @At("HEAD"), method = "getEffectiveRenderDistance", cancellable = true,remap = false)

    private void getEffectiveRenderDistanceXIV(CallbackInfoReturnable<Float> cir) {
        if(Mod.enabled) {
            cir.setReturnValue(8F*16F);

        }
    }
    @Inject(at = @At("HEAD"), method = "shouldPrioritizeRebuild", cancellable = true,remap = false)

    private void shouldPrioritizeTaskXIV(RenderSection section,  CallbackInfoReturnable<Boolean> cir) {
        if(Mod.enabled && lastCameraPosition != null &&  MinecraftClient.getInstance().player != null) {
            cir.setReturnValue(section.getSquaredDistance(lastCameraPosition) < 64*64);

        }
    }
    @Inject(at = @At("HEAD"), method = "getRenderDistance", cancellable = true,remap = false)

    private void getRenderDistanceXIV(CallbackInfoReturnable<Float> cir) {
        if(Mod.enabled) {
            cir.setReturnValue(8F*16F);

        }
    }
/*    @Inject(at = @At("HEAD"), method = "shouldUseOcclusionCulling", cancellable = true,remap = false)

    private void shouldUseOcclusionCullingXIV(Camera camera, boolean spectator,CallbackInfoReturnable<Boolean> cir) {
        if(Mod.enabled){
            cir.setReturnValue(false);
        }
    }*/
  /*      @Inject(at = @At("HEAD"), method = "isSectionVisible", cancellable = true,remap = false)
    public void isSectionVisibleXIV(int x, int y, int z, CallbackInfoReturnable<Boolean> cir) {
        if(Mod.enabled){
            cir.setReturnValue(true);
        }
    }*/
}
