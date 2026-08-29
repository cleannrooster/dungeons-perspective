package com.cleannrooster.dungeons_iso.api.cullers.room;

import net.minecraft.client.Minecraft;

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

    @Override
    public void scheduleSection(int sectionX, int sectionY, int sectionZ) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) {
            return;
        }

        try {
            client.level.setSectionDirtyWithNeighbors(sectionX, sectionY, sectionZ);
        } catch (Throwable ignored) {
        }
    }
}
