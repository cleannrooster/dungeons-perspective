package com.cleannrooster.dungeons_iso.util;

import net.minecraft.block.*;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import static net.minecraft.registry.tag.BlockTags.*;

public final class InteractionTargeting {
    public static final TagKey<Block> CONTEXTUAL_INTERACTABLES =
            TagKey.of(
                    RegistryKeys.BLOCK,
                    Identifier.of("dungeons_iso", "contextual_interactables")
            );

    public static final TagKey<Block> CONTEXTUAL_INTERACTABLE_BLACKLIST =
            TagKey.of(
                    RegistryKeys.BLOCK,
                    Identifier.of("dungeons_iso", "contextual_interactable_blacklist")
            );

    // Blocks that should be targeted with high priority (levers, buttons, wooden doors/trapdoors,
    // fence gates, bells, ...). Membership grants a scoring bonus in the contextual selector.
    public static final TagKey<Block> CONTEXTUAL_INTERACTABLE_PRIORITY =
            TagKey.of(
                    RegistryKeys.BLOCK,
                    Identifier.of("dungeons_iso", "contextual_interactable_priority")
            );

    private InteractionTargeting() {
    }

    public static boolean isInteractable(
            World world,
            BlockPos pos
    ) {
        if (world == null || pos == null || !world.isInBuildLimit(pos)) {
            return false;
        }

        BlockState state = world.getBlockState(pos);

        if (state.isAir()) {
            return false;
        }

        // Use the hardcoded blacklist (vanilla registry checks), not just the custom tag, so it
        // still rejects e.g. iron doors on remote servers that don't have the mod's data pack.
        if (isBlacklisted(world, pos)) {
            return false;
        }

        // Explicit opt-in (allowlist + high-priority list). These should contain special-use blocks
        // that do not necessarily open a screen, including modded blocks.
        if (state.isIn(CONTEXTUAL_INTERACTABLES) || state.isIn(CONTEXTUAL_INTERACTABLE_PRIORITY)) {
            return isCurrentlyUsable(state);
        }

        // Automatically catches most containers and workstations, including
        // many modded menu-opening blocks.
        if (state.createScreenHandlerFactory(world, pos) != null) {
            return true;
        }

        // Stable semantic vanilla groups.
        return state.isIn(BlockTags.BUTTONS)
                || state.isIn(BlockTags.DOORS)
                || state.isIn(BlockTags.TRAPDOORS)
                || state.isIn(BlockTags.FENCE_GATES)
                || state.isIn(BlockTags.CAMPFIRES)
                || state.isIn(BlockTags.CAULDRONS)
                || state.isIn(BlockTags.ALL_SIGNS)
                || state.getBlock() instanceof LeverBlock
                || state.getBlock() instanceof BellBlock;
    }

    public static boolean isInteractableInRange(
            ClientPlayerEntity player,
            BlockPos pos
    ) {
        if (player == null || pos == null) {
            return false;
        }

        World world = player.getWorld();

        if (!isInteractable(world, pos)) {
            return false;
        }

        double range = player.getBlockInteractionRange();

        return squaredDistanceToBlock(player.getEyePos(), pos)
                <= range * range;
    }

    private static double squaredDistanceToBlock(
            Vec3d point,
            BlockPos pos
    ) {
        double nearestX = MathHelper.clamp(
                point.x,
                pos.getX(),
                pos.getX() + 1.0
        );

        double nearestY = MathHelper.clamp(
                point.y,
                pos.getY(),
                pos.getY() + 1.0
        );

        double nearestZ = MathHelper.clamp(
                point.z,
                pos.getZ(),
                pos.getZ() + 1.0
        );

        return point.squaredDistanceTo(
                nearestX,
                nearestY,
                nearestZ
        );
    }

    private static boolean isCurrentlyUsable(BlockState state) {


        return true;
    }

    /** True if the block is on the high-priority contextual-interactable tag. */
    public static boolean isHighPriority(World world, BlockPos pos) {
        return world != null && pos != null
                && (world.getBlockState(pos).isIn(CONTEXTUAL_INTERACTABLE_PRIORITY) ||
                world.getBlockState(pos).isIn(DOORS) ||
                world.getBlockState(pos).isIn(BUTTONS) ||
                world.getBlockState(pos).isIn(BEDS) ||
                world.getBlockState(pos).isIn(FENCE_GATES) ||
                world.getBlockState(pos).getBlock() instanceof LeverBlock ||
                world.getBlockState(pos).getBlock() instanceof BellBlock);
    }

    /** True if the block is blacklisted from contextual interaction (e.g. iron doors). */
    public static boolean isBlacklisted(World world, BlockPos pos) {
        return world != null && pos != null
                && (world.getBlockState(pos).isIn(CONTEXTUAL_INTERACTABLE_BLACKLIST)
                || world.getBlockState(pos).getBlock().equals(Blocks.IRON_DOOR)
                || world.getBlockState(pos).getBlock().equals(Blocks.IRON_TRAPDOOR));
    }
}