package com.cleannrooster.dungeons_iso.mixin.compat.sodium;

import com.cleannrooster.dungeons_iso.api.MinecraftAccessor;
import com.cleannrooster.dungeons_iso.compat.SodiumCompat;
import com.cleannrooster.dungeons_iso.config.Config;
import com.cleannrooster.dungeons_iso.mod.Mod;
import net.caffeinemc.mods.sodium.client.model.color.ColorProvider;
import net.caffeinemc.mods.sodium.client.model.color.ColorProviderRegistry;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import net.caffeinemc.mods.sodium.client.render.model.AbstractBlockRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.Vec3;
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
    private Vec3 posOffsetOffset = Vec3.ZERO;
    private int timer = 10;

    @Shadow
    private @Nullable ColorProvider<BlockState> colorProvider;



    @Inject(at = @At("HEAD"), method = "renderModel", cancellable = true, remap = false)
    public void renderModelXIVCOLORPROVIDER(BakedModel model, BlockState state, BlockPos pos, BlockPos origin, CallbackInfo ci) {
        try {
            if (!Mod.enabled) return;

            Minecraft client = Minecraft.getInstance();
            Entity cameraEntity = client.cameraEntity;
            if (cameraEntity == null || client.gameRenderer == null) return;

            // FloodCuller first — O(1) hash lookup, cheapest check
            if (SodiumCompat.floodCuller.isAboveFlood(pos)) {
                ci.cancel();
                return;
            }

            // GenericCuller3 cylinder test — inlined to avoid redundant allocations
            if (!((MinecraftAccessor) client).shouldRebuild()) return;

            Camera camera = client.gameRenderer.getMainCamera();

            // Block center using raw coords — avoids toCenterPos() Vec3 allocation
            double bx = pos.getX() + 0.5;
            double by = pos.getY() + 0.5;
            double bz = pos.getZ() + 0.5;

            double ex = cameraEntity.getX();
            double ey = cameraEntity.getY();
            double ez = cameraEntity.getZ();

            // Only cull blocks above player and closer than camera
            if (by <= ey + 1) return;

            double preX = Mod.preMod.x;
            double preY = Mod.preMod.y;
            double preZ = Mod.preMod.z;

            // Distance check: block must be closer to entity than camera is
            double bdx = bx - ex;
            double bdy = by - ey;
            double bdz = bz - ez;
            double blockDistSq = bdx * bdx + bdy * bdy + bdz * bdz;

            double cdx = preX - ex;
            double cdy = preY - ey;
            double cdz = preZ - ez;
            double camDistSq = cdx * cdx + cdy * cdy + cdz * cdz;

            if (blockDistSq >= camDistSq) return;

            // Skip ignored block types (doors, spawners, etc.)
            Block block = state.getBlock();
            if (block instanceof VaultBlock || block instanceof SpawnerBlock || block instanceof TrialSpawnerBlock
                    || block instanceof FaceAttachedHorizontalDirectionalBlock || block instanceof LadderBlock || block instanceof DoorBlock) {
                return;
            }

            // Close to camera — always cull
            double pdx = preX - bx;
            double pdy = preY - by;
            double pdz = preZ - bz;
            if (pdx * pdx + pdy * pdy + pdz * pdz < 25) { // < 5^2
                ci.cancel();
                return;
            }

            // Cylinder axis: entity toward camera
            // axisLenSq is camDistSq (same vector)
            if (camDistSq == 0) return;

            double animFactor = 0.1 * Math.min(10, Math.min(
                    cameraEntity.level().getTime() - Mod.startTime + 2,
                    10 - Mod.endTime));
            double radius = animFactor * Config.GSON.instance().cullAngle;

            // Perpendicular distance squared from block center to axis (cross product method)
            // toPoint = blockCenter - entityPos = (bdx, bdy, bdz)
            // axis = cameraPos - entityPos = (cdx, cdy, cdz)
            double cx = bdy * cdz - bdz * cdy;
            double cy = bdz * cdx - bdx * cdz;
            double cz = bdx * cdy - bdy * cdx;
            double perpDistSq = (cx * cx + cy * cy + cz * cz) / camDistSq;

            // Near player: cone with vertex at player, expanding to full radius at 5 blocks along axis
            // projDist = dot(toPoint, axis) / |axis| = how far along the axis the block is
            double dot = bdx * cdx + bdy * cdy + bdz * cdz;
            double axisLen = Math.sqrt(camDistSq);
            double projDist = dot / axisLen;

            // Cone near player: half-angle from config, transitions to cylinder where cone meets full radius
            double tanHalfAngle = Math.tan(Math.toRadians(Config.GSON.instance().coneHalfAngle));
            double coneRadius = projDist * tanHalfAngle;
            double effectiveRadius = Math.min(coneRadius, radius);

            if (perpDistSq <= effectiveRadius * effectiveRadius) {
                ci.cancel();
            }
        } catch (Exception ignored) {
        }
    }

}
