package com.cleannrooster.dungeons_iso.api.cullers;

import com.cleannrooster.dungeons_iso.api.BlockCuller;
import com.cleannrooster.dungeons_iso.api.MinecraftClientAccessor;
import com.cleannrooster.dungeons_iso.config.Config;
import com.cleannrooster.dungeons_iso.mod.Mod;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.SpawnerBlock;
import net.minecraft.world.level.block.TrialSpawnerBlock;
import net.minecraft.world.level.block.VaultBlock;
import net.minecraft.world.phys.Vec3;
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

    public final Vec3 getRotationVec(Entity entity, float tickDelta) {
        return entity.calculateViewVector(entity.getViewXRot(tickDelta), entity.getYHeadRot());
    }

    /**
     * Computes the squared perpendicular distance from a point to a line defined by origin + direction.
     * Returns the squared distance to avoid sqrt.
     */
    private static double perpDistSqToAxis(Vec3 point, Vec3 axisOrigin, Vec3 axis, double axisLenSq) {
        Vec3 toPoint = point.subtract(axisOrigin);
        // cross product components: toPoint x axis
        double cx = toPoint.y * axis.z - toPoint.z * axis.y;
        double cy = toPoint.z * axis.x - toPoint.x * axis.z;
        double cz = toPoint.x * axis.y - toPoint.y * axis.x;
        // |cross|^2 / |axis|^2 = perpendicular distance squared
        return (cx * cx + cy * cy + cz * cz) / axisLenSq;
    }

    // Cached trig value — recomputed only when the config value changes.
    private static float  cachedConeHalfAngle = Float.NaN;
    private static double cachedTanHalfAngle  = 0.0;

    /**
     * Computes the animated cylinder radius based on the configured cullAngle and the transition ramp.
     */
    private static double getAnimatedRadius(Entity cameraEntity) {
        double animFactor = 0.1 * Math.min(10, Math.min(
                cameraEntity.level().getGameTime() - Mod.startTime + 2,
                10 - Mod.endTime));
        return animFactor * Config.GSON.instance().cullAngle;
    }

    /**
     * Computes the effective radius at a given point along the axis.
     * Within 5 blocks of the origin, uses a cone (linear taper to 0).
     * Beyond 5 blocks, uses the full cylinder radius.
     */
    private static double effectiveRadiusSq(Vec3 toPoint, Vec3 axis, double axisLenSq, double radius) {
        double axisLen = Math.sqrt(axisLenSq);
        double projDist = (toPoint.x * axis.x + toPoint.y * axis.y + toPoint.z * axis.z) / axisLen;
        // Cone near player: half-angle from config, transitions to cylinder where cone meets full radius
        float configHalfAngle = Config.GSON.instance().coneHalfAngle;
        if (configHalfAngle != cachedConeHalfAngle) {
            cachedConeHalfAngle = configHalfAngle;
            cachedTanHalfAngle  = Math.tan(Math.toRadians(configHalfAngle));
        }
        double tanHalfAngle = cachedTanHalfAngle;
        double coneRadius = projDist * tanHalfAngle;
        double effRadius = Math.min(coneRadius, radius);
        return effRadius * effRadius;
    }

    @Override
    public boolean shouldCullAlt(BlockPos blockPos, BlockPos fromPos, Camera camera, Entity cameraEntity) {
        if (!((MinecraftClientAccessor) Minecraft.getInstance()).shouldRebuild() || camera == null || cameraEntity == null) {
            return false;
        }

        Vec3 blockCenter = Vec3.atCenterOf(blockPos);
        Vec3 fromCenter = Vec3.atCenterOf(fromPos);

        if (isIgnoredType(cameraEntity.level().getBlockState(blockPos).getBlock())) {
            return false;
        }

        if (blockCenter.y() <= fromCenter.y()) {
            return false;
        }

        // Close to camera always culls
        if (Mod.preMod.distanceTo(blockCenter) < 5) {
            return true;
        }

        // Cylinder/cone axis: from fromPos toward camera
        Vec3 axis = Mod.preMod.subtract(fromCenter);
        double axisLenSq = axis.lengthSqr();
        if (axisLenSq == 0) return false;

        double radius = getAnimatedRadius(cameraEntity);
        double perpDistSq = perpDistSqToAxis(blockCenter, fromCenter, axis, axisLenSq);
        Vec3 toPoint = blockCenter.subtract(fromCenter);

        return perpDistSq <= effectiveRadiusSq(toPoint, axis, axisLenSq, radius);
    }

    public boolean shouldCull(BlockPos blockPos, Camera camera, Entity cameraEntity) {
        if (!((MinecraftClientAccessor) Minecraft.getInstance()).shouldRebuild() || camera == null || cameraEntity == null) {
            return false;
        }

        Vec3 blockCenter = Vec3.atCenterOf(blockPos);
        Vec3 entityPos = cameraEntity.position();

        if (isIgnoredType(cameraEntity.level().getBlockState(blockPos).getBlock())) {
            return false;
        }

        if (blockCenter.y() <= entityPos.y() + 1) {
            return false;
        }

        // Close to camera always culls
        if (Mod.preMod.distanceTo(blockCenter) < 5) {
            return true;
        }

        // Cylinder/cone axis: from player toward camera
        Vec3 axis = Mod.preMod.subtract(entityPos);
        double axisLenSq = axis.lengthSqr();
        if (axisLenSq == 0) return false;

        double radius = getAnimatedRadius(cameraEntity);
        double perpDistSq = perpDistSqToAxis(blockCenter, entityPos, axis, axisLenSq);
        Vec3 toPoint = blockCenter.subtract(entityPos);

        return perpDistSq <= effectiveRadiusSq(toPoint, axis, axisLenSq, radius);
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
        return block instanceof VaultBlock
                || block instanceof SpawnerBlock
                || block instanceof TrialSpawnerBlock
                || block instanceof FaceAttachedHorizontalDirectionalBlock
                || block instanceof LadderBlock
                || block instanceof DoorBlock;
    }

    @Override
    public int frequency() {
        return 1;
    }

    @Override
    public void resetCulledBlocks() {
    }

}
