package com.cleannrooster.dungeons_iso.mixin.compat.sodium;

import com.cleannrooster.dungeons_iso.api.MinecraftClientAccessor;
import com.cleannrooster.dungeons_iso.mod.Mod;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import net.caffeinemc.mods.sodium.client.render.frapi.render.AbstractBlockRenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockRenderer.class)
public abstract class MixinBlockRenderer extends AbstractBlockRenderContext {
    @Inject(at = @At("HEAD"), method = "renderModel", cancellable = true)
    public void cleann$cancelRendering(BakedModel model, BlockState state, BlockPos pos, BlockPos origin, CallbackInfo ci) {
        if(MinecraftClient.getInstance() != null && MinecraftClient.getInstance().player instanceof ClientPlayerEntity player && Mod.enabled && ((MinecraftClientAccessor)MinecraftClient.getInstance()).shouldRebuild()) {
            Box box = new Box(player.getEyePos(),MinecraftClient.getInstance().gameRenderer.getCamera().getPos());

            /*if((pos.getX() < box.maxX && pos.getX() > box.minX-1)
                    && (pos.getY() < box.maxY && pos.getY() > box.minY)
                    && (pos.getZ() < box.maxZ && pos.getZ() > box.minZ-1)
            ){*/
            if(MinecraftClient.getInstance().gameRenderer.getCamera() instanceof Camera camera &&pos.toCenterPos().y > player.getEyePos().y &&
                    (Math.abs(MinecraftClient.getInstance().cameraEntity.getEyePos().subtract(camera.getPos()).normalize().dotProduct(pos.toCenterPos().subtract( camera.getPos()).normalize())) > 0.90 || camera.getPos().distanceTo(pos.toCenterPos()) < 3) &&
                    (MinecraftClient.getInstance().cameraEntity.getEyePos().subtract((pos.toCenterPos())).normalize().dotProduct((MinecraftClient.getInstance().cameraEntity.getEyePos()).subtract(camera.getPos()).normalize()) > 0.7071 || camera.getPos().distanceTo(pos.toCenterPos()) < 3)
/*
                    && (pos.getX()<= (int)box.maxX-1 && pos.getX() >= (int)box.minX-1)
                        && (pos.getY()<= (int)box.maxY && pos.getY() >= (int)box.minY)
                        && (pos.getZ()<= (int)box.maxZ&& pos.getZ() >= (int)box.minZ)

                 */){

                    ci.cancel();
            }
        }
    }
}
