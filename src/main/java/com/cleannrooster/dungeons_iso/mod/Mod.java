package com.cleannrooster.dungeons_iso.mod;

import com.cleannrooster.dungeons_iso.config.Config;
import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.entity.Entity;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class Mod {
    public static float yaw;
    public static float lastyaw;

    public static float pitch;
    public static double x;
    public static boolean shouldReload;
    public static double flyingYAddition;

    public static double z;
    public static int cooldownIs;
    public static int cooldownWas;

    public static float zoom = 5.0F;
    public static boolean enabled = false;
    public static Perspective lastPerspective;
    public static HitResult crosshairTarget;
    public static HitResult prevCrosshairTarget;
    public static Entity lockOnTarget;
    public static boolean dirty;
    public static long dirtyTime;
    public static long startTime;
    public static long endTime;
    public static boolean using;

    public static boolean isInteractable(BlockHitResult result){
        BlockPos pos = result.getBlockPos();

        return (((MinecraftClient.getInstance().world.getBlockEntity(pos) != null ||
                 MinecraftClient.getInstance().world.getBlockState(result.getBlockPos()).streamTags().anyMatch(tag ->{
                      return tag.equals(BlockTags.ALL_SIGNS) ||  tag.equals(BlockTags.ANVIL) ||  tag.equals(BlockTags.BUTTONS) ||  tag.equals(BlockTags.CAMPFIRES)
                              || tag.equals(BlockTags.CAULDRONS) || tag.equals(BlockTags.FENCE_GATES) || tag.equals(BlockTags.TRAPDOORS)  || tag.equals(BlockTags.DOORS);
                 })
                || MinecraftClient.getInstance().world.getBlockState(pos).getBlock() instanceof CraftingTableBlock
                || MinecraftClient.getInstance().world.getBlockState(pos).getBlock() instanceof FurnaceBlock
                || MinecraftClient.getInstance().world.getBlockState(pos).getBlock() instanceof BarrelBlock
                || MinecraftClient.getInstance().world.getBlockState(pos).getBlock() instanceof BellBlock
                || MinecraftClient.getInstance().world.getBlockState(pos).getBlock() instanceof BeehiveBlock
                || MinecraftClient.getInstance().world.getBlockState(pos).getBlock() instanceof BlastFurnaceBlock
                || MinecraftClient.getInstance().world.getBlockState(pos).getBlock() instanceof CakeBlock
                || MinecraftClient.getInstance().world.getBlockState(pos).getBlock() instanceof HopperBlock
                || MinecraftClient.getInstance().world.getBlockState(pos).getBlock() instanceof BlockWithEntity
                || MinecraftClient.getInstance().world.getBlockState(pos).getBlock() instanceof BlockEntityProvider
                || MinecraftClient.getInstance().world.getBlockState(pos).getBlock() instanceof BeaconBlock
                || MinecraftClient.getInstance().world.getBlockState(pos).getBlock() instanceof LoomBlock

        ) && MinecraftClient.getInstance().player.getPos().distanceTo(pos.toCenterPos())<
                4.5));
    }
    public static boolean isInteractable(BlockPos result){
        return (((MinecraftClient.getInstance().world.getBlockEntity(result)) != null ||
                MinecraftClient.getInstance().world.getBlockState(result).streamTags().anyMatch(tag ->{
                    return tag.equals(BlockTags.ALL_SIGNS) ||  tag.equals(BlockTags.ANVIL) ||  tag.equals(BlockTags.BUTTONS) ||  tag.equals(BlockTags.CAMPFIRES)
                            || tag.equals(BlockTags.CAULDRONS) || tag.equals(BlockTags.FENCE_GATES) || tag.equals(BlockTags.TRAPDOORS)  || tag.equals(BlockTags.DOORS);
                })
                || MinecraftClient.getInstance().world.getBlockState(result).getBlock() instanceof CraftingTableBlock
                || MinecraftClient.getInstance().world.getBlockState(result).getBlock() instanceof FurnaceBlock
                || MinecraftClient.getInstance().world.getBlockState(result).getBlock() instanceof BarrelBlock
                || MinecraftClient.getInstance().world.getBlockState(result).getBlock() instanceof BellBlock
                || MinecraftClient.getInstance().world.getBlockState(result).getBlock() instanceof BeehiveBlock
                || MinecraftClient.getInstance().world.getBlockState(result).getBlock() instanceof BlastFurnaceBlock
                || MinecraftClient.getInstance().world.getBlockState(result).getBlock() instanceof CakeBlock
                || MinecraftClient.getInstance().world.getBlockState(result).getBlock() instanceof HopperBlock
                || MinecraftClient.getInstance().world.getBlockState(result).getBlock() instanceof BlockWithEntity
                || MinecraftClient.getInstance().world.getBlockState(result).getBlock() instanceof BlockEntityProvider
                || MinecraftClient.getInstance().world.getBlockState(result).getBlock() instanceof BeaconBlock
                || MinecraftClient.getInstance().world.getBlockState(result).getBlock() instanceof LoomBlock

        ) && MinecraftClient.getInstance().player.getPos().distanceTo(result.toCenterPos())<
                4.5);
    }
}