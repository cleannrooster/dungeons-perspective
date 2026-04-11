package com.cleannrooster.dungeons_iso.api;

import net.minecraft.client.render.chunk.ChunkOcclusionData;

public abstract interface BuiltChunkAccessor {
     void setNeedsImportantRebuild(boolean bool);
}