package com.cleannrooster.dungeons_iso.api.cullers;

import com.cleannrooster.dungeons_iso.api.BlockCuller;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.phys.Vec3;

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

        Vec3 blockCenter = blockPos.toCenterPos();
        Vec3 entityPos = cameraEntity.getPos();
        Vec3 cameraPos = camera.getMainCameraPos();

        if (blockCenter.getY() <= entityPos.getY() + 1) {
            return false;
        }

        Vec3 toBlock = entityPos.subtract(blockCenter).normalize();
        Vec3 toCamera = Minecraft.getInstance().cameraEntity.getPos().subtract(cameraPos).normalize();

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

        Vec3 blockCenter = blockPos.toCenterPos();
        if (cameraEntity instanceof net.minecraft.entity.player.Player player
                && blockCenter.distanceTo(cameraEntity.getEyePos()) > player.getBlockInteractionRange()
                && blockCenter.getY() > cameraEntity.getY() + 1) {
            return UP.dotProduct(blockCenter.subtract(cameraEntity.getPos()).normalize()) > 0.5F;
        }
        return false;
    }

    List<Class<? extends Block>> ignoredTypes = List.of(FaceAttachedHorizontalDirectionalBlock.class);
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
