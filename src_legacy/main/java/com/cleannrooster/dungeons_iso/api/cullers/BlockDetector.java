package com.cleannrooster.dungeons_iso.api.cullers;

import com.cleannrooster.dungeons_iso.api.BlockCuller;
import com.cleannrooster.dungeons_iso.mod.Mod;
import net.minecraft.block.Block;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.WallMountedBlock;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.List;

public class BlockDetector implements BlockCuller {

    @Override
    public boolean shouldForceCull() {
        return false;
    }
    @Override
    public boolean shouldForceNonCull() {
        return true;
    }
    @Override
    public boolean cullBlocks(BlockPos blockPos, Camera camera, Entity cameraEntity) {
        return this.shouldCull(blockPos, camera, cameraEntity);
    }

    @Override
    public float blockTransparancy(BlockPos pos) {
        return 0;
    }

    public boolean shouldCull(BlockPos blockPos, Camera camera, Entity cameraEntity) {
        try {
            if (camera == null || cameraEntity == null) {
                return false;
            }

            Vec3d blockCenter = blockPos.toCenterPos();
            Vec3d entityPos = cameraEntity.getPos();
            Vec3d cameraPos = camera.getPos();

            if (isIgnoredType(cameraEntity.getWorld().getBlockState(blockPos).getBlock())) {
                return false;
            }

            if (blockCenter.getY() <= entityPos.getY() + 2) {
                return false;
            }

            Vec3d vec1 = blockCenter.subtract(cameraPos);
            Vec3d vec2 = cameraEntity.getEyePos().subtract(cameraPos);

            if (cameraEntity instanceof PlayerEntity player) {
                vec1 = vec1.add(player.getMovement().normalize().multiply(2));
            }

            double calc_theta = BlockCuller.angleBetween(vec2, vec1);
            return (calc_theta < 3 * Math.pow(0.9, Mod.zoom)) || cameraPos.distanceTo(blockCenter) < 5;
        } catch (Exception ignored) {
        }
        return false;
    }

    @Override
    public boolean shouldIgnoreBlockPick(BlockPos blockPos, Camera camera, Entity cameraEntity) {
        if (blockPos == null || isIgnoredType(cameraEntity.getWorld().getBlockState(blockPos).getBlock())) {
            return false;
        }

        Vec3d blockCenter = blockPos.toCenterPos();
        if (cameraEntity instanceof PlayerEntity player
                && blockCenter.distanceTo(cameraEntity.getEyePos()) > player.getBlockInteractionRange()
                && blockCenter.getY() > cameraEntity.getY() + 1) {
            return UP.dotProduct(blockCenter.subtract(cameraEntity.getPos()).normalize()) > 0.5F;
        }
        return false;
    }

    List<Class<? extends Block>> ignoredTypes = List.of(WallMountedBlock.class, DoorBlock.class);
    public boolean isIgnoredType(Block block) {
        for (Class<? extends Block> ignoredType : ignoredTypes) {
            if (ignoredType.isInstance(block)) {
                return true;
            }
        }
        return false;
    }
    @Override
    public int frequency() {
        return 1;
    }

    @Override
    public void resetCulledBlocks() {
    }

}
