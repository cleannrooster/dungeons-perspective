package com.cleannrooster.dungeons_iso.api;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;

public abstract interface WorldRendererAccessor {
    ObjectArrayList<SectionRenderDispatcher.RenderSection> chunks();
}