package com.cleannrooster.dungeons_iso.api.cullers.room;

import com.cleannrooster.dungeons_iso.ModCompat;
import com.cleannrooster.dungeons_iso.mod.Mod;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.CarpetBlock;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.LadderBlock;
import net.minecraft.block.SnowBlock;
import net.minecraft.block.SpawnerBlock;
import net.minecraft.block.TrialSpawnerBlock;
import net.minecraft.block.VaultBlock;
import net.minecraft.block.VineBlock;
import net.minecraft.block.WallMountedBlock;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.Direction;

/**
 * The culling decision, stated once, independent of who is building the mesh.
 *
 * <p>Sodium and vanilla have completely separate chunk-build pipelines, so the mod hooks each of
 * them separately — but the question being asked at those hooks is identical, and the scanners that
 * answer it ({@link RoomScanner}, {@link SightlineScanner}) are plain block-coordinate math with no
 * renderer dependency at all. Keeping the answer here means the two paths cannot drift: a block
 * that vanishes under Sodium vanishes without it.
 *
 * <p>Every query is safe to call from a chunk build worker.
 */
public final class TerrainCulling {

    /**
     * True when Sodium is installed. Sodium replaces vanilla's chunk-build pipeline wholesale, so
     * the vanilla hooks would never fire for terrain anyway — but they are also reachable from
     * other callers, and standing down explicitly is cheaper than reasoning about which.
     */
    public static final boolean SODIUM = ModCompat.isModLoaded("sodium");

    /**
     * Set once, on the client thread, by {@link #warmUp()}. Until then every query answers "not
     * culling" without touching anything else.
     *
     * <p>This exists to keep class loading off the chunk-build workers. {@code ChunkRendererRegion}
     * is meshed on a worker pool, so without this the first block of the first section built is a
     * worker thread's first touch of {@link TerrainCulling}, {@code Mod}, {@code Config}, and
     * through it YACL — meaning a worker triggers first-time loading and static init of a stack of
     * mod classes through ModLauncher's transforming classloader, while the render thread is doing
     * the same for its own reasons. That deadlocks silently: no exception, no log line, the world
     * simply never finishes loading. Nothing here is ever the first to load a class now.
     */
    private static volatile boolean ready;

    private TerrainCulling() {
    }

    /**
     * Loads and initialises everything the chunk-build path will touch, on the client thread.
     * Call once from client setup, well before any world exists.
     */
    public static void warmUp() {
        // Reading the field is enough to force each class through <clinit> here rather than later
        // on a worker. The results are deliberately discarded.
        boolean unusedSodium = SODIUM;
        boolean unusedEnabled = Mod.enabled;
        RoomScanner.INSTANCE.isActive();
        SightlineScanner.INSTANCE.isActive();
        ChunkRebuildScheduler.get();
        ready = true;
    }

    /**
     * Blocks that survive culling even when the terrain around them is removed — the things a
     * player needs to see and click in an opened-up room, plus the ones that would look broken
     * hanging in mid-air on their own.
     */
    public static boolean isProtected(Block block) {
        return block instanceof VaultBlock
                || block instanceof SpawnerBlock
                || block instanceof TrialSpawnerBlock
                || block instanceof WallMountedBlock
                || block instanceof DoorBlock;
    }

    /**
     * Whether the block at this position is being removed from the mesh, ignoring the protected
     * list — i.e. "does a culler have an opinion about this column".
     *
     * <p>Tier 1 is the room snapshot: it culls the roof of the room the player occupies (and the
     * walls and pillars that roof rests on) and explicitly protects the storey above, so a
     * {@code KEEP} is final and shape culling never gets to overrule it. {@code NO_OPINION} means
     * either no roof over this column — outdoors, or under a hole — or no scan yet, and tier 2
     * decides.
     *
     * <p>Tier 2 is the sightline scanner, which casts rays to the player, segments what they hit
     * into connected shapes, and removes shapes whole, nearest first.
     */
    public static boolean isRemoved(int x, int y, int z) {
        if (!ready || !Mod.enabled) {
            return false;
        }
        int room = RoomScanner.INSTANCE.test(x, y, z);
        if (room == RoomSnapshot.CULL) {
            return true;
        }
        if (room == RoomSnapshot.KEEP) {
            return false;
        }
        return SightlineScanner.INSTANCE.shouldCull(x, y, z);
    }

    /**
     * Whether this specific block should be dropped from the chunk mesh entirely.
     *
     * <p>Thin cover blocks are usually not part of an occluding shape themselves. They must follow
     * the terrain they are resting on or attached to, otherwise culling a roof leaves floating
     * snow, carpet, ladders, or vines behind. These checks use only immutable state plus the
     * already-published masks, so they remain safe on chunk-build workers.
     */
    public static boolean shouldRemove(BlockState state, int x, int y, int z) {
        Block block = state.getBlock();
        if (isProtected(block)) {
            return false;
        }
        if (isRemoved(x, y, z)) {
            return true;
        }
        if (block instanceof SnowBlock || block instanceof CarpetBlock) {
            return isRemoved(x, y - 1, z);
        }
        if (block instanceof LadderBlock) {
            Direction facing = state.get(Properties.HORIZONTAL_FACING);
            return isRemoved(x - facing.getOffsetX(), y, z - facing.getOffsetZ());
        }
        if (block instanceof VineBlock) {
            return attachedToRemovedTerrain(state, x, y, z);
        }
        return false;
    }

    private static boolean attachedToRemovedTerrain(BlockState state, int x, int y, int z) {
        if (state.get(Properties.UP) && isRemoved(x, y + 1, z)) {
            return true;
        }
        if (state.get(Properties.NORTH) && isRemoved(x, y, z - 1)) {
            return true;
        }
        if (state.get(Properties.SOUTH) && isRemoved(x, y, z + 1)) {
            return true;
        }
        if (state.get(Properties.WEST) && isRemoved(x - 1, y, z)) {
            return true;
        }
        return state.get(Properties.EAST) && isRemoved(x + 1, y, z);
    }

    /**
     * Both scanners idle. A single volatile read each, and the only thing standing between the
     * hot per-face path and two hash lookups, so it is worth testing first.
     */
    public static boolean idle() {
        return !ready
                || (!RoomScanner.INSTANCE.isActive() && !SightlineScanner.INSTANCE.isActive());
    }
}
