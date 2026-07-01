package com.cleannrooster.dungeons_iso.mixin;

import com.cleannrooster.dungeons_iso.mod.Mod;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.SectionPos;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {
    @Shadow
    private @Nullable ViewArea viewArea;

    @Inject(
            method = "repositionCamera", at = @At("HEAD"), cancellable = true
    )    private void repositionCameraXIV(final CameraRenderState camera, CallbackInfo ci) {
        if(Mod.enabled || viewArea == null) {
            ci.cancel();
        }
    }
}
