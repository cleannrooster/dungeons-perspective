package com.cleannrooster.dungeons_iso.api;

import com.cleannrooster.dungeons_iso.compat.SodiumCompat;
import com.cleannrooster.dungeons_iso.mod.Mod;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public enum CustomShapeTypes implements ClipContext.ShapeGetter {

    CULLED((state, world, pos, context) -> {
        for(BlockCuller culler : SodiumCompat.blockCullersShapes){
            if(Minecraft.getInstance().gameRenderer.getMainCamera() != null && Minecraft.getInstance().cameraEntity != null)
            {
                if (culler.shouldCull(pos, Minecraft.getInstance().gameRenderer.getMainCamera(), Minecraft.getInstance().cameraEntity)) {
                    return Shapes.empty();
                }
                if (culler.shouldIgnoreBlockPick(pos, Minecraft.getInstance().gameRenderer.getMainCamera(), Minecraft.getInstance().cameraEntity)) {
                    return Shapes.empty();
                }
            }
        }

            return  state.getMainCameraCollisionShape(world,pos,context);

    }),
    AIR_AT_LEVEL((state, world, pos, context) -> {
        for(BlockCuller culler : SodiumCompat.blockCullersShapes){
            if(Minecraft.getInstance().gameRenderer.getMainCamera() != null && Minecraft.getInstance().cameraEntity != null) {
                if (culler.shouldCull(pos, Minecraft.getInstance().gameRenderer.getMainCamera(), Minecraft.getInstance().cameraEntity)) {
                    return Shapes.empty();
                }
            }

        }
        if(pos.getY() > Minecraft.getInstance().cameraEntity.getY()){
            return Shapes.empty();

        }
        else{
            return Shapes.cuboid(0,0,0,1,Math.max(0,((Minecraft.getInstance().cameraEntity.getY()*16) % 16F)/16F),1);
        }

    }),
    VERTICAL((state, world, pos, context) -> {
        if(pos.getX()== Mod.horizontalTarget.getBlockPos().getX() || pos.getY() ==Mod.horizontalTarget.getBlockPos().getZ()) {

            return Shapes.cuboid(0, 0, 0, 1, Math.max(0, ((Minecraft.getInstance().cameraEntity.getY() * 16) % 16F) / 16F), 1);
        }
        else{
            return Shapes.empty();
        }
    });

    private CustomShapeTypes(final ClipContext.ShapeGetter provider) {
        this.provider = provider;
    }
    private final ClipContext.ShapeGetter provider;


    public VoxelShape get(BlockState blockState, BlockGetter blockView, BlockPos blockPos, CollisionContext shapeContext) {
        return this.provider.get(blockState, blockView, blockPos, shapeContext);
    }
}
