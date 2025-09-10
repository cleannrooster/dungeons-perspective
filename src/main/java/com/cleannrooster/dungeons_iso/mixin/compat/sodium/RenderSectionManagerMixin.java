package com.cleannrooster.dungeons_iso.mixin.compat.sodium;

import com.cleannrooster.dungeons_iso.mod.Mod;
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
    @Shadow
    private @Nullable BlockPos cameraBlockPos;



    @Inject(at = @At("HEAD"), method = "shouldPrioritizeTask", cancellable = true,remap = false)

    private void shouldPrioritizeTaskXIV(RenderSection section, float distance,CallbackInfoReturnable<Boolean> cir) {
        if(Mod.enabled && cameraBlockPos != null &&  MinecraftClient.getInstance().player != null) {
            cir.setReturnValue(section.getSquaredDistance(cameraBlockPos) < 64*64);

        }
    }
    @ModifyArg(at = @At(value = "INVOKE", target = "findVisible"), method = "createTerrainRenderList", remap = false, index = 1)
    private Viewport adjViewport(Viewport viewport) {
        Vec3d vec = MinecraftClient.getInstance().gameRenderer.getCamera().getPos();
        return new Viewport(new SimpleFrustum(new FrustumIntersection()),new Vector3d(vec.getX(),vec.getY(),vec.getZ()));

    }

    @Shadow
      abstract RenderSection getRenderSection(int x, int y, int z);

}
