package com.cleannrooster.dungeons_iso.compat;

import com.cleannrooster.dungeons_iso.api.BlockCuller;
import com.cleannrooster.dungeons_iso.api.FogOfWar;
import com.cleannrooster.dungeons_iso.api.cullers.BlockDetector;
import com.cleannrooster.dungeons_iso.api.cullers.FloodCuller;
import com.cleannrooster.dungeons_iso.api.cullers.GenericCuller3;
import com.cleannrooster.dungeons_iso.config.Config;
import com.cleannrooster.dungeons_iso.mod.Mod;
import net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import static net.minecraft.world.effect.MobEffects.DARKNESS;

public class SodiumCompat {
    public static List<BlockCuller> blockCullers = new ArrayList<>();
    public static BlockCuller detector = new BlockDetector();
    public static List<BlockCuller> blockCullersShapes = new ArrayList<>();

    public static LinkedHashMap<BlockPos,BlockCuller.TransparentBlock> transparentBlocks;
    public static FloodCuller floodCuller = new FloodCuller();
    public static List<BlockPos> stream = List.of();
    public static FogOfWar fogOfWar;
    public static HashMap<List<Integer>, HitResult> map = new HashMap<>();
    static{
        blockCullers.addAll(List.of( new GenericCuller3(),floodCuller) );
        transparentBlocks = new LinkedHashMap<>();
        blockCullersShapes.add(new GenericCuller3());

    }


    public static void run(){
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();

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


        AABB box = new AABB(Minecraft.getInstance().player.getEyePos(), Minecraft.getInstance().gameRenderer.getMainCamera().getPos()).inflate(1.0, 0.0, 1.0);
        if(Minecraft.getInstance().cameraEntity!= null) {
            stream = floodCuller.getCulledBlocks(Minecraft.getInstance().player.getBlockPos().above(), Minecraft.getInstance().gameRenderer.getMainCamera(), Minecraft.getInstance().cameraEntity);
        }
        double dub = 1.25*Mod.getZoom()*Mod.zoomMetric;
        box = box.inflate(dub, dub, dub);

        if (Minecraft.getInstance().player.tickCount % 2 == 0) {
            SodiumWorldRenderer.instance().scheduleRebuildForBlockArea((int) box.minX, (int) box.minY, (int) box.minZ, (int) box.maxX, (int) box.maxY, (int) box.maxZ, true);
        }

        for(BlockCuller culler :blockCullers) {
            if(Minecraft.getInstance().player.tickCount % culler.frequency() == 0) {
                culler.resetCulledBlocks();
            }


        }
    }
}
