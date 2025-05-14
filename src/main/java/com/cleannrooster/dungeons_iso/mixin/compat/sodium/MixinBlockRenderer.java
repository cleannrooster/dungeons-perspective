package com.cleannrooster.dungeons_iso.mixin.compat.sodium;

import com.cleannrooster.dungeons_iso.api.MinecraftClientAccessor;
import com.cleannrooster.dungeons_iso.mod.Mod;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.TranslucentGeometryCollector;
import net.caffeinemc.mods.sodium.client.render.frapi.render.AbstractBlockRenderContext;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.windows.POINT;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;

@Mixin(AbstractBlockRenderContext.class)
public abstract class MixinBlockRenderer  {
    @Shadow    protected BlockPos pos;

    @Inject(at = @At("HEAD"), method = "isFaceCulled", cancellable = true)
    protected final void isFaceCulledDungeons(@Nullable Direction direction, CallbackInfoReturnable<Boolean> ci) {
        if (MinecraftClient.getInstance() != null && MinecraftClient.getInstance().player instanceof ClientPlayerEntity player && Mod.enabled && ((MinecraftClientAccessor) MinecraftClient.getInstance()).shouldRebuild()) {
            Box box = new Box(player.getEyePos(), MinecraftClient.getInstance().gameRenderer.getCamera().getPos());

            /*if((pos.getX() < box.maxX && pos.getX() > box.minX-1)
                    && (pos.getY() < box.maxY && pos.getY() > box.minY)
                    && (pos.getZ() < box.maxZ && pos.getZ() > box.minZ-1)
            ){*/
            if (MinecraftClient.getInstance().gameRenderer.getCamera() instanceof Camera camera ) {
                //if block is within 150 degree cone above player
                boolean bl1 = (MinecraftClient.getInstance().cameraEntity.getPos().subtract(pos.toCenterPos()).normalize().dotProduct(new Vec3d(0, -1, 0)) > 0.2899);
                //if player is within 30 degrees of block and camera
                boolean bl2 = (Math.abs(MinecraftClient.getInstance().cameraEntity.getEyePos().subtract(camera.getPos()).normalize().dotProduct(pos.toCenterPos().subtract(camera.getPos()).normalize())) > 0.90 );
                //if block is within 90 degrees of player and camera
                boolean bl3 = ((MinecraftClient.getInstance().cameraEntity.getBoundingBox().getCenter().subtract((pos.toCenterPos())).normalize().dotProduct((MinecraftClient.getInstance().cameraEntity.getBoundingBox().getCenter())
                        .subtract(camera.getPos()).normalize()) > 0.7071 ));

                boolean bl4 = MinecraftClient.getInstance().cameraEntity.squaredDistanceTo(pos.toCenterPos()) < 16F;
                //(MinecraftClient.getInstance().cameraEntity.getEyePos().subtract((blockInfo.blockPos.toCenterPos())).normalize().dotProduct((MinecraftClient.getInstance().cameraEntity.getEyePos()).subtract(camera.getPos()).normalize()) > 0.7071 || camera.getPos().distanceTo(blockInfo.blockPos.toCenterPos()) < 3)
                //clears blocks in front of the player towards the camera in a 90 degree cone of length 4
                boolean bl5 = bl3 && bl4;
                if (bl5 || (bl1 && bl2 && bl3) || pos.isWithinDistance(camera.getPos(),4F)) {
                    ci.setReturnValue(true);
                    return;
                }
                if (direction != Direction.DOWN) {
                    ci.setReturnValue(false);
                }


          /*  if().getType().equals(HitResult.Type.MISS)) {
                ci.cancel();
            }*/
            }
        }
    }
}
