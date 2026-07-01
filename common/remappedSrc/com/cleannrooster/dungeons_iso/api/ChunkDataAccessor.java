package com.cleannrooster.dungeons_iso.api;

import net.minecraft.client.renderer.chunk.VisibilitySet;

public abstract interface ChunkDataAccessor {
    VisibilitySet getOcclusionGraph();
}