package com.cleannrooster.dungeons_iso.api;

import net.minecraft.block.enums.BlockFace;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.List;

public interface BlockCuller {
    List<BlockPos> cullBlocks( BlockPos blockPos, Camera camera, Entity cameraEntity, Direction face);
    List<BlockPos> getCulledBlocks();
    void resetCulledBlocks();

}
