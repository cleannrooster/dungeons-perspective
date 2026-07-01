package com.cleannrooster.dungeons_iso.api;

import java.util.List;
import net.minecraft.core.BlockPos;

public interface BlockCullerUser {
    List<BlockPos> getCulledBlocks();
    void resetCulledBlocks();
}
