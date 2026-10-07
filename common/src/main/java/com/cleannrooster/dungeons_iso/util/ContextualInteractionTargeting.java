package com.cleannrooster.dungeons_iso.util;

import com.cleannrooster.dungeons_iso.mod.Mod;
import com.cleannrooster.dungeons_iso.util.ContextualTargeting.TargetingInputMode;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.enums.ChestType;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.state.property.Properties;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Contextual interactable-block target acquisition for Dungeons Perspective.
 *
 * <p>Locates nearby usable blocks (chests, doors, buttons, crafting tables, furnaces, bells, ...),
 * scores them against an acquisition direction (cursor in pointer mode, movement input in
 * movement mode), retains the best candidate briefly, and exposes it as {@link Mod#targetedInteractable}.
 * A dedicated key ({@code ClientInit.interact}) then performs a normal vanilla
 * {@code interactionManager.interactBlock} on it — this class never triggers use automatically and
 * never touches {@link Mod#crosshairTarget} / {@link Mod#targeted}.
 *
 * <p>Block eligibility comes entirely from {@link Mod#isInteractable(BlockPos)} (block entities and
 * the known usable-block list); it is never duplicated here.
 */
public final class ContextualInteractionTargeting {

    private ContextualInteractionTargeting() {
    }

    // Pointer-mode scoring: angular 0.45, distance 0.35, pointer proximity 0.20.
    private static final double POINTER_ANGULAR_WEIGHT = 0.25;
    private static final double POINTER_DISTANCE_WEIGHT = 0.60;
    private static final double POINTER_PROXIMITY_WEIGHT = 0.15;

    // Movement-mode scoring: pointer proximity omitted, weight redistributed.
    private static final double MOVEMENT_ANGULAR_WEIGHT = 0.25;
    private static final double MOVEMENT_DISTANCE_WEIGHT = 0.75;

    private static final double INTERACTABLE_CONE_DEG = 100.0;
    private static final double RETAINED_INTERACTABLE_BONUS = 0.08;
    private static final double INTERACTABLE_SWITCH_THRESHOLD = 0.05;
    private static final int INTERACTABLE_SWITCH_CONFIRM_TICKS = 2;
    private static final double MINIMUM_INTERACTABLE_SCORE = 0.20;
    // Additive score for blocks on InteractionTargeting.CONTEXTUAL_INTERACTABLE_PRIORITY (levers,
    // buttons, wooden doors/trapdoors, fence gates, bells, ...). Large relative to the retained
    // bonus / switch threshold so priority blocks win at comparable or closer range; distance still
    // dominates (weight 0.60, squared), so a far priority block can't override one at the cursor.
    private static final double PRIORITY_INTERACTABLE_BONUS = 0.25;

    // Scan cadence: don't rescan the box every frame.
    private static final int SCAN_INTERVAL_TICKS = 2;
    private static long lastScanTick = Long.MIN_VALUE;
    private static List<BlockPos> cachedCandidates = new ArrayList<>();

    /**
     * Update the retained interactable target for this tick.
     *
     * @param player               the local player
     * @param acquisitionDirection normalized horizontal aim direction, or {@code null}/near-zero
     *                             when there is no directional input this tick
     * @param previousTarget       the current {@link Mod#targetedInteractable} (may be null/invalid)
     * @param inputMode            pointer or movement targeting
     * @return the selected interactable block position, or {@code null}
     */
    public static BlockPos updateTarget(
            ClientPlayerEntity player,
            Vec3d acquisitionDirection,
            BlockPos previousTarget,
            TargetingInputMode inputMode
    ) {
        if (player == null || player.getWorld() == null) {
            reset();
            return null;
        }

        World world = player.getWorld();
        double range = player.getBlockInteractionRange();
        Vec3d center_player = player.getBoundingBox().getCenter().add(player.getRotationVector().multiply(player.getWidth()*0.5F));

        Vec3d scoreDir = acquisitionDirection;
        if (scoreDir != null) {
            scoreDir = scoreDir.multiply(1.0, 0.0, 1.0);

            if (scoreDir.lengthSquared() < 1.0e-6) {
                scoreDir = null;
            } else {
                scoreDir = scoreDir.normalize();
            }
        }

        List<BlockPos> candidates = scan(player, world, range);
        if (candidates.isEmpty()) {
            reset();
            return null;
        }

        BlockPos current =
                previousTarget != null && candidates.contains(previousTarget)
                        ? previousTarget
                        : null;

        boolean neutral = scoreDir == null;

        if (neutral) {
            if (current != null) {
                resetPending();
                return current;
            }

            Vec3d facing = player.getRotationVec(1.0F)
                    .multiply(1.0, 0.0, 1.0);

            if (facing.lengthSquared() < 1.0e-6) {
                return nearest(world, candidates, center_player);
            }

            scoreDir = facing.normalize();
        }

        boolean movement =
                inputMode == TargetingInputMode.MOVEMENT;

        double angularWeight =
                movement
                        ? MOVEMENT_ANGULAR_WEIGHT
                        : POINTER_ANGULAR_WEIGHT;

        double distanceWeight =
                movement
                        ? MOVEMENT_DISTANCE_WEIGHT
                        : POINTER_DISTANCE_WEIGHT;

        double cosHalfCone =
                Math.cos(Math.toRadians(INTERACTABLE_CONE_DEG / 2.0));

        Vec3d pointerPos =
                !movement && Mod.crosshairTarget != null
                        ? Mod.crosshairTarget.getPos()
                        : null;

        double nearestCandidateDistance = candidates.stream()
                .mapToDouble(pos ->
                        nearestPointDistance(world, pos, center_player))
                .min()
                .orElse(range);

        double prevScore = Double.NEGATIVE_INFINITY;
        BlockPos bestChallenger = null;
        double bestChallengerScore = Double.NEGATIVE_INFINITY;

        for (BlockPos pos : candidates) {
            Vec3d center = Vec3d.ofCenter(pos);

            Vec3d toBlock = center
                    .subtract(center_player)
                    .multiply(1.0, 0.0, 1.0);

            if (toBlock.lengthSquared() < 1.0e-6) {
                continue;
            }

            double angularDot =
                    scoreDir.dotProduct(toBlock.normalize());

            if (angularDot < cosHalfCone) {
                continue;
            }

            double angularScore = MathHelper.clamp(
                    (angularDot - cosHalfCone)
                            / (1.0 - cosHalfCone),
                    0.0,
                    1.0
            );

            double distance =
                    nearestPointDistance(world, pos, center_player);

            double linearDistanceScore = MathHelper.clamp(
                    1.0 - distance / range,
                    0.0,
                    1.0
            );

            double distanceScore =
                    linearDistanceScore * linearDistanceScore;

            double base =
                    angularScore * angularWeight
                            + distanceScore * distanceWeight;

            double excessDistance = Math.max(
                    0.0,
                    distance - nearestCandidateDistance - 0.75
            );

            base -= excessDistance * 0.12;

            if (pointerPos != null) {
                double pointerDistance = new Vec3d(
                        center.x - pointerPos.x,
                        0.0,
                        center.z - pointerPos.z
                ).length();

                double proximity = MathHelper.clamp(
                        1.0 - pointerDistance / range,
                        0.0,
                        1.0
                );

                base +=
                        proximity
                                * POINTER_PROXIMITY_WEIGHT;
            }

            if (InteractionTargeting.isHighPriority(world, pos)) {
                base += PRIORITY_INTERACTABLE_BONUS;
            }

            if (pos.equals(current)) {
                prevScore = base;

                boolean retainDirectionally =
                        angularDot >= Math.cos(Math.toRadians(65.0));

                if (retainDirectionally) {
                    prevScore += RETAINED_INTERACTABLE_BONUS;
                }
            } else if (base > bestChallengerScore) {
                bestChallengerScore = base;
                bestChallenger = pos;
            }
        }

        boolean hasPrevious =
                prevScore > Double.NEGATIVE_INFINITY;

        boolean hasChallenger =
                bestChallenger != null;

        if (hasPrevious) {
            boolean challengerWins =
                    hasChallenger
                            && bestChallengerScore
                            >= prevScore + INTERACTABLE_SWITCH_THRESHOLD
                            && bestChallengerScore
                            >= MINIMUM_INTERACTABLE_SCORE;

            if (challengerWins) {
                if (bestChallenger.equals(Mod.pendingInteractable)) {
                    Mod.pendingInteractableTicks++;
                } else {
                    Mod.pendingInteractable = bestChallenger;
                    Mod.pendingInteractableTicks = 1;
                }

                if (Mod.pendingInteractableTicks
                        >= INTERACTABLE_SWITCH_CONFIRM_TICKS) {
                    BlockPos selected = bestChallenger;
                    resetPending();
                    return selected;
                }
            } else {
                resetPending();
            }

            return current;
        }

        resetPending();

        if (hasChallenger
                && bestChallengerScore >= MINIMUM_INTERACTABLE_SCORE) {
            return bestChallenger;
        }

        return nearest(world, candidates, center_player);
    }

    // --- Scanning --------------------------------------------------------------------------------

    private static List<BlockPos> scan(ClientPlayerEntity player, World world, double range) {
        long now = world.getTime();
        // Reuse the cached scan for a couple of ticks. Guard against the Long.MIN_VALUE sentinel
        // (first run) and world/time changes going backwards, both of which would otherwise make
        // the subtraction wrap and permanently skip scanning.
        boolean fresh = lastScanTick != Long.MIN_VALUE
                && now >= lastScanTick
                && (now - lastScanTick) < SCAN_INTERVAL_TICKS;
        if (fresh) {
            return cachedCandidates;
        }
        lastScanTick = now;

        BlockPos min = BlockPos.ofFloored(player.getX() - range, player.getY() - 2, player.getZ() - range);
        BlockPos max = BlockPos.ofFloored(player.getX() + range, player.getY() + 2, player.getZ() + range);

        List<BlockPos> result = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        for (BlockPos pos : BlockPos.iterate(min, max)) {
            if (!Mod.isInteractable(pos) || InteractionTargeting.isBlacklisted(world, pos)) {
                continue;
            }
            BlockPos normalized = normalize(world, pos).toImmutable();
            if (!seen.add(normalized)) {
                continue;
            }
            if (hasUsableVisibleFace(player, world, normalized)) {
                result.add(normalized);
            }
        }
        cachedCandidates = result;
        return result;
    }

    /**
     * Normalize paired/double blocks to a single logical position so the two halves never alternate.
     * Doors (and other {@code DOUBLE_BLOCK_HALF} blocks) collapse to their lower half; double chests
     * collapse to a deterministic half. Either resulting position yields the correct interaction.
     */
    private static BlockPos normalize(World world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (state.contains(Properties.DOUBLE_BLOCK_HALF)) {
            return state.get(Properties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER ? pos.down() : pos;
        }
        if (state.getBlock() instanceof ChestBlock && state.contains(ChestBlock.CHEST_TYPE)
                && state.contains(ChestBlock.FACING)) {
            ChestType type = state.get(ChestBlock.CHEST_TYPE);
            if (type != ChestType.SINGLE) {
                Direction facing = state.get(ChestBlock.FACING);
                Direction toNeighbor = type == ChestType.LEFT
                        ? facing.rotateYClockwise() : facing.rotateYCounterclockwise();
                BlockPos neighbor = pos.offset(toNeighbor);
                // Deterministic canonical half so both sides map to the same position.
                return isBefore(pos, neighbor) ? pos.toImmutable() : neighbor;
            }
        }
        return pos;
    }

    private static boolean isBefore(BlockPos a, BlockPos b) {
        if (a.getX() != b.getX()) return a.getX() < b.getX();
        if (a.getZ() != b.getZ()) return a.getZ() < b.getZ();
        return a.getY() <= b.getY();
    }

    /**
     * True when the block has a face reachable in a straight line from the eye that is not hidden
     * behind another solid block (line of sight to the block or one of its interaction faces).
     */
    private static boolean hasUsableVisibleFace(ClientPlayerEntity player, World world, BlockPos pos) {
        Vec3d eye = player.getEyePos();
        for (Vec3d point : facePoints(pos)) {
            BlockHitResult los = world.raycast(new RaycastContext(
                    eye, point, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, player));
            if (los.getType() == HitResult.Type.MISS || los.getBlockPos().equals(pos)) {
                return true;
            }
        }
        return false;
    }

    // --- Interaction point -----------------------------------------------------------------------

    /**
     * Resolve a valid {@link BlockHitResult} for a target block by raycasting from the eye toward
     * several candidate points (center + face centers) and choosing the nearest visible face,
     * preserving the actual hit side. Falls back to the face nearest the player when none are
     * directly visible.
     */
    public static BlockHitResult resolveHit(ClientPlayerEntity player, BlockPos pos) {
        World world = player.getWorld();
        Vec3d eye = player.getEyePos();
        BlockHitResult best = null;
        double bestDist = Double.MAX_VALUE;
        for (Vec3d point : facePoints(pos)) {
            BlockHitResult r = world.raycast(new RaycastContext(
                    eye, point, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, player));
            if (r.getType() == HitResult.Type.BLOCK && r.getBlockPos().equals(pos)) {
                double d = eye.squaredDistanceTo(r.getPos());
                if (d < bestDist) {
                    bestDist = d;
                    best = r;
                }
            }
        }
        if (best != null) {
            return best;
        }
        // Fallback: click the face pointing most toward the player.
        Vec3d center = Vec3d.ofCenter(pos);
        Direction side = Direction.getFacing(eye.x - center.x, eye.y - center.y, eye.z - center.z);
        Vec3d hit = center.add(side.getOffsetX() * 0.5, side.getOffsetY() * 0.5, side.getOffsetZ() * 0.5);
        return new BlockHitResult(hit, side, pos, false);
    }

    private static List<Vec3d> facePoints(BlockPos pos) {
        Vec3d center = Vec3d.ofCenter(pos);
        List<Vec3d> points = new ArrayList<>(7);
        points.add(center);
        for (Direction dir : Direction.values()) {
            points.add(center.add(dir.getOffsetX() * 0.5, dir.getOffsetY() * 0.5, dir.getOffsetZ() * 0.5));
        }
        return points;
    }

    // --- Helpers ---------------------------------------------------------------------------------

    /** Distance from {@code eye} to the nearest point on the block's outline shape (not its center). */
    private static double nearestPointDistance(World world, BlockPos pos, Vec3d eye) {
        VoxelShape shape = world.getBlockState(pos).getOutlineShape(world, pos);
        double minX, minY, minZ, maxX, maxY, maxZ;
        if (shape.isEmpty()) {
            minX = minY = minZ = 0.0;
            maxX = maxY = maxZ = 1.0;
        } else {
            var box = shape.getBoundingBox();
            minX = box.minX; minY = box.minY; minZ = box.minZ;
            maxX = box.maxX; maxY = box.maxY; maxZ = box.maxZ;
        }
        double nx = MathHelper.clamp(eye.x, pos.getX() + minX, pos.getX() + maxX);
        double ny = MathHelper.clamp(eye.y, pos.getY() + minY, pos.getY() + maxY);
        double nz = MathHelper.clamp(eye.z, pos.getZ() + minZ, pos.getZ() + maxZ);
        return eye.distanceTo(new Vec3d(nx, ny, nz));
    }
    private static BlockPos nearest(
            World world,
            List<BlockPos> candidates,
            Vec3d eye
    ) {
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;

        for (BlockPos pos : candidates) {
            double distance =
                    nearestPointDistance(world, pos, eye);

            if (distance < bestDistance) {
                bestDistance = distance;
                best = pos;
            }
        }

        return best;
    }

    private static void resetPending() {
        Mod.pendingInteractable = null;
        Mod.pendingInteractableTicks = 0;
    }

    private static void reset() {
        resetPending();
        cachedCandidates = new ArrayList<>();
        lastScanTick = Long.MIN_VALUE;
    }
}
