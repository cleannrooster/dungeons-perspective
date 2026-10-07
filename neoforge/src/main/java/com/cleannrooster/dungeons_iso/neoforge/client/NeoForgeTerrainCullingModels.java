package com.cleannrooster.dungeons_iso.neoforge.client;

import com.cleannrooster.dungeons_iso.api.cullers.room.TerrainCulling;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;
import net.minecraft.block.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Position-aware model culling through NeoForge's native model-data API. */
@EventBusSubscriber(modid = "dungeons_iso", bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class NeoForgeTerrainCullingModels {

    private NeoForgeTerrainCullingModels() {
    }

    @SubscribeEvent
    public static void modifyModels(ModelEvent.ModifyBakingResult event) {
        event.getModels().replaceAll((id, model) -> model instanceof CullingModel
                ? model
                : new CullingModel(model));
    }

    private static final class CullingModel extends BakedModelWrapper<BakedModel> {

        private static final ModelProperty<Boolean> CULLED = new ModelProperty<>();

        private CullingModel(BakedModel originalModel) {
            super(originalModel);
        }

        @Override
        public ModelData getModelData(BlockRenderView level, BlockPos pos, BlockState state,
                                      ModelData modelData) {
            ModelData delegated = originalModel.getModelData(level, pos, state, modelData);
            try {
                if (TerrainCulling.shouldRemove(state, pos.getX(), pos.getY(), pos.getZ())) {
                    return delegated.derive().with(CULLED, true).build();
                }
            } catch (Exception ignored) {
            }
            return delegated;
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side,
                                        Random rand, ModelData data,
                                        @Nullable RenderLayer renderLayer) {
            if (Boolean.TRUE.equals(data.get(CULLED))) {
                return List.of();
            }
            return originalModel.getQuads(state, side, rand, data, renderLayer);
        }
    }
}
