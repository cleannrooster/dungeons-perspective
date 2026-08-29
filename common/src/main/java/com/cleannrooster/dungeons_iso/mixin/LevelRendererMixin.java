package com.cleannrooster.dungeons_iso.mixin;

import com.cleannrooster.dungeons_iso.api.cullers.room.CullingBackdrop;
import com.cleannrooster.dungeons_iso.api.ContextualTargetOutline;
import com.cleannrooster.dungeons_iso.api.cullers.room.GhostRenderer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.SectionPos;
import org.jspecify.annotations.Nullable;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {
    @Shadow
    private @Nullable ViewArea viewArea;

    @Inject(method = "submitFeatures", at = @At("TAIL"))
    private void submitCulledTerrainGhosts(LevelRenderState state, SubmitNodeCollector collector,
                                           boolean renderBlockOutline, CallbackInfo ci) {
        GhostRenderer.submit(state.cameraRenderState, collector);
        ContextualTargetOutline.submit(state.cameraRenderState, collector);
    }

    /**
     * 26.2 supplies the level target's clear colour directly to LevelRenderer#render. Updating the
     * backdrop here is early enough to replace that clear with black before the frame graph is
     * assembled, while remaining independent of the installed chunk renderer.
     */
    @Inject(method = "render", at = @At("HEAD"))
    private void prepareCutawayBackdrop(GraphicsResourceAllocator allocator,
                                        DeltaTracker deltaTracker, boolean renderBlockOutline,
                                        CameraRenderState camera, Matrix4fc projection,
                                        GpuBufferSlice fogBuffer, Vector4f clearColor,
                                        boolean renderWorld, CallbackInfo ci) {
        CullingBackdrop.update();
        if (CullingBackdrop.hidesSky()) {
            clearColor.set(0.0F, 0.0F, 0.0F, 1.0F);
        }
    }

    @Inject(method = "addSkyPass", at = @At("HEAD"), cancellable = true)
    private void hideSkyBehindCutaway(FrameGraphBuilder frameGraph, CameraRenderState camera,
                                      GpuBufferSlice fogBuffer, CallbackInfo ci) {
        if (CullingBackdrop.hidesSky()) {
            ci.cancel();
        }
    }

    /** NeoForge may request a camera reposition during the frame in which the ViewArea is reset. */
    @Inject(method = "repositionCamera", at = @At("HEAD"), cancellable = true)
    private void guardUninitializedViewArea(CameraRenderState camera, CallbackInfo ci) {
        if (this.viewArea == null) {
            ci.cancel();
        }
    }

    /**
     * The isometric camera can sit many sections away from the player. Moving ViewArea to that
     * visual camera unloads every terrain section around the player. Keep chunk management centred
     * on the camera entity while the actual camera position continues to drive rendering.
     */
    @ModifyArg(
            method = "repositionCamera",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/ViewArea;repositionCamera(Lnet/minecraft/core/SectionPos;)Z"),
            index = 0
    )
    private SectionPos useCameraEntitySection(SectionPos visualCameraSection) {
        if (com.cleannrooster.dungeons_iso.mod.Mod.enabled) {
            var entity = Minecraft.getInstance().getCameraEntity();
            if (entity != null) {
                return SectionPos.of(entity.blockPosition());
            }
        }
        return visualCameraSection;
    }

}
