package com.cleannrooster.dungeons_iso.neoforge.client;

import com.cleannrooster.dungeons_iso.api.cullers.room.TerrainCulling;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.DelegateBlockStateModel;

import java.util.List;

/** Position-aware terrain removal through NeoForge's native 26.2 model API. */
@EventBusSubscriber(modid = "dungeons_iso", value = Dist.CLIENT)
public final class NeoForgeTerrainCullingModels {

    private NeoForgeTerrainCullingModels() {
    }

    @SubscribeEvent
    public static void modifyModels(ModelEvent.ModifyBakingResult event) {
        event.getBakingResult().blockStateModels().replaceAll((state, model) ->
                model instanceof CullingModel ? model : new CullingModel(model));
    }

    private static final class CullingModel extends DelegateBlockStateModel {

        private CullingModel(BlockStateModel delegate) {
            super(delegate);
        }

        @Override
        public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state,
                                 RandomSource random, List<BlockStateModelPart> parts) {
            try {
                if (TerrainCulling.shouldRemove(state, pos.getX(), pos.getY(), pos.getZ())) {
                    return;
                }
            } catch (Exception ignored) {
            }
            super.collectParts(level, pos, state, random, parts);
        }
    }
}
