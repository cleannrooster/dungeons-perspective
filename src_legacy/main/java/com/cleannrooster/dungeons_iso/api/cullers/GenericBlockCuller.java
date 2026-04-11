package com.cleannrooster.dungeons_iso.api.cullers;

import com.cleannrooster.dungeons_iso.api.BlockCuller;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.WallMountedBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.List;

public class GenericBlockCuller implements BlockCuller {

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
        if (camera == null || cameraEntity == null || blockPos == null) {
            return false;
        }

        if (isIgnoredType(cameraEntity.getWorld().getBlockState(blockPos).getBlock())) {
            return false;
        }

        Vec3d blockCenter = blockPos.toCenterPos();
        Vec3d entityPos = cameraEntity.getPos();
        Vec3d cameraPos = camera.getPos();

        if (blockCenter.getY() <= entityPos.getY() + 1) {
            return false;
        }

        Vec3d toBlock = entityPos.subtract(blockCenter).normalize();
        Vec3d toCamera = MinecraftClient.getInstance().cameraEntity.getPos().subtract(cameraPos).normalize();

        if (toBlock.dotProduct(toCamera) > 0.71 || cameraPos.distanceTo(blockCenter) < 3) {
            return !cameraEntity.getWorld().getBlockState(blockPos).getBlock().equals(Blocks.AIR);
        }
        return false;
    }

    @Override
    public boolean shouldIgnoreBlockPick(BlockPos blockPos, Camera camera, Entity cameraEntity) {
        if (blockPos == null || isIgnoredType(cameraEntity.getWorld().getBlockState(blockPos).getBlock())) {
            return false;
        }

        Vec3d blockCenter = blockPos.toCenterPos();
        if (cameraEntity instanceof net.minecraft.entity.player.PlayerEntity player
                && blockCenter.distanceTo(cameraEntity.getEyePos()) > player.getBlockInteractionRange()
                && blockCenter.getY() > cameraEntity.getY() + 1) {
            return UP.dotProduct(blockCenter.subtract(cameraEntity.getPos()).normalize()) > 0.5F;
        }
        return false;
    }

    List<Class<? extends Block>> ignoredTypes = List.of(WallMountedBlock.class);
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
