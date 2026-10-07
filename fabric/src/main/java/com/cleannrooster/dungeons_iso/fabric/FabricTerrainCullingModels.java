package com.cleannrooster.dungeons_iso.fabric;

import com.cleannrooster.dungeons_iso.api.cullers.room.TerrainCulling;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier;
import net.fabricmc.fabric.api.renderer.v1.model.ForwardingBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;

import java.util.function.Supplier;

/** Position-aware model culling through Fabric's public Renderer API. */
public final class FabricTerrainCullingModels {

    private FabricTerrainCullingModels() {
    }

    public static void register() {
        ModelLoadingPlugin.register(context -> context.modifyModelAfterBake().register(
                ModelModifier.WRAP_LAST_PHASE,
                (model, bakeContext) -> {
                    if (model == null || bakeContext.topLevelId() == null
                            || model instanceof CullingModel) {
                        return model;
                    }
                    return new CullingModel(model);
                }));
    }

    private static final class CullingModel extends ForwardingBakedModel {

        private CullingModel(BakedModel wrapped) {
            this.wrapped = wrapped;
        }

        @Override
        public boolean isVanillaAdapter() {
            return false;
        }

        @Override
        public void emitBlockQuads(BlockRenderView blockView, BlockState state, BlockPos pos,
                                   Supplier<Random> randomSupplier, RenderContext context) {
            try {
                if (TerrainCulling.shouldRemove(state, pos.getX(), pos.getY(), pos.getZ())) {
                    return;
                }
            } catch (Exception ignored) {
            }
            super.emitBlockQuads(blockView, state, pos, randomSupplier, context);
        }
    }
}
