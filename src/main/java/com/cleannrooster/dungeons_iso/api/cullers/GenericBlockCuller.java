package com.cleannrooster.dungeons_iso.api.cullers;

import com.cleannrooster.dungeons_iso.api.BlockCuller;
import com.cleannrooster.dungeons_iso.compat.SodiumCompat;
import com.cleannrooster.dungeons_iso.mod.Mod;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.annotation.Nullable;
import net.fabricmc.fabric.api.block.v1.FabricBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.WallMountedBlock;
import net.minecraft.block.enums.BlockFace;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShapes;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class GenericBlockCuller implements BlockCuller {
    public  List<BlockPos> culledBlocks = new ArrayList<>(1000);

    @Override
    public boolean cullBlocks(  BlockPos blockPos, Camera camera, Entity cameraEntity) {

        if( this.shouldCull(blockPos,camera,cameraEntity)){
            if(culledBlocks.size() < 1000) {
                culledBlocks.add(blockPos);
                TransparentBlock block = new TransparentBlock(blockPos, 0);
                SodiumCompat.transparentBlocks.put(blockPos, SodiumCompat.transparentBlocks.getOrDefault(blockPos, block));
            }
            else{
                culledBlocks = new ArrayList<>(1000);
                SodiumCompat.transparentBlocks = new LinkedHashMap<>();
                culledBlocks.add(blockPos);
                TransparentBlock block = new TransparentBlock(blockPos, 0);
                SodiumCompat.transparentBlocks.put(blockPos, SodiumCompat.transparentBlocks.getOrDefault(blockPos, block));

            }

            return true;
        }
        else{
            return false;
        }
    }

    @Override
    public float blockTransparancy(BlockPos pos) {
        return 0;
    }

    public boolean shouldCull(BlockPos blockPos, Camera camera, Entity cameraEntity){
        if(!(isIgnoredType(cameraEntity.getWorld().getBlockState(blockPos).getBlock())) && blockPos != null && blockPos.toCenterPos().getY() > cameraEntity.getPos().getY() + 1 &&
                (cameraEntity.getPos().subtract((blockPos.toCenterPos())).normalize()
                        .dotProduct(
                                (MinecraftClient.getInstance().cameraEntity.getPos()).subtract(camera.getPos()).normalize()) > 0.71
                        ||
                        camera.getPos().distanceTo(blockPos.toCenterPos()) < 3)){
            return !cameraEntity.getWorld().getBlockState(blockPos).getBlock().equals(Blocks.AIR);
        }
        else{
            return false;
        }
    }
    List<Class<? extends Block>> ignoredTypes = List.of(WallMountedBlock.class);
    public boolean isIgnoredType(Block block){
        for(Class<? extends Block> ignoredType : ignoredTypes){
            if(ignoredType.isInstance(block)){
                return true;
            }
        }
        return false;
    }
    @Override
    public int frequency() {
        return 01;
    }

    @Override
    public List<BlockPos> getCulledBlocks() {
        return culledBlocks;
    }

    @Override
    public void resetCulledBlocks() {
    }

}
