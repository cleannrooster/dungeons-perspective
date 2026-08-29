package com.cleannrooster.dungeons_iso.mod;

import com.cleannrooster.dungeons_iso.compat.DragonCompat;
import com.cleannrooster.dungeons_iso.config.Config;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.BeaconBlock;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.BlastFurnaceBlock;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.FurnaceBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.LoomBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector2f;

import java.util.Objects;

public class Mod {
    public static BlockState prevblock;
    public static float yaw;
    public static float pitch = 45;
    public static double x;
    public static boolean shouldReload;
    public static double flyingYAddition;
    public static boolean shouldRebuild() {
        return Mod.shouldReload && Mod.endTime < 10;
    }
    public static double z;
    public static int cooldownIs;
    public static int cooldownWas;
    public static Vector2f unModMovement = new Vector2f();
    public static float joystickRawFwd;
    public static float joystickRawSide;
    public static int useTimer;
    public static boolean noMouse;
    public static float zoomMetric;
    public static float factor;
    public static float factorScale = 0.5F;
    public static int zoomOutTimeNoDelay;
    public static float zoomTimeNoDelay;
    public static float factor2;
    public static long startZoomNoDelay;
    public static boolean isBlocked;
    public static float relativeYaw;
    public static float clipMetric = 0;
    public static int blockedTime;
    public static BlockHitResult hit;
    public static boolean forward;
    public static boolean notmoving;
    public static Vec3 preMod = Vec3.ZERO;
    public static float livingPitch;
    public static float livingBodyYaw;
    public static float livingYaw;
    public static float livingHeadYaw;

    public static float getScaledFactor() {
        return factor;
    }

    public static float getZoom() {
        float modifier = 1F;

        boolean bool = Config.GSON.instance().clipToSpace && Mod.shouldRebuild();
        if (Minecraft.getInstance().level != null) {

            if (bool) {
                modifier = (Math.min(2F, ((float) 1F + ((DragonCompat.bool ? (float) (Math.max(0F, (float) Minecraft.getInstance().level.getGameTime() - (float) Mod.dragonTimeSince + Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true))) : Math.max(0F, 20F - (float) Minecraft.getInstance().level.getGameTime() + (float) Mod.dragonTime + Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true)))) / 20F)))  * Math.clamp(Config.GSON.instance().zoomFactor, 1F, 1.5F) * Mod.zoom;

            } else {

                if (Config.GSON.instance().clipToSpace) {
                    modifier = (Math.min(2F, ((float) 1F + ((DragonCompat.bool ? (float) (Math.max(0F, (float) Minecraft.getInstance().level.getGameTime() - (float) Mod.dragonTimeSince + Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true))) : Math.max(0F, 20F - (float) Minecraft.getInstance().level.getGameTime() + (float) Mod.dragonTime + Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true)))) / 20F))) * Math.clamp(Config.GSON.instance().zoomFactor, 1F, 1.5F) * Mod.zoom;

                } else {
                    modifier =  Mod.zoom;

                }
            }

            if (Minecraft.getInstance().getCameraEntity() instanceof LivingEntity living) {
                modifier *= living.getScale();
            }
        }
        if(Objects.isNull(hit) || !Config.GSON.instance().clipToSpace) {
            return modifier;
        }
        return (float) Math.max(0.5F*modifier*((Math.clamp((Mod.clipMetric+(Mod.notmoving ? 0 :  Mod.forward ? 0.4F: -1.0F)*Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true)),16,32))/(32)),2F);
    }

    public static float zoom = 5.0F;
    public static boolean enabled = false;
    public static CameraType lastPerspective;
    public static HitResult crosshairTarget;
    public static Entity pickedTarget;
    public static Entity targeted;
    public static BlockPos targetedInteractable;
    public static BlockPos pendingInteractable;
    public static int pendingInteractableTicks;

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

    // 26.2: BlockState#getTags() was removed; query tag membership via the block's registry holder.
    private static boolean isInteractTag(BlockState state) {
        var holder = state.getBlock().builtInRegistryHolder();
        return holder.is(BlockTags.ALL_SIGNS) || holder.is(BlockTags.ANVIL) || holder.is(BlockTags.BUTTONS) || holder.is(BlockTags.CAMPFIRES)
                || holder.is(BlockTags.CAULDRONS) || holder.is(BlockTags.FENCE_GATES) || holder.is(BlockTags.TRAPDOORS) || holder.is(BlockTags.DOORS);
    }

    public static boolean isInteractable(BlockHitResult result){
        BlockPos pos = result.getBlockPos();

        return (((Minecraft.getInstance().level.getBlockEntity(pos) != null ||
                 isInteractTag(Minecraft.getInstance().level.getBlockState(result.getBlockPos()))
                || Minecraft.getInstance().level.getBlockState(pos).getBlock() instanceof CraftingTableBlock
                || Minecraft.getInstance().level.getBlockState(pos).getBlock() instanceof FurnaceBlock
                || Minecraft.getInstance().level.getBlockState(pos).getBlock() instanceof BarrelBlock
                || Minecraft.getInstance().level.getBlockState(pos).getBlock() instanceof BellBlock
                || Minecraft.getInstance().level.getBlockState(pos).getBlock() instanceof BeehiveBlock
                || Minecraft.getInstance().level.getBlockState(pos).getBlock() instanceof BlastFurnaceBlock
                || Minecraft.getInstance().level.getBlockState(pos).getBlock() instanceof CakeBlock
                || Minecraft.getInstance().level.getBlockState(pos).getBlock() instanceof HopperBlock
                || Minecraft.getInstance().level.getBlockState(pos).getBlock() instanceof BaseEntityBlock
                || Minecraft.getInstance().level.getBlockState(pos).getBlock() instanceof EntityBlock
                || Minecraft.getInstance().level.getBlockState(pos).getBlock() instanceof BeaconBlock
                || Minecraft.getInstance().level.getBlockState(pos).getBlock() instanceof LoomBlock

        ) && Minecraft.getInstance().player.position().distanceTo(Vec3.atCenterOf(pos))<
                Minecraft.getInstance().player.blockInteractionRange()));
    }
    public static boolean isInteractable(BlockPos result){
        return (((Minecraft.getInstance().level.getBlockEntity(result)) != null ||
                isInteractTag(Minecraft.getInstance().level.getBlockState(result))
                || Minecraft.getInstance().level.getBlockState(result).getBlock() instanceof CraftingTableBlock
                || Minecraft.getInstance().level.getBlockState(result).getBlock() instanceof FurnaceBlock
                || Minecraft.getInstance().level.getBlockState(result).getBlock() instanceof BarrelBlock
                || Minecraft.getInstance().level.getBlockState(result).getBlock() instanceof BellBlock
                || Minecraft.getInstance().level.getBlockState(result).getBlock() instanceof BeehiveBlock
                || Minecraft.getInstance().level.getBlockState(result).getBlock() instanceof BlastFurnaceBlock
                || Minecraft.getInstance().level.getBlockState(result).getBlock() instanceof CakeBlock
                || Minecraft.getInstance().level.getBlockState(result).getBlock() instanceof HopperBlock
                || Minecraft.getInstance().level.getBlockState(result).getBlock() instanceof BaseEntityBlock
                || Minecraft.getInstance().level.getBlockState(result).getBlock() instanceof EntityBlock
                || Minecraft.getInstance().level.getBlockState(result).getBlock() instanceof BeaconBlock
                || Minecraft.getInstance().level.getBlockState(result).getBlock() instanceof LoomBlock

        ) && Minecraft.getInstance().player.position().distanceTo(Vec3.atCenterOf(result))<
                Minecraft.getInstance().player.blockInteractionRange());
    }
    public static int frustrumZoom;
    public static int cooldown;

}
