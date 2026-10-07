package com.cleannrooster.dungeons_iso.api.cullers.room;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.tag.BlockTags;

/**
 * What kind of thing a block belongs to, used to bound connected-component floods.
 *
 * <p>This is the piece that makes outdoor culling possible at all. Solid blocks are one connected
 * component reaching the horizon — a tree trunk touches dirt, dirt touches the whole world — so an
 * unrestricted flood from any seed blows its size cap and the shape gets discarded as unresolved.
 * Restricting the flood to blocks of the same class gives the boundary back: logs and leaves stop
 * at the ground, so a tree resolves as a complete object standing on terrain it is no longer
 * connected to.
 *
 * <p>{@link #TERRAIN} is the residue — dirt, stone, sand — and it has no natural boundary at any
 * scale, which is why it is handled by silhouette rather than by component.
 *
 * <p>Classification reads only cached tag membership on the {@link BlockState}, so it is safe to
 * call from the scanner's worker thread.
 */
public enum OccluderClass {

    /** Trees, huge fungi, bamboo — anything that grows and has a canopy. */
    TREE,
    /** Player- or structure-built material: planks, bricks, walls, glass, wool. */
    BUILT,
    /** Natural ground. Connected to everything, so never treated as a component. */
    TERRAIN;

    /**
     * True for the load-bearing part of a tree rather than its canopy.
     *
     * <p>Used to keep a canopy tied to its own trunk. Leaves of neighbouring trees touch, and
     * diagonal connectivity follows them happily, so without a notion of "how far is this leaf from
     * a log" a single flood walks an entire forest.
     */
    public static boolean isTrunk(BlockState state) {
        return state != null
                && (state.isIn(BlockTags.LOGS)
                || state.isIn(BlockTags.BAMBOO_BLOCKS)
                || state.isOf(Blocks.MUSHROOM_STEM));
    }

    public static OccluderClass of(BlockState state) {
        if (state == null) {
            return TERRAIN;
        }

        if (state.isIn(BlockTags.LOGS)
                || state.isIn(BlockTags.LEAVES)
                || state.isIn(BlockTags.WART_BLOCKS)
                || state.isIn(BlockTags.BAMBOO_BLOCKS)
                || state.isOf(Blocks.MUSHROOM_STEM)
                || state.isOf(Blocks.BROWN_MUSHROOM_BLOCK)
                || state.isOf(Blocks.RED_MUSHROOM_BLOCK)
                // Jungle trees come draped in these. Left out of the class they are not part of the
                // tree, so they hang in the air after it is removed.
                || state.isOf(Blocks.VINE)
                || state.isOf(Blocks.GLOW_LICHEN)
                || state.isOf(Blocks.COCOA)) {
            return TREE;
        }

        if (state.isIn(BlockTags.PLANKS)
                || state.isIn(BlockTags.WOODEN_STAIRS)
                || state.isIn(BlockTags.WOODEN_SLABS)
                || state.isIn(BlockTags.WOODEN_FENCES)
                || state.isIn(BlockTags.WALLS)
                || state.isIn(BlockTags.FENCES)
                || state.isIn(BlockTags.STONE_BRICKS)
                || state.isIn(BlockTags.WOOL)
                || state.isIn(BlockTags.IMPERMEABLE)) {
            return BUILT;
        }

        return TERRAIN;
    }
}
