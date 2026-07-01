package com.cleannrooster.dungeons_iso.api.cullers;

import com.cleannrooster.dungeons_iso.api.BlockCuller;
import com.cleannrooster.dungeons_iso.mod.Mod;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.phys.Vec3;

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

            Vec3 blockCenter = Vec3.atCenterOf(blockPos);
            Vec3 entityPos = cameraEntity.position();
            Vec3 cameraPos = camera.position();

            if (isIgnoredType(cameraEntity.level().getBlockState(blockPos).getBlock())) {
                return false;
            }

            if (blockCenter.y() <= entityPos.y() + 2) {
                return false;
            }

            Vec3 vec1 = blockCenter.subtract(cameraPos);
            Vec3 vec2 = cameraEntity.getEyePosition().subtract(cameraPos);

            if (cameraEntity instanceof Player player) {
                vec1 = vec1.add(player.getKnownMovement().normalize().scale(2));
            }

            double calc_theta = BlockCuller.angleBetween(vec2, vec1);
            return (calc_theta < 3 * Math.pow(0.9, Mod.zoom)) || cameraPos.distanceTo(blockCenter) < 5;
        } catch (Exception ignored) {
        }
        return false;
    }

    @Override
    public boolean shouldIgnoreBlockPick(BlockPos blockPos, Camera camera, Entity cameraEntity) {
        if (blockPos == null || isIgnoredType(cameraEntity.level().getBlockState(blockPos).getBlock())) {
            return false;
        }

        Vec3 blockCenter = Vec3.atCenterOf(blockPos);
        if (cameraEntity instanceof Player player
                && blockCenter.distanceTo(cameraEntity.getEyePosition()) > player.blockInteractionRange()
                && blockCenter.y() > cameraEntity.getY() + 1) {
            return UP.dot(blockCenter.subtract(cameraEntity.position()).normalize()) > 0.5F;
        }
        return false;
    }

    public boolean isIgnoredType(Block block) {
        return block instanceof FaceAttachedHorizontalDirectionalBlock || block instanceof DoorBlock;
    }
    @Override
    public int frequency() {
        return 1;
    }

    @Override
    public void resetCulledBlocks() {
    }

}
