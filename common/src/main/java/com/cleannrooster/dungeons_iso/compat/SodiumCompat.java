package com.cleannrooster.dungeons_iso.compat;

import com.cleannrooster.dungeons_iso.api.BlockCuller;
import com.cleannrooster.dungeons_iso.api.FogOfWar;
import com.cleannrooster.dungeons_iso.api.cullers.BlockDetector;
import com.cleannrooster.dungeons_iso.api.cullers.FloodCuller;
import com.cleannrooster.dungeons_iso.api.cullers.GenericCuller3;
import com.cleannrooster.dungeons_iso.api.cullers.GenericBlockCuller2;
import com.cleannrooster.dungeons_iso.api.cullers.room.GhostRenderer;
import com.cleannrooster.dungeons_iso.api.cullers.room.RoomScanner;
import com.cleannrooster.dungeons_iso.api.cullers.room.SectionRebuildQueue;
import com.cleannrooster.dungeons_iso.api.cullers.room.SightlineScanner;
import com.cleannrooster.dungeons_iso.config.Config;
import com.cleannrooster.dungeons_iso.mod.Mod;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Stream;

import static net.minecraft.entity.effect.StatusEffects.DARKNESS;

public class SodiumCompat {
    public static List<BlockCuller> blockCullers = new ArrayList<>();
    public static BlockCuller detector = new BlockDetector();
    public static List<BlockCuller> blockCullersShapes = new ArrayList<>();

    public static FloodCuller floodCuller = new FloodCuller();
    public static FogOfWar fogOfWar;
    // Room scanner bookkeeping — client thread only.
    private static BlockPos lastScanPos = null;
    private static int lastScanTick = Integer.MIN_VALUE;
    private static RegistryKey<World> lastDimension = null;
    private static BlockPos lastSightlinePos = null;
    private static Vec3d lastCameraPos = null;
    private static int lastSightlineTick = Integer.MIN_VALUE;
    static{
        blockCullers.addAll(List.of( new GenericCuller3(),floodCuller) );
        blockCullersShapes.add(new GenericCuller3());

    }


    public static void run(){
        Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();

        if(fogOfWar == null){
            fogOfWar = new FogOfWar();
        }
        if(((MinecraftClient.getInstance().getCameraEntity() instanceof LivingEntity living && living.hasStatusEffect(DARKNESS)) ||Config.GSON.instance().fogOfWar)) {
            fogOfWar.map = fogOfWar.getMap();
            fogOfWar.populatePoints();
        }
        else{
            fogOfWar.map = null;
            fogOfWar.realPoints = null;
            fogOfWar.points = null;
        }


        tickRoomScanner();

        // The blanket camera-box rebuild that used to live here is gone. It existed to force
        // re-meshing for the cylinder culler, which decided per block at mesh time and so had no
        // idea which sections it had changed. Both scanners now report their affected sections
        // exactly and feed them through the throttled queue, so this was scheduling work nothing
        // needed.
        //
        // It was also expensive in a way that grew with zoom: the box ran from the eye to the
        // camera and was then expanded by 1.25 * zoom * zoomMetric on every axis, so at high zoom
        // it covered a couple of hundred sections — submitted as *important*, on every block the
        // player stepped. Sodium holds a cloned copy of a section's data for each pending build
        // task, so queuing them faster than the build threads drain grows memory until something
        // resets the renderer.

        for(BlockCuller culler :blockCullers) {
            if(MinecraftClient.getInstance().player.age % culler.frequency() == 0) {
                culler.resetCulledBlocks();
            }


        }
    }

    /**
     * Cooldown test that survives both the "never scanned" sentinel and player.age resetting on
     * respawn or dimension change.
     *
     * <p>Written as {@code age - last >= cooldown} this overflows against {@link Integer#MIN_VALUE}
     * and evaluates to a large negative number, so the very first scan never fires — and since the
     * timestamp is only assigned inside that branch, it stays at the sentinel forever and no scan
     * ever runs at all.
     */
    private static boolean cooledDown(int age, int last, int cooldown) {
        if (last == Integer.MIN_VALUE) {
            return true;
        }
        int since = age - last;
        return since < 0 || since >= cooldown;
    }

    /**
     * Drives the asynchronous room scanner: decide whether a rescan is warranted, install any
     * finished snapshot, and spend this tick's section-rebuild budget.
     *
     * <p>All of this is bookkeeping — the flood itself runs on {@code RoomScanner}'s worker thread.
     * The only work that lands on the client thread here is capturing chunk references (a few dozen
     * array reads) and scheduling at most {@code roomSectionsPerTick} section rebuilds.
     */
    private static void tickRoomScanner() {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientWorld world = client.world;
        Entity cameraEntity = client.cameraEntity;
        if (world == null || cameraEntity == null) {
            return;
        }

        RegistryKey<World> dimension = world.getRegistryKey();
        if (!dimension.equals(lastDimension)) {
            lastDimension = dimension;
            lastScanPos = null;
            lastSightlinePos = null;
            lastCameraPos = null;
            SectionRebuildQueue.INSTANCE.clear();
            RoomScanner.INSTANCE.reset();
            SightlineScanner.INSTANCE.reset();
            // Static, so without this the baked geometry and the mask and snapshot it was built
            // from stay reachable after the world they describe is gone.
            GhostRenderer.invalidate();
            return;
        }

        BlockPos playerPos = cameraEntity.getBlockPos();

        // Culling is baked into chunk meshes, so toggling it has to re-mesh what it affects —
        // otherwise sections stay frozen in whichever state they were built under.
        boolean shouldApply = Mod.enabled && Mod.shouldRebuild() && Config.GSON.instance().roomCulling;
        LongOpenHashSet toggled = RoomScanner.INSTANCE.setActive(shouldApply);
        if (toggled != null) {
            SectionRebuildQueue.INSTANCE.submit(toggled, playerPos);
        }

        if (Config.GSON.instance().roomCulling) {
            int cooldown = Math.max(1, Config.GSON.instance().roomRescanCooldown);
            // Rescanning on a timer regardless of movement cost a chunk capture and a full flood
            // five times a second, for a result that was almost always identical — 8,900 scans in
            // one session, nearly all of them outdoors where the scan bails immediately. Whatever
            // staleness that was working around is handled properly now: a scan that produces the
            // same answer is discarded and the previous snapshot kept, and one that concludes
            // "not a room" publishes an empty snapshot rather than leaving the old one standing.
            boolean moved = lastScanPos == null || !lastScanPos.equals(playerPos);
            if (moved && cooledDown(client.player.age, lastScanTick, cooldown)) {
                lastScanTick = client.player.age;
                lastScanPos = playerPos.toImmutable();
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

    /**
     * Drives the sightline veto: works out which terrain shapes between the camera and the player
     * are genuinely hiding the player, so the cylinder stops removing the ones that are not.
     *
     * <p>Retriggered by camera movement as well as player movement, because the thing being tested
     * is a line of sight — orbiting the camera changes the answer even when the player stands still.
     */
    private static void tickSightlineScanner(ClientWorld world, Entity cameraEntity, BlockPos playerPos) {
        boolean shouldApply = Mod.enabled && Mod.shouldRebuild() && Config.GSON.instance().shapeCulling;
        LongOpenHashSet toggled = SightlineScanner.INSTANCE.setActive(shouldApply);
        if (toggled != null) {
            SectionRebuildQueue.INSTANCE.submit(toggled, playerPos);
        }

        if (!Mod.enabled || !Config.GSON.instance().shapeCulling) {
            return;
        }

        Vec3d camera = Mod.preMod;
        if (camera == null || camera.equals(Vec3d.ZERO)) {
            return;
        }
        double distance = camera.distanceTo(cameraEntity.getPos());
        if (distance < 1.0 || distance > 128.0) {
            return;
        }

        int cooldown = Math.max(1, Config.GSON.instance().sightlineRescanCooldown);
        double step = Math.max(0.05F, Config.GSON.instance().sightlineCameraStep);
        int age = MinecraftClient.getInstance().player.age;

        boolean cameraMoved = lastCameraPos == null || lastCameraPos.squaredDistanceTo(camera) > step * step;
        boolean playerMoved = lastSightlinePos == null || !lastSightlinePos.equals(playerPos);

        if ((cameraMoved || playerMoved) && cooledDown(age, lastSightlineTick, cooldown)) {
            lastSightlineTick = age;
            lastCameraPos = camera;
            lastSightlinePos = playerPos.toImmutable();
            SightlineScanner.INSTANCE.requestScan(world, camera, cameraEntity.getBoundingBox());
        }

        LongOpenHashSet dirty = SightlineScanner.INSTANCE.pollCompleted();
        if (dirty != null) {
            SectionRebuildQueue.INSTANCE.submit(dirty, playerPos);
        }
    }
}
