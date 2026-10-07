package com.cleannrooster.dungeons_iso.api.cullers.room;

import net.minecraft.block.BlockState;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;

/**
 * A fixed set of {@link WorldChunk} references captured on the client thread, readable from any
 * other thread afterwards.
 *
 * <p>The room scanner runs off-thread and must never touch {@code World} directly — chunk lookup
 * goes through the client's chunk manager, which swaps entries as chunks load and unload. Capturing
 * the references once (a few dozen array reads, microseconds) and then reading through them is both
 * cheap and safe: {@code PalettedContainer} replaces its backing data as one atomic field write, so
 * a concurrent block update yields either the old or the new value, never a torn one. A one-tick
 * stale block state is invisible for culling purposes.
 *
 * <p>Coordinates outside the captured area, or inside chunks that were not loaded, classify as
 * {@link #UNKNOWN} — the caller decides what that means. The flood treats it as a wall so it cannot
 * escape into unloaded space; the roof solver treats it as "no roof found" so it does not invent a
 * ceiling out of missing chunks.
 */
public final class ChunkView {

    /** Block is present and blocks movement. */
    public static final int SOLID = 0;
    /** Block is present and does not block movement. */
    public static final int PASSABLE = 1;
    /** Outside the captured area, or in an unloaded chunk. */
    public static final int UNKNOWN = 2;

    private final WorldChunk[] chunks;
    private final int minChunkX;
    private final int minChunkZ;
    private final int sizeX;
    private final int sizeZ;
    private final int bottomY;
    private final int topY;

    private ChunkView(WorldChunk[] chunks, int minChunkX, int minChunkZ, int sizeX, int sizeZ,
                      int bottomY, int topY) {
        this.chunks = chunks;
        this.minChunkX = minChunkX;
        this.minChunkZ = minChunkZ;
        this.sizeX = sizeX;
        this.sizeZ = sizeZ;
        this.bottomY = bottomY;
        this.topY = topY;
    }

    /**
     * Captures every chunk touching the square of the given block radius around the centre.
     * <b>Must be called on the client thread.</b> Returns null if the centre chunk is not loaded.
     */
    public static ChunkView capture(World world, int centerBlockX, int centerBlockZ, int radiusBlocks) {
        if (world == null) {
            return null;
        }

        int minChunkX = (centerBlockX - radiusBlocks) >> 4;
        int maxChunkX = (centerBlockX + radiusBlocks) >> 4;
        int minChunkZ = (centerBlockZ - radiusBlocks) >> 4;
        int maxChunkZ = (centerBlockZ + radiusBlocks) >> 4;

        int sizeX = maxChunkX - minChunkX + 1;
        int sizeZ = maxChunkZ - minChunkZ + 1;

        WorldChunk[] chunks = new WorldChunk[sizeX * sizeZ];
        boolean centerLoaded = false;
        int centerChunkX = centerBlockX >> 4;
        int centerChunkZ = centerBlockZ >> 4;

        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                WorldChunk chunk;
                try {
                    chunk = world.getChunk(cx, cz);
                } catch (Exception ignored) {
                    chunk = null;
                }
                if (chunk != null && chunk.isEmpty()) {
                    chunk = null;
                }
                chunks[(cx - minChunkX) * sizeZ + (cz - minChunkZ)] = chunk;
                if (chunk != null && cx == centerChunkX && cz == centerChunkZ) {
                    centerLoaded = true;
                }
            }
        }

        if (!centerLoaded) {
            return null;
        }
        return new ChunkView(chunks, minChunkX, minChunkZ, sizeX, sizeZ,
                world.getBottomY(), world.getTopY());
    }

    /** Returns {@link #SOLID}, {@link #PASSABLE} or {@link #UNKNOWN} for the given block. */
    public int classify(int x, int y, int z) {
        BlockState state = getBlockState(x, y, z);
        if (state == null) {
            return UNKNOWN;
        }
        // blocksMovement() reads a boolean cached when the BlockState was built. It needs no world
        // context and allocates nothing, which is what makes it usable from this thread — unlike
        // getCameraCollisionShape(world, pos, ctx), which queries the world for some blocks.
        return state.blocksMovement() ? SOLID : PASSABLE;
    }

    /** Returns the block state, or null if outside the captured area or in an unloaded chunk. */
    public BlockState getBlockState(int x, int y, int z) {
        if (y < bottomY || y >= topY) {
            return null;
        }

        int dx = (x >> 4) - minChunkX;
        int dz = (z >> 4) - minChunkZ;
        if (dx < 0 || dx >= sizeX || dz < 0 || dz >= sizeZ) {
            return null;
        }

        WorldChunk chunk = chunks[dx * sizeZ + dz];
        if (chunk == null) {
            return null;
        }

        ChunkSection[] sections = chunk.getSectionArray();
        int index = (y - bottomY) >> 4;
        if (index < 0 || index >= sections.length) {
            return null;
        }

        ChunkSection section = sections[index];
        if (section == null || section.isEmpty()) {
            // An empty section is genuinely all air, not missing data.
            return net.minecraft.block.Blocks.AIR.getDefaultState();
        }
        return section.getBlockState(x & 15, y & 15, z & 15);
    }

    public int getBottomY() {
        return bottomY;
    }

    public int getTopY() {
        return topY;
    }
}
