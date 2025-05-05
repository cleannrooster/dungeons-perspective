package com.cleannrooster.dungeons_iso.api;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.render.chunk.ChunkBuilder;

public abstract interface WorldRendererAccessor {
    ObjectArrayList<ChunkBuilder.BuiltChunk> chunks();
}