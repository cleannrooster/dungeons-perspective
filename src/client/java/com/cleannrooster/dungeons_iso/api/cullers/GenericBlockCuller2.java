package com.cleannrooster.dungeons_iso.api.cullers;

import com.cleannrooster.dungeons_iso.api.BlockCuller;
import com.cleannrooster.dungeons_iso.mod.Mod;
import net.minecraft.client.Camera;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class GenericBlockCuller2 implements BlockCuller {

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
        if (camera == null || cameraEntity == null) {
            return false;
        }

        Vec3 blockCenter = blockPos.toCenterPos();
        Vec3 entityPos = cameraEntity.getPos();
        Vec3 cameraPos = camera.getMainCameraPos();

        if (isIgnoredType(cameraEntity.getWorld().getBlockState(blockPos).getBlock())) {
            return false;
        }

        if (blockCenter.getY() <= entityPos.getY() + 1) {
            return false;
        }

        Vec3 vec1 = blockCenter.subtract(cameraPos);
        Vec3 vec2 = entityPos.subtract(cameraPos);

        if (cameraEntity instanceof Player player) {
            vec1 = vec1.add(player.getMovement().normalize().multiply(2));
        }

        double calc_theta = BlockCuller.angleBetween(vec2, vec1);
        return (calc_theta < 90 * Math.pow(0.9, Mod.zoom)) || cameraPos.distanceTo(blockCenter) < 5;
    }

    @Override
    public boolean shouldIgnoreBlockPick(BlockPos blockPos, Camera camera, Entity cameraEntity) {
        if (blockPos == null || isIgnoredType(cameraEntity.getWorld().getBlockState(blockPos).getBlock())) {
            return false;
        }

        Vec3 blockCenter = blockPos.toCenterPos();
        if (cameraEntity instanceof Player player
                && blockCenter.distanceTo(cameraEntity.getEyePos()) > player.getBlockInteractionRange()
                && blockCenter.getY() > cameraEntity.getY() + 1) {
            return UP.dotProduct(blockCenter.subtract(cameraEntity.getPos()).normalize()) > 0.5F;
        }
        return false;
    }

    List<Class<? extends Block>> ignoredTypes = List.of(FaceAttachedHorizontalDirectionalBlock.class, DoorBlock.class);
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
