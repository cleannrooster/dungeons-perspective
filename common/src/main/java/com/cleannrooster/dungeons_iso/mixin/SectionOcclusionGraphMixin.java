package com.cleannrooster.dungeons_iso.mixin;

import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SectionOcclusionGraph;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.ChunkLoadingRenderState;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.atomic.AtomicReference;

@Mixin(SectionOcclusionGraph.class)
public class SectionOcclusionGraphMixin {
    @Shadow
    private final AtomicReference<?> currentGraph = new AtomicReference();
    @Shadow
    private @Nullable ViewArea viewArea;

    @Inject(
            method = "runPartialUpdate", at = @At("HEAD"), cancellable = true
    )
    private void runPartialUpdateXIV(final CameraRenderState camera, final LongSet loadedExpectedChunks, CallbackInfo ci) {
        Object state = (Object) this.currentGraph.get();
        if(state == null){
            ci.cancel();
        }
    }
    @Inject(
            method = "update", at = @At("HEAD"), cancellable = true
    )
    public void updateXIV(final CameraRenderState camera, final int fov, final ChunkLoadingRenderState chunkLoadingRenderState, CallbackInfo ci ) {
        if(this.viewArea == null){
            ci.cancel();
        }


    }
}
