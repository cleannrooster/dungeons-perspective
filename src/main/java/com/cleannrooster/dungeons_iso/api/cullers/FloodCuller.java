package com.cleannrooster.dungeons_iso.api.cullers;

import com.cleannrooster.dungeons_iso.api.BlockCuller;
import com.cleannrooster.dungeons_iso.config.Config;
import com.cleannrooster.dungeons_iso.mod.Mod;
import net.minecraft.block.*;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;

import java.util.*;

public class FloodCuller implements BlockCuller {

    private HashSet<Long> floodXZSet = new HashSet<>();
    private int floodY;

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
        return false;
    }

    @Override
    public float blockTransparancy(BlockPos pos) {
        return 0;
    }

    public boolean shouldCull(BlockPos blockPos, Camera camera, Entity cameraEntity) {
        return cameraEntity != null && Mod.shouldReload && cameraEntity.getWorld().getBlockState(blockPos).getCameraCollisionShape(cameraEntity.getWorld(), blockPos, ShapeContext.of(cameraEntity)).isEmpty();
    }

    @Override
    public boolean shouldIgnoreBlockPick(BlockPos blockPos, Camera camera, Entity cameraEntity) {
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
    public List<BlockPos> getCulledBlocks(BlockPos blockPos, Camera camera, Entity cameraEntity) {
        BlockPos startPos = cameraEntity.getBlockPos().up();
        floodY = startPos.getY();

        HashSet<Long> visited = new HashSet<>();
        ArrayDeque<long[]> stack = new ArrayDeque<>();
        List<BlockPos> builder = new ArrayList<>();

        double cullAngleRatio = Config.GSON.instance().cullAngle / 30.0;
        double maxDist = 16.0 * cullAngleRatio;
        double zoomDist = 0.1 * (Math.min(10, Math.min(cameraEntity.getWorld().getTime() - Mod.startTime + 2, 10 - Mod.endTime))) * Mod.getZoom() * Mod.zoomMetric * cullAngleRatio;
        double maxDistSq = maxDist * maxDist;
        double zoomDistSq = zoomDist * zoomDist;

        int startX = startPos.getX();
        int startY = startPos.getY();
        int startZ = startPos.getZ();
        int entityX = cameraEntity.getBlockPos().getX();
        int entityZ = cameraEntity.getBlockPos().getZ();

        long startPacked = BlockPos.asLong(startX, startY, startZ);
        stack.push(new long[]{startPacked});
        visited.add(startPacked);

        BlockPos.Mutable mutable = new BlockPos.Mutable();

        while (!stack.isEmpty()) {
            long packed = stack.pop()[0];
            int x = BlockPos.unpackLongX(packed);
            int y = BlockPos.unpackLongY(packed);
            int z = BlockPos.unpackLongZ(packed);

            mutable.set(x, y, z);
            builder.add(mutable.toImmutable());

            double dx = x - entityX;
            double dz = z - entityZ;
            double distSq = dx * dx + dz * dz;

            if (distSq <= maxDistSq && distSq <= zoomDistSq) {
                // Check 4 neighbors
                int[][] offsets = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}};
                for (int[] off : offsets) {
                    int nx = x + off[0];
                    int nz = z + off[1];
                    long neighborPacked = BlockPos.asLong(nx, y, nz);
                    if (!visited.contains(neighborPacked)) {
                        visited.add(neighborPacked);
                        mutable.set(nx, y, nz);
                        if (this.shouldCull(mutable, camera, cameraEntity)) {
                            stack.push(new long[]{neighborPacked});
                        }
                    }
                }
            }
        }

        // Build the O(1) lookup set for isAboveFlood
        floodXZSet = new HashSet<>(visited.size());
        for (BlockPos pos : builder) {
            floodXZSet.add(packXZ(pos.getX(), pos.getZ()));
        }

        return builder;
    }

    @Override
    public void resetCulledBlocks() {
    }

    public boolean isAboveFlood(BlockPos blockPos) {
        return blockPos.getY() > floodY && floodXZSet.contains(packXZ(blockPos.getX(), blockPos.getZ()));
    }

    private static long packXZ(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }
}
