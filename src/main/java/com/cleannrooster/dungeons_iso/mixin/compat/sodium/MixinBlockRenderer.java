package com.cleannrooster.dungeons_iso.mixin.compat.sodium;

import com.cleannrooster.dungeons_iso.api.BlockCuller;
import com.cleannrooster.dungeons_iso.api.MinecraftClientAccessor;
import com.cleannrooster.dungeons_iso.api.cullers.FloodCuller;
import com.cleannrooster.dungeons_iso.compat.SodiumCompat;
import com.cleannrooster.dungeons_iso.mod.Mod;


import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderContext;
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.AbstractBlockRenderContext;
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
import net.minecraft.util.math.*;
import net.minecraft.world.RaycastContext;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.awt.*;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Optional;

@Mixin(BlockRenderer.class)
public abstract class MixinBlockRenderer  {


    @Inject(at = @At("HEAD"), method = "isFaceVisible", cancellable = true,remap = false)

    private void isFaceVisibleDungeons(BlockRenderContext ctx, Direction face, CallbackInfoReturnable<Boolean> ci) {
        if (Mod.enabled) {
            ci.setReturnValue(true);
        }

    }
    @Inject(at = @At("HEAD"), method = "renderModel", cancellable = true,remap = false)

    public void renderModel(BlockRenderContext ctx, ChunkBuildBuffers buffers, CallbackInfo ci) {
        if(Mod.enabled ) {
            if (SodiumCompat.detector.shouldCull(ctx.pos(), MinecraftClient.getInstance().gameRenderer.getCamera(), MinecraftClient.getInstance().cameraEntity)) {
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
                if(culler instanceof FloodCuller floodCuller &&((culler.shouldForceCull() && !bool) || (bool && culler.shouldForceNonCull()))){
                    bool = floodCuller.isAboveFlood(ctx.pos(), MinecraftClient.getInstance().gameRenderer.getCamera(), MinecraftClient.getInstance().cameraEntity, SodiumCompat.stream.stream());
                }
                else if ((culler.shouldForceCull() && !bool) || (bool && culler.shouldForceNonCull())) {
                    bool = culler.shouldCull(ctx.pos(), MinecraftClient.getInstance().gameRenderer.getCamera(), MinecraftClient.getInstance().cameraEntity);
                }
            }
            if (bool) {

                ci.cancel();
                return;
            }
        }

    }

}
