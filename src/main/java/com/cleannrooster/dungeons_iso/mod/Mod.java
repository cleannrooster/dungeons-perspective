package com.cleannrooster.dungeons_iso.mod;

import com.cleannrooster.dungeons_iso.compat.DragonCompat;
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
    public static BlockState prevblock;
    public static float yaw;
    public static float pitch;
    public static double x;
    public static boolean shouldReload;
    public static double flyingYAddition;

    public static double z;
    public static int cooldownIs;
    public static int cooldownWas;

    public static int useTimer;
    public static boolean noMouse;
    public static float zoomMetric;

    public static float getZoom() {
        float modifier;
        boolean bool = Config.GSON.instance().clipToSpace && Mod.shouldReload;
        if ( bool   ){
            modifier =  ( Math.min(2F,((float) 1F + ((DragonCompat.bool ?(float)(Math.max(0F,(float)MinecraftClient.getInstance().world.getTime()- (float)Mod.dragonTimeSince +MinecraftClient.getInstance().gameRenderer.getCamera().getLastTickDelta())) : Math.max(0F,20F-(float)MinecraftClient.getInstance().world.getTime() + (float)Mod.dragonTime -MinecraftClient.getInstance().gameRenderer.getCamera().getLastTickDelta()) ))/20F)))*(0.5F + 0.5F * ((float) Math.max(Mod.zoomTime , 0F))) * Math.clamp(Config.GSON.instance().zoomFactor, 1F, 1.5F) * Mod.zoom;

        } else {

            if(   Config.GSON.instance().clipToSpace ) {
                modifier=  ( Math.min(2F,((float) 1F + ((DragonCompat.bool ?(float)(Math.max(0F,(float)MinecraftClient.getInstance().world.getTime()- (float)Mod.dragonTimeSince +MinecraftClient.getInstance().gameRenderer.getCamera().getLastTickDelta())) : Math.max(0F,20F-(float)MinecraftClient.getInstance().world.getTime() + (float)Mod.dragonTime -MinecraftClient.getInstance().gameRenderer.getCamera().getLastTickDelta()) ))/20F))) * (0.5F + 0.5F * Math.min(1F,((float) Mod.endTime+MinecraftClient.getInstance().gameRenderer.getCamera().getLastTickDelta()) / 10F)) * Math.clamp(Config.GSON.instance().zoomFactor, 1F, 1.5F) * Mod.zoom;

            }
            else {
                modifier =  Math.clamp(Config.GSON.instance().zoomFactor, 1F, 1.5F) * Mod.zoom;

            }
        }

        return modifier*0.8F;
    }

    public static float zoom = 5.0F;
    public static boolean enabled = false;
    public static Perspective lastPerspective;
    public static HitResult crosshairTarget;
    public static Entity pickedTarget;
    public static Entity targeted;

    public static HitResult prevCrosshairTarget;
    public static Entity lockOnTarget;
    public static boolean dirty;
    public static long dirtyTime;
    public static long startTime;
    public static long endTime;
    public static boolean using;
    public static boolean contextToggle;
    public static boolean rotateToggle;

    public static HitResult mouseTarget;

    public static BlockHitResult horizontalTarget;

    public static long dragonTimeSince;
    public static long dragonTime;
    public static boolean dragonDirty;
    public static float zoomTime;
    public static long startZoom;
    public static float zoomOutTime;
    public static boolean verticalMode;

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
                MinecraftClient.getInstance().player.getBlockInteractionRange()));
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
                MinecraftClient.getInstance().player.getBlockInteractionRange());
    }
}