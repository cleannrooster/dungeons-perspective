package com.cleannrooster.dungeons_iso.api;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;

import java.util.List;

public interface BlockCuller {
    boolean cullBlocks( BlockPos blockPos, Camera camera, Entity cameraEntity);
    List<BlockPos> getCulledBlocks();
    void resetCulledBlocks();
    default int frequency(){
        return 5;
    }
    default boolean isCulled(BlockPos pos){
        return getCulledBlocks().contains(pos);
    }
     boolean shouldCull(BlockPos blockPos, Camera camera, Entity cameraEntity);
     boolean isIgnoredType(Block block);
     float blockTransparancy(BlockPos pos);
    public  class TransparentBlock {
        public TransparentBlock(BlockPos pos, float transparency){
            this.transparency = transparency;
            this.pos = pos;
        }
        public float transparency = 0.0F;
        public BlockPos pos;

        public void tickOpacity(){

            this.transparency = this.transparency- transparencyAdd();
            this.transparency =Math.clamp(this.transparency,0F,1F);
        }
        public void tickTransparency(){
            this.transparency = this.transparency+transparencyAdd();
            this.transparency = Math.clamp(this.transparency,0F,1F);

        }
        public float transparencyAdd(){
            return 0.05F;
        }

    }
}
