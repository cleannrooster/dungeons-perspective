package com.cleannrooster.dungeons_iso.api;

import net.minecraft.util.math.BlockPos;

import java.util.List;

public interface BlockCullerUser {
    List<BlockPos> getCulledBlocks();
    void resetCulledBlocks();
}
