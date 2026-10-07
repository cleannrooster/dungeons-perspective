package com.cleannrooster.dungeons_iso.api.cullers.room;

import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * The finished result of one room scan: for every XZ column belonging to the room, the vertical
 * span of roof blocks to cull.
 *
 * <p><b>Immutable after construction.</b> Sodium meshes chunks on worker threads, so
 * {@link #test} is called concurrently from several threads at once. The maps are built by the
 * scanner and then never written again — the reference is published through a volatile field in
 * {@link RoomScanner}, so readers either see the whole old snapshot or the whole new one. Nothing
 * here may ever be mutated in place.
 */
public final class RoomSnapshot {

    /** This snapshot has no view on the block; the caller should fall through to the next culler. */
    public static final int NO_OPINION = 0;
    /** The block is part of the room's roof and should not render. */
    public static final int CULL = 1;
    /** The block belongs to the room but must render; no other culler should remove it. */
    public static final int KEEP = 2;

    /** Sentinel roof base meaning "this column is open to the sky". */
    static final int OPEN_SKY = Integer.MAX_VALUE;

    private static final long NO_COLUMN = Long.MIN_VALUE;

    /** packXZ -> (roofBaseY << 32) | (roofTopY & 0xFFFFFFFF). */
    private final Long2LongOpenHashMap columns;
    /** Section coordinate -> hash of everything in this snapshot that affects its mesh. */
    private final Long2LongOpenHashMap sectionHashes;
    /** Hash of the whole result, so an unchanged rescan can be recognised and discarded. */
    private final long contentHash;

    private final RegistryKey<World> dimension;
    private final BlockPos origin;
    private final int columnCount;

    RoomSnapshot(Long2LongOpenHashMap columns, Long2LongOpenHashMap sectionHashes,
                 long contentHash, RegistryKey<World> dimension, BlockPos origin) {
        this.columns = columns;
        this.columns.defaultReturnValue(NO_COLUMN);
        this.sectionHashes = sectionHashes;
        this.contentHash = contentHash;
        this.dimension = dimension;
        this.origin = origin;
        this.columnCount = columns.size();
    }

    /**
     * Decides the fate of a single block. Hot path — called once per block per section rebuild,
     * on Sodium's worker threads.
     */
    public int test(int x, int y, int z) {
        long packed = this.columns.get(packXZ(x, z));
        if (packed == NO_COLUMN) {
            return NO_OPINION;
        }

        int roofBase = (int) (packed >>> 32);
        if (roofBase == OPEN_SKY) {
            // No ceiling over this column — outdoors, or under a hole in the roof. The room
            // system has nothing useful to say; the occluder/cylinder tiers take over.
            return NO_OPINION;
        }

        if (y < roofBase) {
            // Inside the room. Leave the decision to the cylinder culler so interior obstructions
            // keep behaving as they do today.
            return NO_OPINION;
        }

        int roofTop = (int) packed;
        if (y <= roofTop) {
            return CULL;
        }

        // Above everything covering the room. Whatever is up here is not between the camera and
        // the player by way of this room, so protect it from the other cullers.
        return KEEP;
    }

    /** Receives every block this snapshot removes. */
    public interface BlockSink {
        void accept(int x, int y, int z);
    }

    /**
     * Enumerates the removed blocks, so the ghost pass can draw them back. Columns store a roof
     * span rather than individual blocks, so this expands them.
     */
    public void forEachCulledBlock(BlockSink sink) {
        for (it.unimi.dsi.fastutil.longs.LongIterator it = this.columns.keySet().iterator(); it.hasNext(); ) {
            long xz = it.nextLong();
            long packed = this.columns.get(xz);
            int roofBase = (int) (packed >>> 32);
            if (roofBase == OPEN_SKY) {
                continue;
            }
            int roofTop = (int) packed;
            int x = unpackX(xz);
            int z = unpackZ(xz);
            for (int y = roofBase; y <= roofTop; y++) {
                sink.accept(x, y, z);
            }
        }
    }

    /** True if this snapshot was built for the given dimension. */
    public boolean matches(RegistryKey<World> key) {
        return this.dimension != null && this.dimension.equals(key);
    }

    public LongSet sections() {
        return this.sectionHashes.keySet();
    }

    Long2LongOpenHashMap sectionHashes() {
        return this.sectionHashes;
    }

    long contentHash() {
        return this.contentHash;
    }

    public BlockPos origin() {
        return this.origin;
    }

    public int columnCount() {
        return this.columnCount;
    }

    public static long packXZ(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    public static int unpackX(long xz) {
        return (int) (xz >> 32);
    }

    public static int unpackZ(long xz) {
        return (int) xz;
    }

    static long packRoof(int roofBase, int roofTop) {
        return ((long) roofBase << 32) | (roofTop & 0xFFFFFFFFL);
    }
}
