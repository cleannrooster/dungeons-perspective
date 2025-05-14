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
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractBlockRenderContext.class)
public abstract class MixinBlockRenderer  {
    @Shadow    protected BlockPos pos;

    @Inject(at = @At("HEAD"), method = "isFaceCulled", cancellable = true)
    protected final void isFaceCulledDungeons(@Nullable Direction direction, CallbackInfoReturnable<Boolean> ci) {
        if(MinecraftClient.getInstance() != null && MinecraftClient.getInstance().player instanceof ClientPlayerEntity player && Mod.enabled && ((MinecraftClientAccessor)MinecraftClient.getInstance()).shouldRebuild()) {
            Box box = new Box(player.getEyePos(),MinecraftClient.getInstance().gameRenderer.getCamera().getPos());

            /*if((pos.getX() < box.maxX && pos.getX() > box.minX-1)
                    && (pos.getY() < box.maxY && pos.getY() > box.minY)
                    && (pos.getZ() < box.maxZ && pos.getZ() > box.minZ-1)
            ){*/
            if(MinecraftClient.getInstance().gameRenderer.getCamera() instanceof Camera camera  &&
                    ((((Math.abs(MinecraftClient.getInstance().cameraEntity.getEyePos().subtract(camera.getPos()).normalize().dotProduct(pos.toCenterPos().subtract( camera.getPos()).normalize())) > 0.90 || camera.getPos().distanceTo(pos.toCenterPos()) < 3) &&
                            (MinecraftClient.getInstance().cameraEntity.getEyePos().subtract((pos.toCenterPos())).normalize().dotProduct((MinecraftClient.getInstance().cameraEntity.getEyePos()).subtract(camera.getPos()).normalize()) > 0.7071 || camera.getPos().distanceTo(pos.toCenterPos()) < 3)))||
                            (pos.toCenterPos().y > player.getPos().y+8))
                //(MinecraftClient.getInstance().cameraEntity.getEyePos().subtract((blockInfo.blockPos.toCenterPos())).normalize().dotProduct((MinecraftClient.getInstance().cameraEntity.getEyePos()).subtract(camera.getPos()).normalize()) > 0.7071 || camera.getPos().distanceTo(blockInfo.blockPos.toCenterPos()) < 3)



                    /*&& (blockInfo.blockPos.getX()<=(int)box.maxX-1 && blockInfo.blockPos.getX() >=(int) box.minX-1)
                    && (blockInfo.blockPos.getY() <= (int)box.maxY && blockInfo.blockPos.getY() >=(int) box.minY)
                    && (blockInfo.blockPos.getZ() <= (int)box.maxZ && blockInfo.blockPos.getZ() >=(int) box.minZ)*/
            ){
                ci.setReturnValue(true);
                return;
            }
            else{
                ci.setReturnValue(false);
            }

          /*  if(!(player.getWorld().raycast(new RaycastContext(
                    MinecraftClient.getInstance().gameRenderer.getCamera().getPos(),
                    pos.toCenterPos(),
                    RaycastContext.ShapeType.OUTLINE,
                    RaycastContext.FluidHandling.NONE,
                    player
            ))).getType().equals(HitResult.Type.MISS)) {
                ci.cancel();
            }*/
        }
    }
}
