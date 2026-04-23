package com.cleannrooster.dungeons_iso.mod;

import com.cleannrooster.dungeons_iso.compat.DragonCompat;
import com.cleannrooster.dungeons_iso.config.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.*;
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
                modifier = (Math.min(2F, ((float) 1F + ((DragonCompat.bool ? (float) (Math.max(0F, (float) Minecraft.getInstance().level.getTime() - (float) Mod.dragonTimeSince + Minecraft.getInstance().gameRenderer.getMainCamera().getLastTickProgress())) : Math.max(0F, 20F - (float) Minecraft.getInstance().level.getTime() + (float) Mod.dragonTime + Minecraft.getInstance().gameRenderer.getMainCamera().getLastTickProgress()))) / 20F)))  * Math.clamp(Config.GSON.instance().zoomFactor, 1F, 1.5F) * Mod.zoom;

            } else {

                if (Config.GSON.instance().clipToSpace) {
                    modifier = (Math.min(2F, ((float) 1F + ((DragonCompat.bool ? (float) (Math.max(0F, (float) Minecraft.getInstance().level.getTime() - (float) Mod.dragonTimeSince + Minecraft.getInstance().gameRenderer.getMainCamera().getLastTickProgress())) : Math.max(0F, 20F - (float) Minecraft.getInstance().level.getTime() + (float) Mod.dragonTime + Minecraft.getInstance().gameRenderer.getMainCamera().getLastTickProgress()))) / 20F))) * Math.clamp(Config.GSON.instance().zoomFactor, 1F, 1.5F) * Mod.zoom;

                } else {
                    modifier =  Mod.zoom;

                }
            }

            if (Minecraft.getInstance().cameraEntity instanceof LivingEntity living) {
                modifier *= living.getScale();
            }
        }
        if(Objects.isNull(hit) || !Config.GSON.instance().clipToSpace) {
            return modifier;
        }
        return (float) Math.max(0.5F*modifier*((Math.clamp((Mod.clipMetric+(Mod.notmoving ? 0 :  Mod.forward ? 0.4F: -1.0F)*Minecraft.getInstance().gameRenderer.getMainCamera().getLastTickProgress()),16,32))/(32)),2F);
    }

    public static float zoom = 5.0F;
    public static boolean enabled = false;
    public static CameraType lastPerspective;
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

        return (((Minecraft.getInstance().level.getBlockEntity(pos) != null ||
                 Minecraft.getInstance().level.getBlockState(result.getBlockPos()).streamTags().anyMatch(tag ->{
                      return tag.equals(BlockTags.ALL_SIGNS) ||  tag.equals(BlockTags.ANVIL) ||  tag.equals(BlockTags.BUTTONS) ||  tag.equals(BlockTags.CAMPFIRES)
                              || tag.equals(BlockTags.CAULDRONS) || tag.equals(BlockTags.FENCE_GATES) || tag.equals(BlockTags.TRAPDOORS)  || tag.equals(BlockTags.DOORS);
                 })
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

        ) && Minecraft.getInstance().player.getPos().distanceTo(pos.toCenterPos())<
                Minecraft.getInstance().player.getBlockInteractionRange()));
    }
    public static boolean isInteractable(BlockPos result){
        return (((Minecraft.getInstance().level.getBlockEntity(result)) != null ||
                Minecraft.getInstance().level.getBlockState(result).streamTags().anyMatch(tag ->{
                    return tag.equals(BlockTags.ALL_SIGNS) ||  tag.equals(BlockTags.ANVIL) ||  tag.equals(BlockTags.BUTTONS) ||  tag.equals(BlockTags.CAMPFIRES)
                            || tag.equals(BlockTags.CAULDRONS) || tag.equals(BlockTags.FENCE_GATES) || tag.equals(BlockTags.TRAPDOORS)  || tag.equals(BlockTags.DOORS);
                })
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

        ) && Minecraft.getInstance().player.getPos().distanceTo(result.toCenterPos())<
                Minecraft.getInstance().player.getBlockInteractionRange());
    }
    public static int frustrumZoom;
    public static int cooldown;

}