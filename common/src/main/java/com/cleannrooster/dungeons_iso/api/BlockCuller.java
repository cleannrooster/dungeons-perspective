package com.cleannrooster.dungeons_iso.api;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.List;

public interface BlockCuller {
    Vec3d UP = new Vec3d(0, 1, 0);

    static double angleBetween(Vec3d a, Vec3d b) {
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
