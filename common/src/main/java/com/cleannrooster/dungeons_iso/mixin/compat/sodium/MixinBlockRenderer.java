package com.cleannrooster.dungeons_iso.mixin.compat.sodium;

import com.cleannrooster.dungeons_iso.api.MinecraftClientAccessor;
import com.cleannrooster.dungeons_iso.api.cullers.room.RoomScanner;
import com.cleannrooster.dungeons_iso.api.cullers.room.RoomSnapshot;
import com.cleannrooster.dungeons_iso.api.cullers.room.SightlineScanner;
import com.cleannrooster.dungeons_iso.api.cullers.room.TerrainCulling;
import com.cleannrooster.dungeons_iso.compat.SodiumCompat;
import com.cleannrooster.dungeons_iso.config.Config;
import com.cleannrooster.dungeons_iso.mod.Mod;
import net.caffeinemc.mods.sodium.client.model.color.ColorProvider;
import net.caffeinemc.mods.sodium.client.model.color.ColorProviderRegistry;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import net.caffeinemc.mods.sodium.client.render.frapi.render.AbstractBlockRenderContext;
import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockRenderer.class)
public abstract class MixinBlockRenderer extends AbstractBlockRenderContext {

    @Shadow
    private  ColorProviderRegistry colorProviderRegistry;
    @Shadow
    private  int[] vertexColors;
    @Shadow
    private ChunkBuildBuffers buffers;

    @Shadow
private  ChunkVertexEncoder.Vertex[] vertices;

    @Shadow
private  Vector3f posOffset ;
    private Vec3d posOffsetOffset = Vec3d.ZERO;
    private int timer = 10;

    // Cached trig value — recomputed only when the config value changes.
    private static float  cachedConeHalfAngle = Float.NaN;
    private static double cachedTanHalfAngle  = 0.0;

    @Shadow
    private @Nullable ColorProvider<BlockState> colorProvider;



    @Inject(at = @At("HEAD"), method = "renderModel", cancellable = true, remap = false)
    public void renderModelXIVCOLORPROVIDER(BakedModel model, BlockState state, BlockPos pos, BlockPos origin, CallbackInfo ci) {
        try {
            if (!Mod.enabled) return;

            MinecraftClient client = MinecraftClient.getInstance();
            Entity cameraEntity = client.cameraEntity;
            if (cameraEntity == null || client.gameRenderer == null || cameraEntity.getWorld() == null) return;

            // Tier 1 — the room snapshot. A single hash lookup, and the authority wherever it has
            // an opinion: it culls the roof of the room the player is in (and the walls and pillars
            // that roof rests on), and explicitly protects the storey above. NO_OPINION means
            // either no roof over this column (outdoors, or under a hole) or no scan yet, in which
            // case shape culling below decides.
            switch (RoomScanner.INSTANCE.test(pos.getX(), pos.getY(), pos.getZ())) {
                case RoomSnapshot.CULL -> {
                    if (!TerrainCulling.isProtected(state.getBlock())) {
                        ci.cancel();
                    }
                    return;
                }
                case RoomSnapshot.KEEP -> {
                    return;
                }
                default -> {
                    // fall through to shape culling
                }
            }

            // Tier 2 — shape culling. The cylinder that used to live here is gone entirely.
            // It answered "is this block near the camera-to-player axis", which is proximity
            // rather than visibility and per block rather than per shape, so it bored a tube
            // through whatever it met. The sightline scanner instead casts rays to the player,
            // segments what they hit into connected shapes, and removes shapes whole, nearest
            // first. There is deliberately no fallback: before a scan lands, or when the budget
            // runs out, the result is fewer complete shapes removed rather than a cylinder.
            if (!SightlineScanner.INSTANCE.shouldCull(pos.getX(), pos.getY(), pos.getZ())) {
                return;
            }

            if (TerrainCulling.isProtected(state.getBlock())) {
                return;
            }

            ci.cancel();
        } catch (Exception ignored) {
        }
    }

}
