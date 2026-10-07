package com.cleannrooster.dungeons_iso.api.cullers.room;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.WorldRenderer;

/**
 * Section rebuilds through vanilla's chunk renderer.
 *
 * <p>{@link WorldRenderer#scheduleBlockRender(int, int, int)} takes section coordinates and marks
 * exactly one section dirty — the equivalent of Sodium's {@code scheduleRebuildForBlockArea},
 * minus the importance flag, which vanilla decides for itself by distance to the camera.
 *
 * <p>The six face neighbours go with it. A block removed against a section boundary changes which
 * faces the section next door has to draw, and that section is not in the scanner's result because
 * nothing inside it was culled. Vanilla's own block-box overload expands by a block for the same
 * reason, but it does so by iterating every block coordinate in the box — 5,832 calls to schedule
 * one section, where seven will do.
 */
public final class VanillaRebuildScheduler implements ChunkRebuildScheduler {

    private static final int[][] NEIGHBOURS = {
            {0, 0, 0}, {-1, 0, 0}, {1, 0, 0}, {0, -1, 0}, {0, 1, 0}, {0, 0, -1}, {0, 0, 1}
    };

    @Override
    public void scheduleSection(int sectionX, int sectionY, int sectionZ) {
        WorldRenderer renderer = MinecraftClient.getInstance().worldRenderer;
        if (renderer == null) {
            return;
        }

        try {
            for (int[] offset : NEIGHBOURS) {
                renderer.scheduleBlockRender(
                        sectionX + offset[0], sectionY + offset[1], sectionZ + offset[2]);
            }
        } catch (Throwable ignored) {
        }
    }
}
