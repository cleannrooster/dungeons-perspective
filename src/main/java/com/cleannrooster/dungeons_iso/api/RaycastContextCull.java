package com.cleannrooster.dungeons_iso.api;

import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.RaycastContext;

public class RaycastContextCull extends RaycastContext {
    CustomShapeTypes cull;
    private final Vec3d start;
    private final Vec3d end;
    private final ShapeType shapeType;
    private final FluidHandling fluid;
    private final ShapeContext shapeContext;
    public RaycastContextCull(Vec3d start, Vec3d end, CustomShapeTypes cull, ShapeType shapeType, FluidHandling fluidHandling, Entity entity) {
        super(start, end, shapeType, fluidHandling, entity);
        this.cull = cull;
        this.start = start;
        this.end = end;
        this.shapeType = shapeType;
        this.fluid = fluidHandling;
        this.shapeContext = ShapeContext.of(entity);
    }
    public VoxelShape getBlockShape(BlockState state, BlockView world, BlockPos pos) {
        return this.cull.get(state, world, pos, this.shapeContext);
    }
}
