package com.cleannrooster.dungeons_iso.mixin;

import com.cleannrooster.dungeons_iso.api.BuiltChunkAccessor;
import com.cleannrooster.dungeons_iso.api.ChunkDataAccessor;
import com.cleannrooster.dungeons_iso.api.MinecraftClientAccessor;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SectionRenderDispatcher.RenderSection.class)
public class ChunkDataMixin  implements BuiltChunkAccessor {
    @Shadow
    private boolean needsImportantRebuild;

    public void setNeedsImportantRebuild(boolean needsImportantRebuild) {
        this.needsImportantRebuild = needsImportantRebuild;
    }



}
