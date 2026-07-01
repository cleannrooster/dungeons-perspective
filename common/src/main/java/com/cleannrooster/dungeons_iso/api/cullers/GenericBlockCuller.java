package com.cleannrooster.dungeons_iso.api.cullers;

import com.cleannrooster.dungeons_iso.api.BlockCuller;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.phys.Vec3;

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

        if (isIgnoredType(cameraEntity.level().getBlockState(blockPos).getBlock())) {
            return false;
        }

        Vec3 blockCenter = Vec3.atCenterOf(blockPos);
        Vec3 entityPos = cameraEntity.position();
        Vec3 cameraPos = camera.position();

        if (blockCenter.y() <= entityPos.y() + 1) {
            return false;
        }

        Vec3 toBlock = entityPos.subtract(blockCenter).normalize();
        Vec3 toCamera = Minecraft.getInstance().getCameraEntity().position().subtract(cameraPos).normalize();

        if (toBlock.dot(toCamera) > 0.71 || cameraPos.distanceTo(blockCenter) < 3) {
            return !cameraEntity.level().getBlockState(blockPos).getBlock().equals(Blocks.AIR);
        }
        return false;
    }

    @Override
    public boolean shouldIgnoreBlockPick(BlockPos blockPos, Camera camera, Entity cameraEntity) {
        if (blockPos == null || isIgnoredType(cameraEntity.level().getBlockState(blockPos).getBlock())) {
            return false;
        }

        Vec3 blockCenter = Vec3.atCenterOf(blockPos);
        if (cameraEntity instanceof net.minecraft.world.entity.player.Player player
                && blockCenter.distanceTo(cameraEntity.getEyePosition()) > player.blockInteractionRange()
                && blockCenter.y() > cameraEntity.getY() + 1) {
            return UP.dot(blockCenter.subtract(cameraEntity.position()).normalize()) > 0.5F;
        }
        return false;
    }

    public boolean isIgnoredType(Block block) {
        return block instanceof FaceAttachedHorizontalDirectionalBlock;
    }
    @Override
    public int frequency() {
        return 1;
    }

    @Override
    public void resetCulledBlocks() {
    }

}
