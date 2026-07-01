package com.cleannrooster.dungeons_iso.mixin.compat.sodium;

import net.minecraft.world.phys.Vec3;
import com.cleannrooster.dungeons_iso.api.BlockCuller;
import com.cleannrooster.dungeons_iso.api.BlockCullerUser;
import com.cleannrooster.dungeons_iso.api.MinecraftClientAccessor;
import com.cleannrooster.dungeons_iso.api.cullers.FloodCuller;
import com.cleannrooster.dungeons_iso.compat.SodiumCompat;
import com.cleannrooster.dungeons_iso.config.Config;
import com.cleannrooster.dungeons_iso.mod.Mod;
import net.caffeinemc.mods.sodium.api.util.ColorARGB;
import net.caffeinemc.mods.sodium.client.model.light.LightMode;
import net.caffeinemc.mods.sodium.client.model.light.LightPipeline;
import net.caffeinemc.mods.sodium.client.model.light.LightPipelineProvider;
import net.caffeinemc.mods.sodium.client.model.light.data.QuadLightData;
import net.caffeinemc.mods.sodium.client.render.helper.ColorHelper;
import net.caffeinemc.mods.sodium.client.render.model.MutableQuadViewImpl;
import net.caffeinemc.mods.sodium.client.render.model.AbstractBlockRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Map;

import static com.google.common.collect.ImmutableList.of;

@Mixin(AbstractBlockRenderContext.class)
public abstract class AbstractRenderContextMixin implements BlockCullerUser {
    @Shadow    protected BlockPos pos;
    @Shadow
    protected BlockState state;

    @Shadow
    protected LightPipelineProvider lighters;
@Shadow
    protected  QuadLightData quadLightData ;
/*    @Inject(at = @At("HEAD"), method = "shadeQuad", cancellable = true,remap = false)
    protected void shadeQuadXIV(MutableQuadViewImpl quad, LightMode lightMode, boolean emissive, ShadeMode shadeMode, CallbackInfo info) {
        if(Mod.enabled && Config.GSON.instance().fogOfWar && SodiumCompat.fogOfWar != null) {
            for(HitResult result :
                    SodiumCompat.fogOfWar.realPoints){
                if(result.getPos().distanceTo(pos.toCenterPos()) < 4){
                    info.cancel();
                    break;
                }
            }
            return;

        }

    }*/
    @Inject(at = @At("RETURN"), method = "isFaceCulled", cancellable = true)
    protected final void isFaceCulledDungeons(@Nullable Direction direction, CallbackInfoReturnable<Boolean> ci) {
        try {
            VoxelShape selfShape = direction != null ? state.getFaceOcclusionShape(direction) : null;

            if (Minecraft.getInstance() != null && Minecraft.getInstance().player != null && Mod.enabled && !(state.getBlock() instanceof HalfTransparentBlock) && Mod.shouldRebuild()) {
                if (Minecraft.getInstance().getCameraEntity() != null) {
                    boolean bool = Vec3.atCenterOf(pos).y() > Minecraft.getInstance().getCameraEntity().blockPosition().above().getY();
                    boolean boo3 = Vec3.atCenterOf(pos).distanceTo(Mod.preMod) < Mod.getZoom() * Mod.zoomMetric * 1.25F;
                    boolean bool3 = false;

                    if (bool && boo3) {


                    }

                    boolean bool2 = Mod.preMod.subtract(Minecraft.getInstance().getCameraEntity().position()).dot(Vec3.atCenterOf(pos).subtract(Minecraft.getInstance().getCameraEntity().position())) > 0;
                    BlockPos.MutableBlockPos mutable = Minecraft.getInstance().getCameraEntity().blockPosition().mutable();

                    while(mutable.getY() > Minecraft.getInstance().level.getMinY() && !Minecraft.getInstance().level.getBlockState(mutable).blocksMotion()) {
                        mutable.move(Direction.DOWN);
                    }
                    if(pos.getY() > mutable.getY() -8){
                        return;
                    }
                    if(!(state.getBlock() instanceof HalfTransparentBlock)) {

                            ci.setReturnValue((Config.GSON.instance().backCull && direction != null && direction.isFacingAngle(Minecraft.getInstance().gameRenderer.mainCamera().yRot())));

                    }
                    return;
                }


            }
            if(Mod.enabled){
                if(!(state.getBlock() instanceof HalfTransparentBlock)){
                    if((Config.GSON.instance().backCull &&  direction != null && direction.isFacingAngle(Minecraft.getInstance().gameRenderer.mainCamera().yRot()))){

                        ci.setReturnValue((Config.GSON.instance().backCull && direction != null && direction.isFacingAngle(Minecraft.getInstance().gameRenderer.mainCamera().yRot())));
                        return;
                    }

                }


            }
        }
        catch(Exception ignored){

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
