package com.cleannrooster.dungeons_iso.api.cullers;

import com.cleannrooster.dungeons_iso.api.BlockCuller;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.annotation.Nullable;
import net.minecraft.block.enums.BlockFace;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.List;

public class GenericBlockCuller implements BlockCuller {
    public static List<BlockPos> culledBlocks = new ArrayList<>(1000000);

    @Override
    public List<BlockPos> cullBlocks(  BlockPos blockPos, Camera camera, Entity cameraEntity, Direction face) {

        if(blockPos != null && ((int)Math.abs(MinecraftClient.getInstance().cameraEntity.getPos().subtract((blockPos.toCenterPos())).normalize().multiply(1,1,1)
                .dotProduct(
                        (MinecraftClient.getInstance().cameraEntity.getPos())
                                .subtract(camera.getPos()).normalize().multiply(8,8,8))) >= (int)(0.8*8) && blockPos.toCenterPos().getY() > cameraEntity.getY()+1)){
            if (culledBlocks.size() < 1000000) {
                culledBlocks.add(blockPos);
            }
        }
        return culledBlocks;
    }

    @Override
    public List<BlockPos> getCulledBlocks() {
        return culledBlocks;
    }

    @Override
    public void resetCulledBlocks() {
        culledBlocks = new ArrayList<>(1000000) ;
    }
}
