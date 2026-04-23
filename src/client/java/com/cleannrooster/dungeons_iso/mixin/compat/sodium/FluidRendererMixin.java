package com.cleannrooster.dungeons_iso.mixin.compat.sodium;

import net.caffeinemc.mods.sodium.fabric.render.FluidRendererImpl;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(FluidRendererImpl.class)
public abstract class FluidRendererMixin {

   /* @Inject(at = @At("HEAD"), method = "render", cancellable = true)
    public void renderXIV(LevelSlice level, BlockState blockState, FluidState fluidState, BlockPos pos, BlockPos offset, TranslucentGeometryCollector collector, ChunkBuildBuffers buffers, CallbackInfo ci) {
        if(Minecraft.getInstance() != null && Minecraft.getInstance().player instanceof LocalPlayer player && Mod.enabled && ((MinecraftAccessor)Minecraft.getInstance()).shouldRebuild()) {
            Box box = new Box(player.getEyePos(),Minecraft.getInstance().gameRenderer.getMainCamera().getPos());

            *//*if((pos.getX() < box.maxX && pos.getX() > box.minX-1)
                    && (pos.getY() < box.maxY && pos.getY() > box.minY)
                    && (pos.getZ() < box.maxZ && pos.getZ() > box.minZ-1)
            ){*//*
            if(Minecraft.getInstance().gameRenderer.getMainCamera() instanceof Camera camera  &&(

              *//*               (((float)Minecraft.getInstance().cameraEntity.getEyePos().subtract((pos.toCenterPos())).normalize().dotProduct((Minecraft.getInstance().cameraEntity.getEyePos()).subtract(camera.getPos()).normalize()) > 0.71F || camera.getPos().distanceTo(pos.toCenterPos()) < 3) &&

                             (Math.abs((float)Minecraft.getInstance().cameraEntity.getEyePos().subtract(camera.getPos()).normalize().dotProduct(pos.toCenterPos().subtract( camera.getPos()).normalize())) >= 0.9F || camera.getPos().distanceTo(pos.toCenterPos()) < 3)) &&
                             ((float)Minecraft.getInstance().cameraEntity.getPos().subtract(pos.toCenterPos()).normalize().dotProduct(new Vec3(0,-1,0)) > 0.3F)))  ||
*//*
*//*
                            Objects.equals(pos, BlockPos.ofFloored(player.getX(), (int) player.getBoundingBox().getMax(Direction.Axis.Y), player.getZ())) || Objects.equals(pos, BlockPos.ofFloored(player.getX(), (int) player.getBoundingBox().getMax(Direction.Axis.Y)+1, player.getZ())) ||
*//*
                    ((int)Math.abs(Minecraft.getInstance().cameraEntity.getPos().subtract((pos.toCenterPos())).normalize().multiply(1,1,1).dotProduct((Minecraft.getInstance().cameraEntity.getPos()).subtract(camera.getPos()).normalize().multiply(8,8,8))) >= (int)(0.8*8) && pos.toCenterPos().getY() > player.getY()+1)

*//*            ((Minecraft.getInstance().cameraEntity.getPos().subtract((pos.toCenterPos())).normalize().dotProduct((Minecraft.getInstance().cameraEntity.getPos()).subtract(camera.getPos()).normalize()) > 0.5 && pos.toCenterPos().getY() > player.getY()+1) )*//*

                                     //(Minecraft.getInstance().cameraEntity.getEyePos().subtract((blockInfo.blockPos.toCenterPos())).normalize().dotProduct((Minecraft.getInstance().cameraEntity.getEyePos()).subtract(camera.getPos()).normalize()) > 0.7071 || camera.getPos().distanceTo(blockInfo.blockPos.toCenterPos()) < 3)



                    *//*&& (blockInfo.blockPos.getX()<=(int)box.maxX-1 && blockInfo.blockPos.getX() >=(int) box.minX-1)
                    && (blockInfo.blockPos.getY() <= (int)box.maxY && blockInfo.blockPos.getY() >=(int) box.minY)
                    && (blockInfo.blockPos.getZ() <= (int)box.maxZ && blockInfo.blockPos.getZ() >=(int) box.minZ)*//*
            )){
                ci.cancel();
                return;
            }

          *//*  if().getType().equals(HitResult.Type.MISS)) {
                ci.cancel();
            }*//*
        }
    }*/
}
