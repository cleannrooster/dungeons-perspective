package com.cleannrooster.dungeons_iso.api;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.render.chunk.ChunkBuilder;

import java.util.concurrent.BlockingQueue;

public abstract interface WorldRendererAccessor {
    BlockingQueue<ChunkBuilder.BuiltChunk> chunks();
}