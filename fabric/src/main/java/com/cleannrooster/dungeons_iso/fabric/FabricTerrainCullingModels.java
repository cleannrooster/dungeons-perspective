package com.cleannrooster.dungeons_iso.fabric;

import com.cleannrooster.dungeons_iso.api.cullers.room.TerrainCulling;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.wrapper.WrapperBlockStateModel;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Predicate;

/** Position-aware terrain removal through Fabric's public 26.2 Renderer API. */
public final class FabricTerrainCullingModels {

    private FabricTerrainCullingModels() {
    }

    public static void register() {
        ModelLoadingPlugin.register(context -> context.modifyBlockModelAfterBake().register(
                (model, bakeContext) -> model instanceof CullingModel
                        ? model : new CullingModel(model)));
    }

    private static final class CullingModel extends WrapperBlockStateModel {

        private CullingModel(BlockStateModel wrapped) {
            super(wrapped);
        }

        @Override
        public void emitQuads(QuadEmitter emitter, BlockAndTintGetter level, BlockPos pos,
                              BlockState state, RandomSource random,
                              Predicate<Direction> cullTest) {
            try {
                if (TerrainCulling.shouldRemove(state, pos.getX(), pos.getY(), pos.getZ())) {
                    return;
                }
            } catch (Exception ignored) {
            }
            super.emitQuads(emitter, level, pos, state, random, cullTest);
        }
    }
}
