package com.cleannrooster.dungeons_iso.api.cullers.room;

import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.util.math.BlockPos;

/**
 * The set of blocks to remove between the camera and the player, expressed as whole shapes.
 *
 * <p>This replaced the cylinder rather than filtering it. A cylinder answers "is this block near
 * the camera-to-player axis", which is the wrong question twice over: it is proximity rather than
 * visibility, so terrain the player is plainly visible past still qualifies; and it is per block
 * rather than per shape, so it bores a clean tube through a rock face and leaves the rest standing.
 *
 * <p>Instead, rays are cast from the camera to sample points on the player's hitbox, whatever they
 * hit is segmented into connected shapes, and each shape is judged whole: it is either entirely
 * removed or entirely left alone. Shapes are taken nearest the player first, and a shape whose
 * flood hit the size cap is never taken at all — a half-resolved shape would cull as a ragged blob,
 * which is the exact artifact this is meant to avoid. When the budget runs out the result is simply
 * fewer complete shapes, never a partial one, and never a fallback to the cylinder.
 *
 * <p><b>Immutable after construction</b>, for the same reason as {@link RoomSnapshot}: it is read
 * concurrently from Sodium's chunk build workers.
 */
public final class SightlineMask {

    /** Blocks belonging to shapes selected for removal. Complete shapes only. */
    private final LongOpenHashSet occluding;
    /** Section coordinate -> hash of everything in this mask that affects its mesh. */
    private final Long2LongOpenHashMap sectionHashes;
    /** Hash of the whole result, so an unchanged rescan can be recognised and discarded. */
    private final long contentHash;

    private final float visibleFraction;
    private final boolean suppressed;
    private final int shapesFound;
    private final int shapesCulled;
    private final int shapesIncomplete;

    SightlineMask(LongOpenHashSet occluding, Long2LongOpenHashMap sectionHashes, long contentHash,
                  float visibleFraction, boolean suppressed, int shapesFound, int shapesCulled,
                  int shapesIncomplete) {
        this.occluding = occluding;
        this.sectionHashes = sectionHashes;
        this.contentHash = contentHash;
        this.visibleFraction = visibleFraction;
        this.suppressed = suppressed;
        this.shapesFound = shapesFound;
        this.shapesCulled = shapesCulled;
        this.shapesIncomplete = shapesIncomplete;
    }

    /** True when enough of the player is visible that nothing should be removed at all. */
    public boolean suppressesCulling() {
        return this.suppressed;
    }

    /**
     * True if this block belongs to a shape selected for removal. Hot path — one hash lookup,
     * called from Sodium's chunk build workers.
     */
    public boolean isOccluding(int x, int y, int z) {
        return !this.suppressed && this.occluding.contains(BlockPos.asLong(x, y, z));
    }

    /** The removed blocks, for the ghost pass to draw back translucently. Read-only. */
    public LongSet blocks() {
        return this.occluding;
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

    public float visibleFraction() {
        return this.visibleFraction;
    }

    public int shapesFound() {
        return this.shapesFound;
    }

    public int shapesCulled() {
        return this.shapesCulled;
    }

    /** Shapes that hit the size cap and were therefore skipped rather than partly culled. */
    public int shapesIncomplete() {
        return this.shapesIncomplete;
    }

    public int blockCount() {
        return this.occluding.size();
    }
}
