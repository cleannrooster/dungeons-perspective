package com.cleannrooster.dungeons_iso.mixin;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.cleannrooster.dungeons_iso.mod.Mod;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(method = "shouldRenderBlockOutline", at = @At("HEAD"), cancellable = true)
    private void shouldRenderBlockOutlineXIV(CallbackInfoReturnable<Boolean> cir) {
        if (Mod.enabled && Mod.crosshairTarget instanceof BlockHitResult result) {
            cir.setReturnValue(true);
        }
    }

    // NOTE (26.2 port): the orthographic-projection, frustum-culling-zoom and debug-zoom
    // injections were removed here. They targeted GameRenderer#getBasicProjectionMatrix /
    // #renderWorld and the zoom/zoomX/zoomY fields, all deleted in 26.2's render pipeline rewrite
    // (projection now lives on Camera; LevelRenderer#render takes a GpuBufferSlice projection).
    // These features need a fresh implementation against the new pipeline, not a descriptor swap.
}
