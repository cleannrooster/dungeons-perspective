package com.cleannrooster.dungeons_iso.compat;

import com.cleannrooster.dungeons_iso.api.BlockCuller;
import com.cleannrooster.dungeons_iso.api.cullers.BlockDetector;
import com.cleannrooster.dungeons_iso.api.cullers.GenericCuller3;
import com.cleannrooster.dungeons_iso.api.cullers.GenericBlockCuller2;
import net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class SodiumCompat {
    public static List<BlockCuller> blockCullers = new ArrayList<>();
    public static List<BlockCuller> blockCullersShapes = new ArrayList<>();

    public static BlockCuller detector = new BlockDetector();
    public static LinkedHashMap<BlockPos,BlockCuller.TransparentBlock> transparentBlocks;

    static{

        blockCullers.addAll(List.of(new GenericCuller3()));
        blockCullersShapes.add(new GenericCuller3());

        transparentBlocks = new LinkedHashMap<>();
    }


    public static void run(){
        Box box = new Box(MinecraftClient.getInstance().player.getEyePos(),MinecraftClient.getInstance().gameRenderer.getCamera().getPos()).expand(1,0,1);
        double dub = 2*MinecraftClient.getInstance().player.getPos().distanceTo(MinecraftClient.getInstance().gameRenderer.getCamera().getPos());
        box.stretch(dub,dub,dub);
        SodiumWorldRenderer.instance().scheduleRebuildForBlockArea((int) box.minX, (int) box.minY, (int) box.minZ, (int) box.maxX, (int) box.maxY, (int) box.maxZ, true);

    }
}
