package com.cleannrooster.dungeons_iso.compat;

import com.cleannrooster.dungeons_iso.api.BlockCuller;
import com.cleannrooster.dungeons_iso.api.FogOfWar;
import com.cleannrooster.dungeons_iso.api.cullers.BlockDetector;
import com.cleannrooster.dungeons_iso.api.cullers.FloodCuller;
import com.cleannrooster.dungeons_iso.api.cullers.GenericCuller3;
import com.cleannrooster.dungeons_iso.api.cullers.GenericBlockCuller2;
import com.cleannrooster.dungeons_iso.api.cullers.room.RoomScanner;
import com.cleannrooster.dungeons_iso.api.cullers.room.GhostRenderer;
import com.cleannrooster.dungeons_iso.api.cullers.room.SectionRebuildQueue;
import com.cleannrooster.dungeons_iso.api.cullers.room.SightlineScanner;
import com.cleannrooster.dungeons_iso.config.Config;
import com.cleannrooster.dungeons_iso.mod.Mod;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Stream;

import static net.minecraft.world.effect.MobEffects.DARKNESS;

public class SodiumCompat {
    public static List<BlockCuller> blockCullers = new ArrayList<>();
    public static BlockCuller detector = new BlockDetector();
    public static List<BlockCuller> blockCullersShapes = new ArrayList<>();

    public static LinkedHashMap<BlockPos,BlockCuller.TransparentBlock> transparentBlocks;
    public static FloodCuller floodCuller = new FloodCuller();
    public static FogOfWar fogOfWar;
    public static HashMap<List<Integer>, HitResult> map = new HashMap<>();
    private static BlockPos lastScanPos;
    private static int lastScanTick = Integer.MIN_VALUE;
    private static ResourceKey<Level> lastDimension;
    private static BlockPos lastSightlinePos;
    private static Vec3 lastCameraPos;
    private static int lastSightlineTick = Integer.MIN_VALUE;
    static{
        blockCullers.addAll(List.of( new GenericCuller3(),floodCuller) );
        transparentBlocks = new LinkedHashMap<>();
        blockCullersShapes.add(new GenericCuller3());

    }


    public static void run(){
        Camera camera = Minecraft.getInstance().gameRenderer.mainCamera();

        if(fogOfWar == null){
            fogOfWar = new FogOfWar();
        }
        if(((Minecraft.getInstance().getCameraEntity() instanceof LivingEntity living && living.hasEffect(DARKNESS)) ||Config.GSON.instance().fogOfWar)) {
            fogOfWar.map = fogOfWar.getMap();
            fogOfWar.populatePoints();
        }
        else{
            fogOfWar.map = null;
            fogOfWar.realPoints = null;
            fogOfWar.points = null;
        }

        tickTerrainScanners();


        for(BlockCuller culler :blockCullers) {
            if(Minecraft.getInstance().player.tickCount % culler.frequency() == 0) {
                culler.resetCulledBlocks();
            }


        }
    }

    private static boolean cooledDown(int age, int last, int cooldown) {
        if (last == Integer.MIN_VALUE) {
            return true;
        }
        int since = age - last;
        return since < 0 || since >= cooldown;
    }

    /** Drives both asynchronous scanners and the shared, renderer-neutral rebuild queue. */
    private static void tickTerrainScanners() {
        Minecraft client = Minecraft.getInstance();
        ClientLevel world = client.level;
        Entity cameraEntity = client.getCameraEntity();
        if (world == null || cameraEntity == null || client.player == null) {
            return;
        }

        ResourceKey<Level> dimension = world.dimension();
        if (!dimension.equals(lastDimension)) {
            lastDimension = dimension;
            lastScanPos = null;
            lastSightlinePos = null;
            lastCameraPos = null;
            SectionRebuildQueue.INSTANCE.clear();
            RoomScanner.INSTANCE.reset();
            SightlineScanner.INSTANCE.reset();
            GhostRenderer.invalidate();
            return;
        }

        BlockPos playerPos = cameraEntity.blockPosition();
        boolean roomActive = Mod.enabled && Mod.shouldRebuild() && Config.GSON.instance().roomCulling;
        LongOpenHashSet toggled = RoomScanner.INSTANCE.setActive(roomActive);
        if (toggled != null) {
            SectionRebuildQueue.INSTANCE.submit(toggled, playerPos);
        }

        if (Config.GSON.instance().roomCulling) {
            int cooldown = Math.max(1, Config.GSON.instance().roomRescanCooldown);
            boolean moved = lastScanPos == null || !lastScanPos.equals(playerPos);
            if (moved && cooledDown(client.player.tickCount, lastScanTick, cooldown)) {
                lastScanTick = client.player.tickCount;
                lastScanPos = playerPos.immutable();
                RoomScanner.INSTANCE.requestScan(world, playerPos);
            }
            LongOpenHashSet dirty = RoomScanner.INSTANCE.pollCompleted();
            if (dirty != null) {
                SectionRebuildQueue.INSTANCE.submit(dirty, playerPos);
            }
        }

        tickSightlineScanner(world, cameraEntity, playerPos);
        SectionRebuildQueue.INSTANCE.drain();
    }

    private static void tickSightlineScanner(ClientLevel world, Entity cameraEntity,
                                              BlockPos playerPos) {
        boolean active = Mod.enabled && Mod.shouldRebuild() && Config.GSON.instance().shapeCulling;
        LongOpenHashSet toggled = SightlineScanner.INSTANCE.setActive(active);
        if (toggled != null) {
            SectionRebuildQueue.INSTANCE.submit(toggled, playerPos);
        }
        if (!active) {
            return;
        }

        Vec3 camera = Mod.preMod;
        if (camera == null || camera.equals(Vec3.ZERO)) {
            return;
        }
        double distance = camera.distanceTo(cameraEntity.position());
        if (distance < 1.0 || distance > 128.0) {
            return;
        }

        int cooldown = Math.max(1, Config.GSON.instance().sightlineRescanCooldown);
        double step = Math.max(0.05F, Config.GSON.instance().sightlineCameraStep);
        int age = Minecraft.getInstance().player.tickCount;
        boolean cameraMoved = lastCameraPos == null
                || lastCameraPos.distanceToSqr(camera) > step * step;
        boolean playerMoved = lastSightlinePos == null || !lastSightlinePos.equals(playerPos);
        if ((cameraMoved || playerMoved) && cooledDown(age, lastSightlineTick, cooldown)) {
            lastSightlineTick = age;
            lastCameraPos = camera;
            lastSightlinePos = playerPos.immutable();
            SightlineScanner.INSTANCE.requestScan(world, camera, cameraEntity.getBoundingBox());
        }

        LongOpenHashSet dirty = SightlineScanner.INSTANCE.pollCompleted();
        if (dirty != null) {
            SectionRebuildQueue.INSTANCE.submit(dirty, playerPos);
        }
    }
}
