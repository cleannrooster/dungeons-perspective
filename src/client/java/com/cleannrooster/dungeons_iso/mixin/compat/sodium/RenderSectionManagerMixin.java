package com.cleannrooster.dungeons_iso.mixin.compat.sodium;

import com.cleannrooster.dungeons_iso.mod.Mod;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSectionManager;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(RenderSectionManager.class)
public abstract class RenderSectionManagerMixin {


    @ModifyReturnValue(at = @At("RETURN"), method = "getEffectiveRenderDistance",remap = false)

    private float getEffectiveRenderDistanceXIV(float flot) {
        if(Mod.enabled) {
            flot =(Mod.getZoom()*Mod.zoomMetric+(4)*16 )* 1.15F;
        }
        return flot;
    }

    @ModifyReturnValue(at = @At("RETURN"), method = "getRenderDistance",remap = false)

    private float getRenderDistanceXIV(float flot) {
        if(Mod.enabled) {
            flot =(Mod.getZoom()*Mod.zoomMetric+(4)*16) * 1.15F;

        }
        return flot;

    }
 @Inject(at = @At("HEAD"), method = "shouldUseOcclusionCulling", cancellable = true,remap = false)

    private void shouldUseOcclusionCullingXIV(Camera camera, boolean spectator,CallbackInfoReturnable<Boolean> cir) {
        if(Mod.enabled&& Minecraft.getInstance().player != null && Mod.shouldRebuild()){
            cir.setReturnValue(false);
        }
    }


}
