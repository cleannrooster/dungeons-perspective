package com.cleannrooster.dungeons_iso.util;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.List;

/**
 * Contextual combat target acquisition for Dungeons Perspective.
 *
 * <p>This picks a single "combat facing" entity ({@code Mod.targeted}) based on an acquisition
 * <em>direction</em> in the world. It does not deal damage, choose Better Combat hit victims,
 * modify reach, or replace Spell Engine / projectile targeting — it only establishes which entity
 * the player character should face before an attack, item use, or spell cast.
 *
 * <p>Two input modes feed the same scorer:
 * <ul>
 *   <li>{@link TargetingInputMode#POINTER} — the direction comes from the top-down cursor's world
 *       position (mouse aiming).</li>
 *   <li>{@link TargetingInputMode#MOVEMENT} — the direction comes from the player's movement input
 *       (controller / one-handed / keyboard-only play). Movement <em>chooses</em> a target when
 *       needed, then combat focus persists independently of movement until the player deliberately
 *       redirects it, so kiting (retreating/strafing while attacking) keeps the same target.</li>
 * </ul>
 * Both modes share candidate filtering, angular + distance scoring, range limits, soft-target
 * retention and line-of-sight rules; only the retention strength, cones and switch behaviour differ.
 */
public final class ContextualTargeting {

    private ContextualTargeting() {}

    public enum TargetingMode {
        MELEE,
        RANGED,
        /** Out-of-combat: acquire interactable entities (villagers, traders) rather than enemies. */
        INTERACT,
        NONE
    }

    public enum TargetingInputMode {
        POINTER,
        MOVEMENT
    }

    // Acquisition ranges. Melee is derived from the player's interaction range so it scales with
    // attributes/mods; ranged is a fixed comfortable screen-space distance.
    private static final double RANGED_RANGE = 24.0;
    private static final double MELEE_RANGE_BONUS = 2.0;

    // Pointer-mode acquisition cones (full angle, degrees).
    private static final double MELEE_CONE_DEG = 100.0;
    private static final double RANGED_CONE_DEG = 70.0;

    // Scoring weights per combat mode.
    private static final double MELEE_ANGULAR_WEIGHT = 0.60;
    private static final double MELEE_DISTANCE_WEIGHT = 0.40;
    private static final double RANGED_ANGULAR_WEIGHT = 0.85;
    private static final double RANGED_DISTANCE_WEIGHT = 0.15;

    // Out-of-combat interaction targeting (villagers, traders): short reach, melee-like cone/weights.
    private static final double INTERACT_RANGE_BONUS = 1.0;
    private static final double INTERACT_CONE_DEG = 110.0;
    private static final double INTERACT_ANGULAR_WEIGHT = 0.55;
    private static final double INTERACT_DISTANCE_WEIGHT = 0.45;

    // Pointer-mode soft-target retention.
    private static final double RETAINED_TARGET_BONUS = 0.20;
    private static final double SWITCH_THRESHOLD = 0.12;
    private static final double MINIMUM_SCORE = 0.30;

    // Movement-mode retention is stronger, and target switching is gated by a wider acquire cone,
    // a narrower switch cone, a larger score margin and a confirmation window so that ordinary
    // strafing / retreating never steals the current target.
    private static final double MOVEMENT_SWITCH_THRESHOLD = 0.22;
    private static final double MOVEMENT_ACQUIRE_CONE_DEG = 110.0;
    private static final double MOVEMENT_SWITCH_CONE_DEG = 55.0;
    private static final int MOVEMENT_SWITCH_CONFIRM_TICKS = 4;

    // Line-of-sight grace: a retained target may briefly break LOS (walking behind cover) without
    // being dropped.
    private static final int LOS_GRACE_TICKS = 10;

    // --- Client-singleton state (movement mode) ---------------------------------------------------
    private static Entity pendingMovementTarget;
    private static int pendingMovementTargetTicks;
    private static Entity graceTarget;
    private static int losGraceTicks;

    /**
     * Choose the contextual combat target for this tick.
     *
     * @param player               the local player
     * @param acquisitionDirection normalized world-space horizontal direction expressing aim
     *                             intent, or {@code null}/near-zero for "no direction this tick"
     * @param previousTarget       the current soft target (may be null or now-invalid)
     * @param combatMode           the active combat mode (melee / ranged); {@link TargetingMode#NONE}
     *                             yields no target
     * @param inputMode            whether the direction came from the pointer or from movement
     * @return the acquired target, or {@code null} when nothing qualifies
     */
    public static Entity updateTarget(ClientPlayerEntity player,
                                      Vec3d acquisitionDirection,
                                      Entity previousTarget,
                                      TargetingMode combatMode,
                                      TargetingInputMode inputMode) {
        if (player == null || player.getWorld() == null || combatMode == TargetingMode.NONE) {
            return null;
        }

        boolean interact = combatMode == TargetingMode.INTERACT;
        double range;
        double angularWeight;
        double distanceWeight;
        double coneDeg;
        switch (combatMode) {
            case MELEE -> {
                range = player.getEntityInteractionRange() + MELEE_RANGE_BONUS;
                angularWeight = MELEE_ANGULAR_WEIGHT;
                distanceWeight = MELEE_DISTANCE_WEIGHT;
                coneDeg = MELEE_CONE_DEG;
            }
            case INTERACT -> {
                range = player.getEntityInteractionRange() + INTERACT_RANGE_BONUS;
                angularWeight = INTERACT_ANGULAR_WEIGHT;
                distanceWeight = INTERACT_DISTANCE_WEIGHT;
                coneDeg = INTERACT_CONE_DEG;
            }
            default -> { // RANGED
                range = RANGED_RANGE;
                angularWeight = RANGED_ANGULAR_WEIGHT;
                distanceWeight = RANGED_DISTANCE_WEIGHT;
                coneDeg = RANGED_CONE_DEG;
            }
        }

        Vec3d eye = player.getEyePos();

        if (inputMode == TargetingInputMode.MOVEMENT) {
            return updateMovementTarget(player, acquisitionDirection, previousTarget,
                    range, angularWeight, distanceWeight, eye, interact);
        }
        return updatePointerTarget(player, acquisitionDirection, previousTarget,
                range, angularWeight, distanceWeight, eye, Math.cos(Math.toRadians(coneDeg / 2.0)), interact);
    }

    // --- Pointer mode ----------------------------------------------------------------------------

    private static Entity updatePointerTarget(ClientPlayerEntity player, Vec3d aimDir, Entity previousTarget,
                                              double range, double angularWeight, double distanceWeight,
                                              Vec3d eye, double cosHalfCone, boolean interact) {
        if (aimDir == null) {
            // No usable direction: keep the previous target if it is still valid.
            return isCandidate(player, previousTarget, range, interact) ? previousTarget : null;
        }

        double prevScore = Double.NEGATIVE_INFINITY;
        Entity bestChallenger = null;
        double bestChallengerScore = Double.NEGATIVE_INFINITY;

        for (LivingEntity candidate : candidates(player, range, interact)) {
            double angular = angularAlignment(aimDir, eye, candidate);
            if (angular < cosHalfCone) {
                continue;
            }
            double base = baseScore(angular, eye, candidate, range, angularWeight, distanceWeight);
            if (candidate == previousTarget) {
                prevScore = base + RETAINED_TARGET_BONUS;
            } else if (base > bestChallengerScore) {
                bestChallengerScore = base;
                bestChallenger = candidate;
            }
        }

        boolean hasPrev = prevScore > Double.NEGATIVE_INFINITY;
        boolean hasChallenger = bestChallenger != null;

        if (hasPrev) {
            if (hasChallenger
                    && bestChallengerScore >= prevScore + SWITCH_THRESHOLD
                    && bestChallengerScore >= MINIMUM_SCORE) {
                return bestChallenger;
            }
            if (prevScore >= MINIMUM_SCORE) {
                return previousTarget;
            }
            return (hasChallenger && bestChallengerScore >= MINIMUM_SCORE) ? bestChallenger : null;
        }
        return (hasChallenger && bestChallengerScore >= MINIMUM_SCORE) ? bestChallenger : null;
    }

    // --- Movement mode ---------------------------------------------------------------------------

    private static Entity updateMovementTarget(ClientPlayerEntity player, Vec3d moveDir, Entity previousTarget,
                                               double range, double angularWeight, double distanceWeight,
                                               Vec3d eye, boolean interact) {
        Entity current = retainMovementTarget(player, previousTarget, range, interact);
        boolean neutral = moveDir == null || moveDir.lengthSquared() < 1.0e-6;

        if (current != null) {
            // A valid target is retained regardless of movement direction (retreating / strafing
            // keep it). Switching requires deliberate, sustained forward movement toward another
            // candidate inside the narrow switch cone.
            if (neutral) {
                resetPending();
                return current;
            }
            double cosSwitchCone = Math.cos(Math.toRadians(MOVEMENT_SWITCH_CONE_DEG / 2.0));
            Entity bestChallenger = null;
            double bestChallengerScore = Double.NEGATIVE_INFINITY;
            double currentScore = Double.NEGATIVE_INFINITY;

            for (LivingEntity candidate : candidates(player, range, interact)) {
                double angular = angularAlignment(moveDir, eye, candidate);
                double base = baseScore(angular, eye, candidate, range, angularWeight, distanceWeight);
                if (candidate == current) {
                    // Current target's score relative to the movement direction, used only for the
                    // switch margin comparison (no cone gate — it may legitimately be behind us).
                    currentScore = base;
                    continue;
                }
                if (angular < cosSwitchCone) {
                    continue;
                }
                if (base > bestChallengerScore) {
                    bestChallengerScore = base;
                    bestChallenger = candidate;
                }
            }

            if (bestChallenger == null) {
                resetPending();
                return current;
            }

            if (bestChallenger == pendingMovementTarget) {
                pendingMovementTargetTicks++;
            } else {
                pendingMovementTarget = bestChallenger;
                pendingMovementTargetTicks = 1;
            }

            if (pendingMovementTargetTicks >= MOVEMENT_SWITCH_CONFIRM_TICKS
                    && bestChallengerScore >= currentScore + MOVEMENT_SWITCH_THRESHOLD
                    && bestChallengerScore >= MINIMUM_SCORE) {
                resetPending();
                return bestChallenger;
            }
            return current;
        }

        // No valid current target. Never auto-select the nearest enemy while movement is neutral.
        resetPending();
        if (neutral) {
            return null;
        }
        double cosAcquireCone = Math.cos(Math.toRadians(MOVEMENT_ACQUIRE_CONE_DEG / 2.0));
        Entity best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (LivingEntity candidate : candidates(player, range, interact)) {
            double angular = angularAlignment(moveDir, eye, candidate);
            if (angular < cosAcquireCone) {
                continue;
            }
            double base = baseScore(angular, eye, candidate, range, angularWeight, distanceWeight);
            if (base > bestScore) {
                bestScore = base;
                best = candidate;
            }
        }
        return (best != null && bestScore >= MINIMUM_SCORE) ? best : null;
    }

    /**
     * Movement-mode retention: valid if alive, attackable, in range and not invisible, with a
     * short line-of-sight grace window. Deliberately does not reject on the acquisition cone —
     * moving away from the target must not drop it (kiting).
     */
    private static Entity retainMovementTarget(ClientPlayerEntity player, Entity previousTarget, double range,
                                               boolean interact) {
        if (!(previousTarget instanceof LivingEntity living)
                || !living.isAlive()
                || living == player
                || living.isRemoved()
                || (interact ? !isInteractableEntity(living) : !living.canHit())
                || living.isSpectator()
                || living.isInvisibleTo(player)
                || player.getEyePos().distanceTo(living.getPos()) > range) {
            losGraceTicks = 0;
            graceTarget = null;
            return null;
        }
        if (graceTarget != previousTarget) {
            graceTarget = previousTarget;
            losGraceTicks = 0;
        }
        if (player.canSee(living)) {
            losGraceTicks = 0;
            return previousTarget;
        }
        // Line of sight temporarily lost — keep the target through the grace period.
        losGraceTicks++;
        if (losGraceTicks > LOS_GRACE_TICKS) {
            losGraceTicks = 0;
            graceTarget = null;
            return null;
        }
        return previousTarget;
    }

    private static void resetPending() {
        pendingMovementTarget = null;
        pendingMovementTargetTicks = 0;
    }

    /** Clear all soft-targeting state. Called on explicit clear or idle timeout. */
    public static void clearSoftTarget() {
        resetPending();
        losGraceTicks = 0;
        graceTarget = null;
    }

    // --- Shared scoring / filtering --------------------------------------------------------------

    private static List<LivingEntity> candidates(ClientPlayerEntity player, double range, boolean interact) {
        return player.getWorld().getEntitiesByClass(
                LivingEntity.class,
                player.getBoundingBox().expand(range),
                candidate -> isCandidate(player, candidate, range, interact));
    }

    /** Dispatch to the combat (attackable) or interaction (interactable-entity) candidate filter. */
    private static boolean isCandidate(PlayerEntity player, Entity entity, double range, boolean interact) {
        return interact
                ? isInteractableTarget(player, entity, range)
                : isValidTarget(player, entity, range);
    }

    /** Horizontal angular alignment (dot product) of a candidate against the aim direction. */
    private static double angularAlignment(Vec3d aimDir, Vec3d eye, Entity candidate) {
        Vec3d toTarget = candidate.getPos().subtract(eye).multiply(1.0, 0.0, 1.0);
        if (toTarget.lengthSquared() < 1.0e-6) {
            return -2.0; // undefined direction — never inside any cone
        }
        return aimDir.dotProduct(toTarget.normalize());
    }

    private static double baseScore(double angularAlignment, Vec3d eye, Entity candidate,
                                    double range, double angularWeight, double distanceWeight) {
        double distance = eye.distanceTo(candidate.getPos());
        double distanceScore = 1.0 - distance / range;
        return angularAlignment * angularWeight + distanceScore * distanceWeight;
    }

    /**
     * Whether an entity may be a contextual combat candidate for the given player and range.
     * Used as the world-query predicate and to validate a retained pointer-mode target.
     */
    public static boolean isValidTarget(PlayerEntity player, Entity entity, double range) {
        if (!(entity instanceof LivingEntity living)) {
            return false;
        }
        return living.isAlive()
                && living != player
                && living.canHit()
                && !living.isSpectator()
                && !living.isInvisibleTo(player)
                && player.canSee(living)
                && player.getEyePos().distanceTo(living.getPos()) <= range;
    }

    /**
     * Whether an entity is a valid out-of-combat interaction candidate (interactable type, alive,
     * visible, in range). Mirrors {@link #isValidTarget} but swaps attackability for interactability.
     */
    public static boolean isInteractableTarget(PlayerEntity player, Entity entity, double range) {
        if (!(entity instanceof LivingEntity living)) {
            return false;
        }
        return living.isAlive()
                && living != player
                && isInteractableEntity(living)
                && !living.isSpectator()
                && !living.isInvisibleTo(player)
                && player.canSee(living)
                && player.getEyePos().distanceTo(living.getPos()) <= range;
    }

    /**
     * Entities that have a meaningful right-click interaction out of combat. Currently merchants
     * (villagers, wandering traders); extend here for other interactable NPC types.
     */
    public static boolean isInteractableEntity(Entity entity) {
        return entity instanceof MerchantEntity;
    }

    /**
     * Whether an explicit hard lock-on target is still usable. Deliberately looser than
     * {@link #isValidTarget}: a hard lock persists through occlusion and cone changes and only
     * drops when the target dies, is removed, or becomes invisible to the player.
     */
    public static boolean isValidLockOn(PlayerEntity player, Entity entity) {
        if (!(entity instanceof LivingEntity living)) {
            return false;
        }
        return living.isAlive()
                && living != player
                && !living.isRemoved()
                && !living.isInvisibleTo(player);
    }
}
