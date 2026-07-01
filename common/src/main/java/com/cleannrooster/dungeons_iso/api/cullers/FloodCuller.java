package com.cleannrooster.dungeons_iso.api.cullers;

import com.cleannrooster.dungeons_iso.api.BlockCuller;
import com.cleannrooster.dungeons_iso.config.Config;
import com.cleannrooster.dungeons_iso.mod.Mod;
import net.minecraft.client.Camera;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.phys.shapes.CollisionContext;
import java.util.*;

public class FloodCuller implements BlockCuller {

    // Per-column ceiling height: packed XZ -> highest air block Y visited in the flood.
    // Blocks at Y > ceiling[xz] (and xz in the map) are culled.
    private final HashMap<Long, Integer> floodXZCeilingMap = new HashMap<>();
    private int floodY;

    // Reusable BFS structures — cleared each call, never reallocated.
    private final HashSet<Long> visited = new HashSet<>();
    // Manual primitive long stack — avoids one long[]{} allocation per node.
    private long[] longStack = new long[1024];
    private int stackTop = 0;

    // Vertical range constants for 3D room exploration.
    // Allow upward exploration up to this many blocks above floodY (covers tall ceilings).
    private static final int MAX_VERTICAL_UP   = 24;
    // Allow downward exploration for sunken floors and stairs.
    private static final int MAX_VERTICAL_DOWN = 8;

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
        return cameraEntity != null && Mod.shouldReload
                && cameraEntity.level().getBlockState(blockPos)
                .getVisualShape(cameraEntity.level(), blockPos, CollisionContext.of(cameraEntity))
                .isEmpty();
    }

    @Override
    public boolean shouldIgnoreBlockPick(BlockPos blockPos, Camera camera, Entity cameraEntity) {
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
    public List<BlockPos> getCulledBlocks(BlockPos blockPos, Camera camera, Entity cameraEntity) {
        BlockPos startPos = cameraEntity.blockPosition().above();
        floodY = startPos.getY();
        int minY = floodY - MAX_VERTICAL_DOWN;
        int maxY = floodY + MAX_VERTICAL_UP;

        // Reuse collections — clear is O(n) but skips all GC pressure of reallocation.
        visited.clear();
        floodXZCeilingMap.clear();
        stackTop = 0;

        double cullAngleRatio = Config.GSON.instance().cullAngle / 30.0;
        double maxDist    = 16.0 * cullAngleRatio;
        double zoomDist   = 0.1 * (Math.min(10, Math.min(
                cameraEntity.level().getGameTime() - Mod.startTime + 2,
                10 - Mod.endTime)))
                * Mod.getZoom() * Mod.zoomMetric * cullAngleRatio;
        double maxDistSq  = maxDist  * maxDist;
        double zoomDistSq = zoomDist * zoomDist;

        int entityX = cameraEntity.blockPosition().getX();
        int entityZ = cameraEntity.blockPosition().getZ();

        int startX = startPos.getX();
        int startY = startPos.getY();
        int startZ = startPos.getZ();

        long startPacked = BlockPos.asLong(startX, startY, startZ);
        pushStack(startPacked);
        visited.add(startPacked);

        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        // 6-directional offsets: ±X, ±Z (horizontal), ±Y (vertical)
        // Stored as flat int pairs: {dx, dy, dz} — use 3-element int arrays.
        // To avoid allocating these every call, define them as a constant.
        while (stackTop > 0) {
            long packed = longStack[--stackTop];
            int x = BlockPos.getX(packed);
            int y = BlockPos.getY(packed);
            int z = BlockPos.getZ(packed);

            // Update ceiling map: track highest air-block Y seen at this XZ column.
            long xz = packXZ(x, z);
            Integer prev = floodXZCeilingMap.get(xz);
            if (prev == null || y > prev) {
                floodXZCeilingMap.put(xz, y);
            }

            double dx = x - entityX;
            double dz = z - entityZ;
            double distSq = dx * dx + dz * dz;

            if (distSq <= maxDistSq && distSq <= zoomDistSq) {
                // Horizontal neighbors
                tryNeighbor(x + 1, y, z, camera, cameraEntity, mutable);
                tryNeighbor(x - 1, y, z, camera, cameraEntity, mutable);
                tryNeighbor(x, y, z + 1, camera, cameraEntity, mutable);
                tryNeighbor(x, y, z - 1, camera, cameraEntity, mutable);
                // Vertical neighbors — bounded to prevent sky/void escapes
                if (y + 1 <= maxY) tryNeighbor(x, y + 1, z, camera, cameraEntity, mutable);
                if (y - 1 >= minY) tryNeighbor(x, y - 1, z, camera, cameraEntity, mutable);
            }
        }

        // getCulledBlocks() return value is stored in SodiumCompat.stream but never consumed.
        // Return empty list to avoid the old per-position toImmutable() allocations.
        return List.of();
    }

    private void tryNeighbor(int nx, int ny, int nz, Camera camera, Entity cameraEntity, BlockPos.MutableBlockPos mutable) {
        long neighborPacked = BlockPos.asLong(nx, ny, nz);
        if (!visited.contains(neighborPacked)) {
            visited.add(neighborPacked);
            mutable.set(nx, ny, nz);
            if (shouldCull(mutable, camera, cameraEntity)) {
                pushStack(neighborPacked);
            }
        }
    }

    private void pushStack(long value) {
        if (stackTop == longStack.length) {
            longStack = Arrays.copyOf(longStack, longStack.length * 2);
        }
        longStack[stackTop++] = value;
    }

    @Override
    public void resetCulledBlocks() {
    }

    /**
     * Returns true if the given block position is above the ceiling of the connected
     * air-space the player occupies at its XZ column. Such blocks should be culled.
     */
    public boolean isAboveFlood(BlockPos blockPos) {
        long xz = packXZ(blockPos.getX(), blockPos.getZ());
        Integer ceiling = floodXZCeilingMap.get(xz);
        return ceiling != null && blockPos.getY() > ceiling;
    }

    private static long packXZ(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }
}
