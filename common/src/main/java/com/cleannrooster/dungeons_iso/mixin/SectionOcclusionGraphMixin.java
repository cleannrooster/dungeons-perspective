package com.cleannrooster.dungeons_iso.mixin;

import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SectionOcclusionGraph;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.ChunkLoadingRenderState;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

import java.util.concurrent.atomic.AtomicReference;

@Mixin(SectionOcclusionGraph.class)
public class SectionOcclusionGraphMixin {
    @Shadow @Final
    private AtomicReference<?> currentGraph;
    @Shadow
    private @Nullable ViewArea viewArea;

    @Inject(
            method = "runPartialUpdate", at = @At("HEAD"), cancellable = true
    )
    private void runPartialUpdateXIV(final CameraRenderState camera, final LongSet loadedExpectedChunks, CallbackInfo ci) {
        if(this.currentGraph.get() == null){
            ci.cancel();
        }
    }

    /**
     * NeoForge can render one frame while LevelRenderer is replacing its ViewArea. Vanilla clears
     * currentGraph during that hand-off, but these read paths assume it is always populated. An
     * empty visible-section result is correct for that transient frame and lets the next update
     * install the new graph normally.
     */
    @Inject(method = "addSectionsInFrustum", at = @At("HEAD"), cancellable = true)
    private void guardSectionsInFrustum(net.minecraft.client.renderer.culling.Frustum frustum,
                                        List<SectionRenderDispatcher.RenderSection> visible,
                                        List<SectionRenderDispatcher.RenderSection> nearby,
                                        CallbackInfo ci) {
        if (this.currentGraph.get() == null) {
            ci.cancel();
        }
    }

    @Inject(method = "schedulePropagationFrom", at = @At("HEAD"), cancellable = true)
    private void guardPropagation(SectionRenderDispatcher.RenderSection section, CallbackInfo ci) {
        if (this.currentGraph.get() == null) {
            ci.cancel();
        }
    }

    @Inject(method = "getNode", at = @At("HEAD"), cancellable = true)
    private void guardGetNode(SectionRenderDispatcher.RenderSection section,
                              CallbackInfoReturnable<SectionOcclusionGraph.Node> cir) {
        if (this.currentGraph.get() == null) {
            cir.setReturnValue(null);
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
