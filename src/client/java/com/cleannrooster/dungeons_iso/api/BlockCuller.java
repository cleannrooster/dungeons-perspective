package com.cleannrooster.dungeons_iso.api;

import net.minecraft.client.Camera;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public interface BlockCuller {
    Vec3 UP = new Vec3(0, 1, 0);

    static double angleBetween(Vec3 a, Vec3 b) {
        double dot = a.dotProduct(b);
        double lenSq = a.lengthSquared() * b.lengthSquared();
        if (lenSq == 0) return 0;
        double cosineTheta = dot / Math.sqrt(lenSq);
        double angle = Math.acos(Math.clamp(cosineTheta, -1.0, 1.0)) * 57.29577951308232;
        return Double.isNaN(angle) ? 0.0 : angle;
    }

    boolean cullBlocks( BlockPos blockPos, Camera camera, Entity cameraEntity);
    default List<BlockPos> getCulledBlocks(BlockPos blockPos, Camera camera, Entity cameraEntity){
        return List.of();
    };
    void resetCulledBlocks();
    default int frequency(){
        return 5;
    }
    default boolean isCulled(BlockPos pos){
        return false;
    }
     boolean shouldCull(BlockPos blockPos, Camera camera, Entity cameraEntity);
    default boolean shouldCullAlt(BlockPos blockPos,BlockPos fromPos, Camera camera, Entity cameraEntity){
        return false;
    }

    boolean shouldIgnoreBlockPick(BlockPos blockPos, Camera camera, Entity cameraEntity);
    boolean shouldForceCull();
    boolean shouldForceNonCull();


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
