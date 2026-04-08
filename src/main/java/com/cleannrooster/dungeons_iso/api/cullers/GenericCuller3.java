package com.cleannrooster.dungeons_iso.api.cullers;

import com.cleannrooster.dungeons_iso.api.BlockCuller;
import com.cleannrooster.dungeons_iso.api.MinecraftClientAccessor;
import com.cleannrooster.dungeons_iso.config.Config;
import com.cleannrooster.dungeons_iso.mod.Mod;
import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.List;

public class GenericCuller3 implements BlockCuller {

    @Override
    public boolean shouldForceCull() {
        return true;
    }

    @Override
    public boolean shouldForceNonCull() {
        return false;
    }

    @Override
    public boolean cullBlocks(BlockPos blockPos, Camera camera, Entity cameraEntity) {
        try {
            return this.shouldCull(blockPos, camera, cameraEntity);
        } catch (Exception ignored) {
        }
        return false;
    }

    @Override
    public float blockTransparancy(BlockPos pos) {
        return 0;
    }

    public final Vec3d getRotationVec(Entity entity, float tickDelta) {
        return entity.getRotationVector(entity.getPitch(tickDelta), entity.getHeadYaw());
    }

    /**
     * Computes the squared perpendicular distance from a point to a line defined by origin + direction.
     * Returns the squared distance to avoid sqrt.
     */
    private static double perpDistSqToAxis(Vec3d point, Vec3d axisOrigin, Vec3d axis, double axisLenSq) {
        Vec3d toPoint = point.subtract(axisOrigin);
        // cross product components: toPoint x axis
        double cx = toPoint.y * axis.z - toPoint.z * axis.y;
        double cy = toPoint.z * axis.x - toPoint.x * axis.z;
        double cz = toPoint.x * axis.y - toPoint.y * axis.x;
        // |cross|^2 / |axis|^2 = perpendicular distance squared
        return (cx * cx + cy * cy + cz * cz) / axisLenSq;
    }

    /**
     * Computes the animated cylinder radius based on the configured cullAngle and the transition ramp.
     */
    private static double getAnimatedRadius(Entity cameraEntity) {
        double animFactor = 0.1 * Math.min(10, Math.min(
                cameraEntity.getWorld().getTime() - Mod.startTime + 2,
                10 - Mod.endTime));
        return animFactor * Config.GSON.instance().cullAngle;
    }

    /**
     * Computes the effective radius at a given point along the axis.
     * Within 5 blocks of the origin, uses a cone (linear taper to 0).
     * Beyond 5 blocks, uses the full cylinder radius.
     */
    private static double effectiveRadiusSq(Vec3d toPoint, Vec3d axis, double axisLenSq, double radius) {
        double axisLen = Math.sqrt(axisLenSq);
        double projDist = (toPoint.x * axis.x + toPoint.y * axis.y + toPoint.z * axis.z) / axisLen;
        // Cone near player: half-angle from config, transitions to cylinder where cone meets full radius
        double tanHalfAngle = Math.tan(Math.toRadians(Config.GSON.instance().coneHalfAngle));
        double coneRadius = projDist * tanHalfAngle;
        double effRadius = Math.min(coneRadius, radius);
        return effRadius * effRadius;
    }

    @Override
    public boolean shouldCullAlt(BlockPos blockPos, BlockPos fromPos, Camera camera, Entity cameraEntity) {
        if (!((MinecraftClientAccessor) MinecraftClient.getInstance()).shouldRebuild() || camera == null || cameraEntity == null) {
            return false;
        }

        Vec3d blockCenter = blockPos.toCenterPos();
        Vec3d fromCenter = fromPos.toCenterPos();

        if (isIgnoredType(cameraEntity.getWorld().getBlockState(blockPos).getBlock())) {
            return false;
        }

        if (blockCenter.getY() <= fromCenter.getY()) {
            return false;
        }

        // Close to camera always culls
        if (Mod.preMod.distanceTo(blockCenter) < 5) {
            return true;
        }

        // Cylinder/cone axis: from fromPos toward camera
        Vec3d axis = Mod.preMod.subtract(fromCenter);
        double axisLenSq = axis.lengthSquared();
        if (axisLenSq == 0) return false;

        double radius = getAnimatedRadius(cameraEntity);
        double perpDistSq = perpDistSqToAxis(blockCenter, fromCenter, axis, axisLenSq);
        Vec3d toPoint = blockCenter.subtract(fromCenter);

        return perpDistSq <= effectiveRadiusSq(toPoint, axis, axisLenSq, radius);
    }

    public boolean shouldCull(BlockPos blockPos, Camera camera, Entity cameraEntity) {
        if (!((MinecraftClientAccessor) MinecraftClient.getInstance()).shouldRebuild() || camera == null || cameraEntity == null) {
            return false;
        }

        Vec3d blockCenter = blockPos.toCenterPos();
        Vec3d entityPos = cameraEntity.getPos();

        if (isIgnoredType(cameraEntity.getWorld().getBlockState(blockPos).getBlock())) {
            return false;
        }

        if (blockCenter.getY() <= entityPos.getY() + 1) {
            return false;
        }

        // Close to camera always culls
        if (Mod.preMod.distanceTo(blockCenter) < 5) {
            return true;
        }

        // Cylinder/cone axis: from player toward camera
        Vec3d axis = Mod.preMod.subtract(entityPos);
        double axisLenSq = axis.lengthSquared();
        if (axisLenSq == 0) return false;

        double radius = getAnimatedRadius(cameraEntity);
        double perpDistSq = perpDistSqToAxis(blockCenter, entityPos, axis, axisLenSq);
        Vec3d toPoint = blockCenter.subtract(entityPos);

        return perpDistSq <= effectiveRadiusSq(toPoint, axis, axisLenSq, radius);
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

    List<Class<? extends Block>> ignoredTypes = List.of(VaultBlock.class, SpawnerBlock.class, TrialSpawnerBlock.class, WallMountedBlock.class, LadderBlock.class, DoorBlock.class);
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
