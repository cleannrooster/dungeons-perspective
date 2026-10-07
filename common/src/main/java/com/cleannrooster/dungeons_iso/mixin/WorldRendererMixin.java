package com.cleannrooster.dungeons_iso.mixin;

import com.cleannrooster.dungeons_iso.ClientInit;
import com.cleannrooster.dungeons_iso.config.Config;
import com.cleannrooster.dungeons_iso.api.cullers.room.GhostRenderer;
import com.cleannrooster.dungeons_iso.api.cullers.room.CullingBackdrop;
import com.cleannrooster.dungeons_iso.mod.Mod;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Draws a distinct outline around the contextual interactable target ({@link Mod#targetedInteractable}).
 *
 * Piggybacks on vanilla's world-outline pass: injected at the debug-renderer call in
 * {@link WorldRenderer#render}, where the camera-relative {@link MatrixStack} and the line
 * {@link VertexConsumerProvider.Immediate} are live and not yet flushed. The gold outline is
 * visibly different from vanilla's black block outline, so the contextual target is easy to tell
 * apart from the raw mouse target. It appears only while Dungeons Perspective is enabled and
 * vanishes the instant the target becomes invalid.
 */
@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {

    @Inject(method = "renderSky", at = @At("HEAD"), cancellable = true)
    private void hideSkyBehindCulledCaves(Matrix4f positionMatrix, Matrix4f projectionMatrix,
                                          float tickDelta, Camera camera, boolean thickFog,
                                          Runnable fogCallback, CallbackInfo ci) {
        if (CullingBackdrop.hidesSky()) {
            // Do not depend on an earlier framebuffer clear retaining BackgroundRenderer's colour:
            // loader hooks and renderer replacements can move that work around. The sky stage is
            // before terrain, so explicitly replacing only the colour buffer here is deterministic
            // and cannot erase terrain or depth that has already been drawn.
            RenderSystem.clearColor(CullingBackdrop.red(), CullingBackdrop.green(),
                    CullingBackdrop.blue(), 1.0F);
            RenderSystem.clear(0x4000, MinecraftClient.IS_SYSTEM_MAC);
            ci.cancel();
        }
    }

    @Inject(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/debug/DebugRenderer;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;DDD)V"
            )
    )
    private void drawWorldOverlaysXIV(RenderTickCounter tickCounter, boolean renderBlockOutline,
                                  Camera camera, GameRenderer gameRenderer,
                                  LightmapTextureManager lightmapTextureManager,
                                  Matrix4f matrix4f, Matrix4f matrix4f2, CallbackInfo ci,
                                  @Local MatrixStack matrices,
                                  @Local VertexConsumerProvider.Immediate buffers) {
        try {
            GhostRenderer.render(matrices, buffers, camera, gameRenderer);
        } catch (Exception ignored) {
        }
        if (!Mod.enabled || !Config.GSON.instance().isContextualInteract()) {
            return;
        }
        BlockPos pos = Mod.targetedInteractable;
        if (pos == null) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) {
            return;
        }
        World world = client.world;
        BlockState state = world.getBlockState(pos);
        if (state.isAir()) {
            return;
        }
        VoxelShape shape = state.getOutlineShape(world, pos, ShapeContext.of(camera.getFocusedEntity()));
        if (shape.isEmpty()) {
            return;
        }
        Vec3d cam = camera.getPos();
        VertexConsumer lines = buffers.getBuffer(RenderLayer.getLines());
        // Gold, higher alpha than vanilla's black 0.4 outline → clearly distinct from the mouse target.
        ClientInit.drawCuboidShapeOutline(matrices, lines, shape,
                pos.getX() - cam.x, pos.getY() - cam.y, pos.getZ() - cam.z,
                1.0F, 0.82F, 0.15F, 0.9F);
    }
}
