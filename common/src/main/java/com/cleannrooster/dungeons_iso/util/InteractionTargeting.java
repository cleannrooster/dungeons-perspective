package com.cleannrooster.dungeons_iso.util;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import static net.minecraft.tags.BlockTags.*;

public final class InteractionTargeting {
    public static final TagKey<Block> CONTEXTUAL_INTERACTABLES =
            TagKey.create(
                    Registries.BLOCK,
                    Identifier.fromNamespaceAndPath("dungeons_iso", "contextual_interactables")
            );

    public static final TagKey<Block> CONTEXTUAL_INTERACTABLE_BLACKLIST =
            TagKey.create(
                    Registries.BLOCK,
                    Identifier.fromNamespaceAndPath("dungeons_iso", "contextual_interactable_blacklist")
            );

    // Blocks that should be targeted with high priority (levers, buttons, wooden doors/trapdoors,
    // fence gates, bells, ...). Membership grants a scoring bonus in the contextual selector.
    public static final TagKey<Block> CONTEXTUAL_INTERACTABLE_PRIORITY =
            TagKey.create(
                    Registries.BLOCK,
                    Identifier.fromNamespaceAndPath("dungeons_iso", "contextual_interactable_priority")
            );

    private InteractionTargeting() {
    }

    public static boolean isInteractable(
            Level world,
            BlockPos pos
    ) {
        if (world == null || pos == null || !world.isInWorldBounds(pos)) {
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
        if (state.is(CONTEXTUAL_INTERACTABLES) || state.is(CONTEXTUAL_INTERACTABLE_PRIORITY)) {
            return isCurrentlyUsable(state);
        }

        // Automatically catches most containers and workstations, including
        // many modded menu-opening blocks.
        if (state.getMenuProvider(world, pos) != null) {
            return true;
        }

        // Stable semantic vanilla groups.
        return state.is(BlockTags.BUTTONS)
                || state.is(BlockTags.DOORS)
                || state.is(BlockTags.TRAPDOORS)
                || state.is(BlockTags.FENCE_GATES)
                || state.is(BlockTags.CAMPFIRES)
                || state.is(BlockTags.CAULDRONS)
                || state.is(BlockTags.ALL_SIGNS)
                || state.getBlock() instanceof LeverBlock
                || state.getBlock() instanceof BellBlock;
    }

    public static boolean isInteractableInRange(
            LocalPlayer player,
            BlockPos pos
    ) {
        if (player == null || pos == null) {
            return false;
        }

        Level world = player.level();

        if (!isInteractable(world, pos)) {
            return false;
        }

        double range = player.blockInteractionRange();

        return squaredDistanceToBlock(player.getEyePosition(), pos)
                <= range * range;
    }

    private static double squaredDistanceToBlock(
            Vec3 point,
            BlockPos pos
    ) {
        double nearestX = Mth.clamp(
                point.x,
                pos.getX(),
                pos.getX() + 1.0
        );

        double nearestY = Mth.clamp(
                point.y,
                pos.getY(),
                pos.getY() + 1.0
        );

        double nearestZ = Mth.clamp(
                point.z,
                pos.getZ(),
                pos.getZ() + 1.0
        );

        return point.distanceToSqr(
                nearestX,
                nearestY,
                nearestZ
        );
    }

    private static boolean isCurrentlyUsable(BlockState state) {


        return true;
    }

    /** True if the block is on the high-priority contextual-interactable tag. */
    public static boolean isHighPriority(Level world, BlockPos pos) {
        return world != null && pos != null
                && (world.getBlockState(pos).is(CONTEXTUAL_INTERACTABLE_PRIORITY) ||
                world.getBlockState(pos).is(DOORS) ||
                world.getBlockState(pos).is(BUTTONS) ||
                world.getBlockState(pos).is(BEDS) ||
                world.getBlockState(pos).is(FENCE_GATES) ||
                world.getBlockState(pos).getBlock() instanceof LeverBlock ||
                world.getBlockState(pos).getBlock() instanceof BellBlock);
    }

    /** True if the block is blacklisted from contextual interaction (e.g. iron doors). */
    public static boolean isBlacklisted(Level world, BlockPos pos) {
        return world != null && pos != null
                && (world.getBlockState(pos).is(CONTEXTUAL_INTERACTABLE_BLACKLIST)
                || world.getBlockState(pos).getBlock().equals(Blocks.IRON_DOOR)
                || world.getBlockState(pos).getBlock().equals(Blocks.IRON_TRAPDOOR));
    }
}


