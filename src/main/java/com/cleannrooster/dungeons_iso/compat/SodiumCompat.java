package com.cleannrooster.dungeons_iso.compat;

import com.cleannrooster.dungeons_iso.api.BlockCuller;
import com.cleannrooster.dungeons_iso.api.MinecraftClientAccessor;
import com.cleannrooster.dungeons_iso.api.WorldRendererAccessor;
import com.cleannrooster.dungeons_iso.api.cullers.DirectionalBlockCuller;
import com.cleannrooster.dungeons_iso.api.cullers.GenericBlockCuller;
import com.cleannrooster.dungeons_iso.config.Config;
import com.cleannrooster.dungeons_iso.mod.Mod;
import net.caffeinemc.mods.sodium.client.SodiumClientMod;
import net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.client.render.chunk.ChunkRendererRegionBuilder;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldView;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class SodiumCompat {
    public static List<BlockCuller> blockCullers = new ArrayList<>();
    public static LinkedHashMap<BlockPos,BlockCuller.TransparentBlock> transparentBlocks;

    static{
        blockCullers.add(new DirectionalBlockCuller());
        transparentBlocks = new LinkedHashMap<>();
    }


    public static void run(){
        Box box = new Box(MinecraftClient.getInstance().player.getEyePos(),MinecraftClient.getInstance().gameRenderer.getCamera().getPos()).expand(1,0,1);
            SodiumWorldRenderer.instance().scheduleRebuildForBlockArea((int) box.minX - 8*2, (int) box.minY - 8*2, (int) box.minZ - 8*2, (int) box.maxX + 8*2, (int) box.maxY + 8*2, (int) box.maxZ + 8*2, true);
/*        if(MinecraftClient.getInstance() != null  && Mod.enabled && ((MinecraftClientAccessor)MinecraftClient.getInstance()).shouldRebuild()) {
            if (MinecraftClient.getInstance().cameraEntity != null && MinecraftClient.getInstance().gameRenderer.getCamera() instanceof Camera camera) {

        for(BlockPos pos: BlockPos.iterate(
                BlockPos.ofFloored((int) box.minX - 8*2, (int) box.minY - 8*2, (int) box.minZ - 8*2),
                BlockPos.ofFloored((int) box.maxX + 8*2, (int) box.maxY + 8*2, (int) box.maxZ + 8*2))){

                    for (BlockCuller culler : SodiumCompat.blockCullers) {
                        if(culler.cullBlocks(pos, camera, MinecraftClient.getInstance().cameraEntity)){
                            BlockCuller.TransparentBlock block =  SodiumCompat.transparentBlocks.get(pos);
                            if( block != null) {
                                block.tickTransparency();
                            }



                        }
                        else{
                            BlockCuller.TransparentBlock block =  SodiumCompat.transparentBlocks.get(pos);
                            if( block != null) {
                                block.tickOpacity();
                            }

                        }



                    }

                }
            }
        }*/

        if(!((MinecraftClientAccessor)MinecraftClient.getInstance()).shouldRebuild()){
            if(Config.GSON.instance().forceNoDefer) {
                if (SodiumClientMod.options().performance.alwaysDeferChunkUpdates) {
                    SodiumClientMod.options().performance.alwaysDeferChunkUpdates = false;
                }
            }
        }
        else{
            if(Config.GSON.instance().forceNoDefer) {

                if (!SodiumClientMod.options().performance.alwaysDeferChunkUpdates) {
                    SodiumClientMod.options().performance.alwaysDeferChunkUpdates = true;
                }
            }

        }

        for(BlockCuller culler :blockCullers) {
            if(MinecraftClient.getInstance().player.age % culler.frequency() == 0) {
                culler.resetCulledBlocks();
            }


        }
    }
}
