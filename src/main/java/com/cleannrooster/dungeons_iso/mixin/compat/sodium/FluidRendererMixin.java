package com.cleannrooster.dungeons_iso.mixin.compat.sodium;

import com.cleannrooster.dungeons_iso.api.MinecraftClientAccessor;
import com.cleannrooster.dungeons_iso.mod.Mod;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.TranslucentGeometryCollector;
import net.caffeinemc.mods.sodium.client.render.frapi.render.AbstractBlockRenderContext;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.caffeinemc.mods.sodium.fabric.render.FluidRendererImpl;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static java.lang.Math.sqrt;

@Mixin(FluidRendererImpl.class)
public abstract class FluidRendererMixin {

    @Inject(at = @At("HEAD"), method = "render", cancellable = true)
    public void renderXIV(LevelSlice level, BlockState blockState, FluidState fluidState, BlockPos pos, BlockPos offset, TranslucentGeometryCollector collector, ChunkBuildBuffers buffers, CallbackInfo ci) {
        if(MinecraftClient.getInstance() != null && MinecraftClient.getInstance().player instanceof ClientPlayerEntity player && Mod.enabled && ((MinecraftClientAccessor)MinecraftClient.getInstance()).shouldRebuild()) {
            Box box = new Box(player.getEyePos(),MinecraftClient.getInstance().gameRenderer.getCamera().getPos());

            /*if((pos.getX() < box.maxX && pos.getX() > box.minX-1)
                    && (pos.getY() < box.maxY && pos.getY() > box.minY)
                    && (pos.getZ() < box.maxZ && pos.getZ() > box.minZ-1)
            ){*/
            if(MinecraftClient.getInstance().gameRenderer.getCamera() instanceof Camera camera  &&(

              /*               (((float)MinecraftClient.getInstance().cameraEntity.getEyePos().subtract((pos.toCenterPos())).normalize().dotProduct((MinecraftClient.getInstance().cameraEntity.getEyePos()).subtract(camera.getPos()).normalize()) > 0.71F || camera.getPos().distanceTo(pos.toCenterPos()) < 3) &&

                             (Math.abs((float)MinecraftClient.getInstance().cameraEntity.getEyePos().subtract(camera.getPos()).normalize().dotProduct(pos.toCenterPos().subtract( camera.getPos()).normalize())) >= 0.9F || camera.getPos().distanceTo(pos.toCenterPos()) < 3)) &&
                             ((float)MinecraftClient.getInstance().cameraEntity.getPos().subtract(pos.toCenterPos()).normalize().dotProduct(new Vec3d(0,-1,0)) > 0.3F)))  ||
*/
/*
                            Objects.equals(pos, BlockPos.ofFloored(player.getX(), (int) player.getBoundingBox().getMax(Direction.Axis.Y), player.getZ())) || Objects.equals(pos, BlockPos.ofFloored(player.getX(), (int) player.getBoundingBox().getMax(Direction.Axis.Y)+1, player.getZ())) ||
*/
                    ((int)Math.abs(MinecraftClient.getInstance().cameraEntity.getPos().subtract((pos.toCenterPos())).normalize().multiply(1,1,1).dotProduct((MinecraftClient.getInstance().cameraEntity.getPos()).subtract(camera.getPos()).normalize().multiply(8,8,8))) >= (int)(0.8*8) && pos.toCenterPos().getY() > player.getY()+1)

/*            ((MinecraftClient.getInstance().cameraEntity.getPos().subtract((pos.toCenterPos())).normalize().dotProduct((MinecraftClient.getInstance().cameraEntity.getPos()).subtract(camera.getPos()).normalize()) > 0.5 && pos.toCenterPos().getY() > player.getY()+1) )*/

                                     //(MinecraftClient.getInstance().cameraEntity.getEyePos().subtract((blockInfo.blockPos.toCenterPos())).normalize().dotProduct((MinecraftClient.getInstance().cameraEntity.getEyePos()).subtract(camera.getPos()).normalize()) > 0.7071 || camera.getPos().distanceTo(blockInfo.blockPos.toCenterPos()) < 3)



                    /*&& (blockInfo.blockPos.getX()<=(int)box.maxX-1 && blockInfo.blockPos.getX() >=(int) box.minX-1)
                    && (blockInfo.blockPos.getY() <= (int)box.maxY && blockInfo.blockPos.getY() >=(int) box.minY)
                    && (blockInfo.blockPos.getZ() <= (int)box.maxZ && blockInfo.blockPos.getZ() >=(int) box.minZ)*/
            )){
                ci.cancel();
                return;
            }

          /*  if().getType().equals(HitResult.Type.MISS)) {
                ci.cancel();
            }*/
        }
    }
}
