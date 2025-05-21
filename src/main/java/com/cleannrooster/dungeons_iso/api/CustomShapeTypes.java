package com.cleannrooster.dungeons_iso.api;

import com.cleannrooster.dungeons_iso.compat.SodiumCompat;
import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.RaycastContext;

public enum CustomShapeTypes implements RaycastContext.ShapeProvider {

    CULLED((state, world, pos, context) -> {
        for(BlockCuller culler : SodiumCompat.blockCullers){
            if(culler.shouldCull(pos,MinecraftClient.getInstance().gameRenderer.getCamera(),MinecraftClient.getInstance().cameraEntity)){
                return VoxelShapes.empty();
            }
        }

            return  state.getCameraCollisionShape(world,pos,context);

    }),
    AIR_AT_LEVEL((state, world, pos, context) -> {
        for(BlockCuller culler : SodiumCompat.blockCullers){
            if(culler.shouldCull(pos,MinecraftClient.getInstance().gameRenderer.getCamera(),MinecraftClient.getInstance().cameraEntity)){
                return VoxelShapes.empty();
            }
        }
        if(pos.getY() > MinecraftClient.getInstance().cameraEntity.getY()){
            return VoxelShapes.empty();

        }
        else{
            return VoxelShapes.cuboid(0,0,0,1,Math.max(0,((MinecraftClient.getInstance().cameraEntity.getY()*16) % 16F)/16F),1);
        }

    });

    private CustomShapeTypes(final RaycastContext.ShapeProvider provider) {
        this.provider = provider;
    }
    private final RaycastContext.ShapeProvider provider;


    public VoxelShape get(BlockState blockState, BlockView blockView, BlockPos blockPos, ShapeContext shapeContext) {
        return this.provider.get(blockState, blockView, blockPos, shapeContext);
    }
}
