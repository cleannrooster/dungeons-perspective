package com.cleannrooster.dungeons_iso.mixin.compat.sodium;

import com.cleannrooster.dungeons_iso.api.BlockCuller;
import com.cleannrooster.dungeons_iso.api.MinecraftClientAccessor;
import com.cleannrooster.dungeons_iso.compat.SodiumCompat;
import com.cleannrooster.dungeons_iso.mod.Mod;
import dev.kosmx.playerAnim.core.util.Vec3f;
import net.caffeinemc.mods.sodium.client.model.color.ColorProvider;
import net.caffeinemc.mods.sodium.client.model.color.ColorProviderRegistry;
import net.caffeinemc.mods.sodium.client.model.color.DefaultColorProviders;
import net.caffeinemc.mods.sodium.client.model.light.LightMode;
import net.caffeinemc.mods.sodium.client.model.light.LightPipeline;
import net.caffeinemc.mods.sodium.client.model.light.data.QuadLightData;
import net.caffeinemc.mods.sodium.client.model.quad.ModelQuadView;
import net.caffeinemc.mods.sodium.client.model.quad.blender.BlendedColorProvider;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.material.DefaultMaterials;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.material.Material;
import net.caffeinemc.mods.sodium.client.render.frapi.mesh.MutableQuadViewImpl;
import net.caffeinemc.mods.sodium.client.render.frapi.render.AbstractBlockRenderContext;
import net.caffeinemc.mods.sodium.client.services.PlatformModelAccess;
import net.caffeinemc.mods.sodium.client.services.SodiumModelData;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.renderer.v1.material.ShadeMode;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.loader.impl.lib.sat4j.core.Vec;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.color.world.BiomeColors;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedModelManager;
import net.minecraft.entity.Entity;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.resource.ResourceReloader;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.*;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Optional;

@Mixin(BlockRenderer.class)
public abstract class MixinBlockRenderer extends AbstractBlockRenderContext {


/*    @Inject(at = @At("HEAD"), method = "transform", cancellable = true, remap = false)
    protected final void transformDungeons(MutableQuadView q, CallbackInfoReturnable<Boolean> info) {
        if(MinecraftClient.getInstance() != null && MinecraftClient.getInstance().player instanceof ClientPlayerEntity player && Mod.enabled && ((MinecraftClientAccessor)MinecraftClient.getInstance()).shouldRebuild()) {

            if(Mod.enabled){
                info.setReturnValue(true);
            }
        }
    }*/
    @Shadow
    private  ColorProviderRegistry colorProviderRegistry;

    @Shadow
private  Vector3f posOffset ;
    private Vec3d posOffsetOffset = Vec3d.ZERO;

    @Shadow
    private @Nullable ColorProvider<BlockState> colorProvider;

    public static class TranslucencyProvider<T extends  BlockState> implements ColorProvider<BlockState> {


        private TranslucencyProvider() {
        }

        protected int getColor(LevelSlice slice, BlockState state, BlockPos pos) {
            if (SodiumCompat.blockCullers.stream().anyMatch(blockCuller -> blockCuller.shouldCull(pos,MinecraftClient.getInstance().gameRenderer.getCamera(),MinecraftClient.getInstance().cameraEntity))) {

                return ColorHelper.Argb.getArgb((int) (255 * (1 - 1)), 0, 0,0);

            }
            return ColorHelper.Argb.getArgb((int) 255,255,255);
        }

        @Override
        public void getColors(LevelSlice levelSlice, BlockPos blockPos, BlockPos.Mutable mutable, BlockState blockState, ModelQuadView modelQuadView, int[] ints) {
            Arrays.fill(ints, -16777216 | this.getColor(levelSlice, blockState, blockPos));

        }
    }

    @Inject(at = @At("HEAD"), method = "renderModel", cancellable = true,remap = false)

    public void renderModelXIVCOLORPROVIDER(BakedModel model, BlockState state, BlockPos pos, BlockPos origin, CallbackInfo ci) {
        if(Mod.enabled ) {
            if (SodiumCompat.detector.shouldCull(pos, MinecraftClient.getInstance().gameRenderer.getCamera(), MinecraftClient.getInstance().cameraEntity)) {
                Mod.shouldReload = true;
                if (!Mod.dirty) {
                    Mod.startTime = MinecraftClient.getInstance().world.getTime();
                }
                Mod.dirtyTime = MinecraftClient.getInstance().world.getTime();

                Mod.dirty = true;

            } else {
                if (!Mod.dirty) {
                    Mod.shouldReload = false;
                }
            }


            boolean bool = false;
            for (BlockCuller culler : SodiumCompat.blockCullers) {
                if ((culler.shouldForceCull() && !bool) || (bool && culler.shouldForceNonCull())) {
                    bool = culler.shouldCull(pos, MinecraftClient.getInstance().gameRenderer.getCamera(), MinecraftClient.getInstance().cameraEntity);
                }
            }
            if (bool) {
                this.state = Blocks.GLASS.getDefaultState();
                this.pos = pos;
                this.randomSeed = state.getRenderingSeed(pos);
                this.posOffset.set((float) origin.getX(), (float) origin.getY(), (float) origin.getZ());
                if (state.hasModelOffset()) {
                    Vec3d modelOffset = state.getModelOffset(this.level, pos);
                    this.posOffset.add((float) modelOffset.x, (float) modelOffset.y, (float) modelOffset.z);
                }

                this.colorProvider = this.colorProviderRegistry.getColorProvider(state.getBlock());
                this.type = RenderLayers.getBlockLayer(state);
                this.prepareCulling(true);
                this.prepareAoInfo(model.useAmbientOcclusion());
                this.modelData = PlatformModelAccess.getInstance().getModelData(this.slice, MinecraftClient.getInstance().getBlockRenderManager().getModel(Blocks.AIR.getDefaultState()), Blocks.AIR.getDefaultState(), pos, this.slice.getPlatformModelData(pos));
                Iterable<RenderLayer> renderTypes = PlatformModelAccess.getInstance().getModelRenderTypes(this.level, MinecraftClient.getInstance().getBlockRenderManager().getModel(Blocks.AIR.getDefaultState()), Blocks.AIR.getDefaultState(), pos, this.random, this.modelData);
                Iterator var6 = renderTypes.iterator();

                while (var6.hasNext()) {
                    RenderLayer type = (RenderLayer) var6.next();
                    this.type = type;
                    ((FabricBakedModel) MinecraftClient.getInstance().getBlockRenderManager().getModel(Blocks.AIR.getDefaultState())).emitBlockQuads(this.level, Blocks.GLASS.getDefaultState(), pos, this.randomSupplier, this);
                }

                this.type = null;
                this.modelData = SodiumModelData.EMPTY;
                ci.cancel();
                return;
            }
        }

    }

}
