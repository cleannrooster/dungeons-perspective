package com.cleannrooster.dungeons_iso.mixin.compat.sodium;

import com.cleannrooster.dungeons_iso.mod.Mod;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.blaze3d.systems.RenderSystem;
import net.caffeinemc.mods.sodium.client.SodiumClientMod;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSectionManager;
import net.caffeinemc.mods.sodium.client.render.chunk.lists.VisibleChunkCollector;
import net.caffeinemc.mods.sodium.client.render.viewport.Viewport;
import net.caffeinemc.mods.sodium.client.render.viewport.frustum.Frustum;
import net.caffeinemc.mods.sodium.client.render.viewport.frustum.SimpleFrustum;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.joml.FrustumIntersection;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(RenderSectionManager.class)
public abstract class RenderSectionManagerMixin {


    @ModifyReturnValue(at = @At("RETURN"), method = "getEffectiveRenderDistance",remap = false)

    private float getEffectiveRenderDistanceXIV(float flot) {
        if(Mod.enabled) {
            flot =(Mod.getZoom()*Mod.zoomMetric+(4)*16 )* 1.15F;
        }
        return flot;
    }

    @ModifyReturnValue(at = @At("RETURN"), method = "getRenderDistance",remap = false)

    private float getRenderDistanceXIV(float flot) {
        if(Mod.enabled) {
            flot =(Mod.getZoom()*Mod.zoomMetric+(4)*16) * 1.15F;

        }
        return flot;

    }
 @Inject(at = @At("HEAD"), method = "shouldUseOcclusionCulling", cancellable = true,remap = false)

    private void shouldUseOcclusionCullingXIV(Camera camera, boolean spectator,CallbackInfoReturnable<Boolean> cir) {
        if(Mod.enabled&& MinecraftClient.getInstance().player != null && Mod.shouldRebuild()){
            cir.setReturnValue(false);
        }
    }


}
