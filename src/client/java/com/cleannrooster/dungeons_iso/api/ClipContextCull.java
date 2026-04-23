package com.cleannrooster.dungeons_iso.api;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ClipContextCull extends ClipContext {
    CustomShapeTypes cull;
    private final Vec3 start;
    private final Vec3 end;
    private final Block shapeType;
    private final Fluid fluid;
    private final CollisionContext shapeContext;
    public ClipContextCull(Vec3 start, Vec3 end, CustomShapeTypes cull, Block shapeType, Fluid fluidHandling, Entity entity) {
        super(start, end, shapeType, fluidHandling, entity);
        this.cull = cull;
        this.start = start;
        this.end = end;
        this.shapeType = shapeType;
        this.fluid = fluidHandling;
        this.shapeContext = CollisionContext.of(entity);
    }
    public VoxelShape getBlockShape(BlockState state, BlockGetter world, BlockPos pos) {
        return this.cull.get(state, world, pos, this.shapeContext);
    }
}
