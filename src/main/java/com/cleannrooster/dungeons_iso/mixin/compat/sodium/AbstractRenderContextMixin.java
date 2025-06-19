package com.cleannrooster.dungeons_iso.mixin.compat.sodium;

import com.cleannrooster.dungeons_iso.api.BlockCullerUser;
import com.cleannrooster.dungeons_iso.api.MinecraftClientAccessor;
import com.cleannrooster.dungeons_iso.mod.Mod;

import me.jellysquid.mods.sodium.client.model.light.LightPipelineProvider;
import me.jellysquid.mods.sodium.client.model.light.data.QuadLightData;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.AbstractBlockRenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.RaycastContext;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractBlockRenderContext.class)
public abstract class AbstractRenderContextMixin implements BlockCullerUser {

    @Inject(at = @At("RETURN"), method = "isFaceCulled", cancellable = true)
    protected final void isFaceCulledDungeons(@Nullable Direction direction, CallbackInfoReturnable<Boolean> ci) {
        if(MinecraftClient.getInstance() != null && MinecraftClient.getInstance().player != null     && Mod.enabled) {

                    ci.setReturnValue(false);

        }
    }
    @Inject(at = @At("RETURN"), method = "transform", cancellable = true)

    protected void transformDDungeons(MutableQuadView q, CallbackInfoReturnable<Boolean> ci) {
        if(MinecraftClient.getInstance() != null && MinecraftClient.getInstance().player != null     && Mod.enabled) {

            ci.setReturnValue(true);

        }
    }
  /*  @Inject(at = @At("TAIL"), method = "shadeQuad", cancellable = true,remap = false)

    protected void shadeQuadRooster(MutableQuadViewImpl quad, LightMode lightMode, boolean emissive, ShadeMode shadeMode, CallbackInfo info) {
        BlockHitResult result  = MinecraftClient.getInstance().cameraEntity.getWorld().raycast(new RaycastContext(MinecraftClient.getInstance().cameraEntity.getEyePos(), pos.toCenterPos().add(pos.toCenterPos().getX() > MinecraftClient.getInstance().cameraEntity.getX() ? -0.6: 0.6,pos.toCenterPos().getY() > MinecraftClient.getInstance().cameraEntity.getEyeY() ? -0.6: 0.6,pos.toCenterPos().getZ() > MinecraftClient.getInstance().cameraEntity.getZ() ? -0.6: 0.6), RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, MinecraftClient.getInstance().cameraEntity));

        BlockState block  = MinecraftClient.getInstance().cameraEntity.getWorld().getBlockState(result.getBlockPos());
        if((MinecraftClient.getInstance().cameraEntity.getPos().distanceTo(pos.toCenterPos())) < 16 &&  (block != null && !result.getType().equals(HitResult.Type.MISS) && block != state )) {

            float[] brightnesses = this.quadLightData.br;

            for(int i = 0; i < 4; ++i) {
                quad.color(i, ColorARGB.mulRGB(quad.color(i), (float) (Math.max(0,(0.5+0.5*MinecraftClient.getInstance().cameraEntity.getPos().distanceTo(pos.toCenterPos())/16))*brightnesses[i])));
            }
        }

    }*/
}
