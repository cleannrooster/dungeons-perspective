package com.cleannrooster.dungeons_iso.mixin.compat.sodium;

import com.cleannrooster.dungeons_iso.api.BlockCullerUser;
import com.cleannrooster.dungeons_iso.api.MinecraftClientAccessor;
import com.cleannrooster.dungeons_iso.config.Config;
import com.cleannrooster.dungeons_iso.mod.Mod;
import net.caffeinemc.mods.sodium.api.util.ColorARGB;
import net.caffeinemc.mods.sodium.client.model.light.LightMode;
import net.caffeinemc.mods.sodium.client.model.light.LightPipeline;
import net.caffeinemc.mods.sodium.client.model.light.LightPipelineProvider;
import net.caffeinemc.mods.sodium.client.model.light.data.QuadLightData;
import net.caffeinemc.mods.sodium.client.render.frapi.helper.ColorHelper;
import net.caffeinemc.mods.sodium.client.render.frapi.mesh.MutableQuadViewImpl;
import net.caffeinemc.mods.sodium.client.render.frapi.render.AbstractBlockRenderContext;
import net.caffeinemc.mods.sodium.fabric.block.FabricBlockAccess;
import net.fabricmc.fabric.api.renderer.v1.material.ShadeMode;
import net.minecraft.block.BlockState;
import net.minecraft.block.IceBlock;
import net.minecraft.block.TranslucentBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.screen.EnchantmentScreenHandler;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractBlockRenderContext.class)
public abstract class AbstractRenderContextMixin implements BlockCullerUser {
    @Shadow    protected BlockPos pos;
    @Shadow
    protected BlockState state;

    @Shadow
    protected LightPipelineProvider lighters;
@Shadow
    protected  QuadLightData quadLightData ;

    @Inject(at = @At("RETURN"), method = "isFaceCulled", cancellable = true)
    protected final void isFaceCulledDungeons(@Nullable Direction direction, CallbackInfoReturnable<Boolean> ci) {
        if(MinecraftClient.getInstance() != null && MinecraftClient.getInstance().player != null && Mod.enabled && !(state.getBlock() instanceof TranslucentBlock) && Mod.shouldReload) {
                    if( direction != null && MinecraftClient.getInstance().cameraEntity != null){
                        VoxelShape selfShape = state.getCullingFace(MinecraftClient.getInstance().world, pos, direction);
                        boolean bool = pos.toCenterPos().getY() > MinecraftClient.getInstance().cameraEntity.getEyeY();
                        boolean boo3 = pos.toCenterPos().distanceTo(Mod.preMod) < Mod.getZoom()*Mod.zoomMetric;
                        boolean bool2 = MinecraftClient.getInstance().gameRenderer.getCamera().getPos().subtract(MinecraftClient.getInstance().cameraEntity.getPos()).dotProduct(pos.toCenterPos().subtract(MinecraftClient.getInstance().cameraEntity.getPos())) >0 ;
                        ci.setReturnValue(selfShape.isEmpty() || (bool2 && bool && boo3 )  );

                    }



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
