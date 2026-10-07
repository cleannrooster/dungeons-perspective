package com.cleannrooster.dungeons_iso.compat;

import com.cleannrooster.dungeons_iso.api.cullers.room.ChunkRebuildScheduler;
import net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer;

/**
 * Section rebuilds through Sodium's renderer.
 *
 * <p>Every reference to a Sodium class in the rebuild path lives in this file, so that a no-Sodium
 * install never loads it. Instantiated reflectively from {@link ChunkRebuildScheduler#get()}.
 */
public final class SodiumRebuildScheduler implements ChunkRebuildScheduler {

    @Override
    public void scheduleSection(int sectionX, int sectionY, int sectionZ) {
        SodiumWorldRenderer renderer;
        try {
            renderer = SodiumWorldRenderer.instance();
        } catch (Throwable ignored) {
            // Not initialised yet — between world load and the renderer coming up.
            return;
        }
        if (renderer == null) {
            return;
        }

        int minX = sectionX << 4;
        int minY = sectionY << 4;
        int minZ = sectionZ << 4;
        try {
            renderer.scheduleRebuildForBlockArea(
                    minX, minY, minZ, minX + 15, minY + 15, minZ + 15, false);
        } catch (Throwable ignored) {
        }
    }
}
