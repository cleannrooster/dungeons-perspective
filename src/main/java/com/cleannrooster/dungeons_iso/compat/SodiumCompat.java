package com.cleannrooster.dungeons_iso.compat;

import com.cleannrooster.dungeons_iso.api.BlockCuller;
import com.cleannrooster.dungeons_iso.api.FogOfWar;
import com.cleannrooster.dungeons_iso.api.cullers.BlockDetector;
import com.cleannrooster.dungeons_iso.api.cullers.FloodCuller;
import com.cleannrooster.dungeons_iso.api.cullers.GenericCuller3;
import com.cleannrooster.dungeons_iso.api.cullers.GenericBlockCuller2;
import com.cleannrooster.dungeons_iso.config.Config;
import com.cleannrooster.dungeons_iso.mod.Mod;
import net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
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


        Box box = new Box(MinecraftClient.getInstance().player.getEyePos(),MinecraftClient.getInstance().gameRenderer.getCamera().getPos()).expand(1,0,1);
        if(MinecraftClient.getInstance().cameraEntity!= null) {
            stream = floodCuller.getCulledBlocks(MinecraftClient.getInstance().player.getBlockPos().up(), MinecraftClient.getInstance().gameRenderer.getCamera(), MinecraftClient.getInstance().cameraEntity);
        }
        //System.out.println(stream.toArray().length);
        double dub = 1.25*Mod.getZoom()*Mod.zoomMetric;
        var vec = camera.getRotation().transform(new Vector3d(1,1,-1));
        box.expand(dub,dub,dub);
        SodiumWorldRenderer.instance().scheduleRebuildForBlockArea((int) box.minX, (int) box.minY, (int) box.minZ, (int) box.maxX, (int) box.maxY, (int) box.maxZ, true);

        for(BlockCuller culler :blockCullers) {
            if(MinecraftClient.getInstance().player.age % culler.frequency() == 0) {
                culler.resetCulledBlocks();
            }


        }
    }
}
