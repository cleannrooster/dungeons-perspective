package com.cleannrooster.dungeons_iso.api.cullers;

import com.cleannrooster.dungeons_iso.api.BlockCuller;
import com.cleannrooster.dungeons_iso.api.cullers.room.RoomScanner;
import com.cleannrooster.dungeons_iso.api.cullers.room.RoomSnapshot;
import net.minecraft.block.Block;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.WallMountedBlock;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;

import java.util.List;

/**
 * Adapter exposing {@link RoomScanner}'s results through the {@link BlockCuller} interface.
 *
 * <p>The flood itself used to live here and ran synchronously, from scratch, every single tick.
 * It now runs on a worker thread under a budget — see {@code RoomScanner} for the pipeline and
 * {@code SodiumCompat.run} for the per-tick driving. This class only forwards queries.
 */
public class FloodCuller implements BlockCuller {

    @Override
    public boolean shouldForceCull() {
        return true;
    }

    @Override
    public boolean shouldForceNonCull() {
        return false;
    }

    @Override
    public boolean cullBlocks(BlockPos blockPos, Camera camera, Entity cameraEntity) {
        return false;
    }

    @Override
    public float blockTransparancy(BlockPos pos) {
        return 0;
    }

    @Override
    public boolean shouldCull(BlockPos blockPos, Camera camera, Entity cameraEntity) {
        return isAboveFlood(blockPos);
    }

    @Override
    public boolean shouldIgnoreBlockPick(BlockPos blockPos, Camera camera, Entity cameraEntity) {
        return false;
    }

    @Override
    public boolean isIgnoredType(Block block) {
        return block instanceof WallMountedBlock || block instanceof DoorBlock;
    }

    @Override
    public int frequency() {
        return 1;
    }

    @Override
    public List<BlockPos> getCulledBlocks(BlockPos blockPos, Camera camera, Entity cameraEntity) {
        return List.of();
    }

    @Override
    public void resetCulledBlocks() {
    }

    /**
     * True if this block is part of the roof of the room the player currently occupies.
     * Safe to call from Sodium's chunk build workers.
     */
    public boolean isAboveFlood(BlockPos blockPos) {
        return RoomScanner.INSTANCE.test(blockPos.getX(), blockPos.getY(), blockPos.getZ())
                == RoomSnapshot.CULL;
    }
}
