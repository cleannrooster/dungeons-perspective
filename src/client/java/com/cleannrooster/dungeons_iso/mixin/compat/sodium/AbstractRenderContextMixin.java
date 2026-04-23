package com.cleannrooster.dungeons_iso.mixin.compat.sodium;

import com.cleannrooster.dungeons_iso.api.BlockCullerUser;
import com.cleannrooster.dungeons_iso.config.Config;
import com.cleannrooster.dungeons_iso.mod.Mod;
import net.caffeinemc.mods.sodium.client.model.light.LightPipelineProvider;
import net.caffeinemc.mods.sodium.client.model.light.data.QuadLightData;
import net.caffeinemc.mods.sodium.client.render.model.MutableQuadViewImpl;
import net.caffeinemc.mods.sodium.client.render.model.AbstractBlockRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.TranslucentBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(AbstractBlockRenderContext.class)
public abstract class AbstractRenderContextMixin implements BlockCullerUser {
    @Shadow    protected BlockPos pos;
    @Shadow
    protected BlockState state;
@Shadow
private  MutableQuadViewImpl editorQuad;
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
            VoxelShape selfShape = direction != null ? state.getCullingFace(Minecraft.getInstance().level, pos, direction) : null;

            if (Minecraft.getInstance() != null && Minecraft.getInstance().player != null && Mod.enabled && !(state.getBlock() instanceof TranslucentBlock) && Mod.shouldRebuild()) {
                if (Minecraft.getInstance().cameraEntity != null) {
                    boolean bool = pos.toCenterPos().getY() > Minecraft.getInstance().cameraEntity.getBlockPos().up().getY();
                    boolean boo3 = pos.toCenterPos().distanceTo(Mod.preMod) < Mod.getZoom() * Mod.zoomMetric * 1.25F;
                    boolean bool3 = false;

                    if (bool && boo3) {


                    }

                    boolean bool2 = Mod.preMod.subtract(Minecraft.getInstance().cameraEntity.getPos()).dotProduct(pos.toCenterPos().subtract(Minecraft.getInstance().cameraEntity.getPos())) > 0;
                    BlockPos.Mutable mutable = Minecraft.getInstance().cameraEntity.getBlockPos().mutableCopy();

                    while(mutable.getY() > Minecraft.getInstance().level.getMinBuildHeight() && !Minecraft.getInstance().level.getBlockState(mutable).blocksMotion()) {
                        mutable.move(Direction.DOWN);
                    }
                    if(pos.getY() > mutable.getY() -8){
                        return;
                    }
                    if(!(state.getBlock() instanceof TranslucentBlock)) {

                            ci.setReturnValue((Config.GSON.instance().backCull && direction != null && direction.pointsTo(Minecraft.getInstance().gameRenderer.getMainCamera().yRot())));

                    }
                    return;
                }


            }
            if(Mod.enabled){
                if(!(state.getBlock() instanceof TranslucentBlock)){
                    if((Config.GSON.instance().backCull &&  direction != null && direction.pointsTo(Minecraft.getInstance().gameRenderer.getMainCamera().yRot()))){

                        ci.setReturnValue((Config.GSON.instance().backCull && direction != null && direction.pointsTo(Minecraft.getInstance().gameRenderer.getMainCamera().yRot())));
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
        BlockHitResult result  = Minecraft.getInstance().cameraEntity.getWorld().raycast(new ClipContext(Minecraft.getInstance().cameraEntity.getEyePos(), pos.toCenterPos().add(pos.toCenterPos().getX() > Minecraft.getInstance().cameraEntity.getX() ? -0.6: 0.6,pos.toCenterPos().getY() > Minecraft.getInstance().cameraEntity.getEyeY() ? -0.6: 0.6,pos.toCenterPos().getZ() > Minecraft.getInstance().cameraEntity.getZ() ? -0.6: 0.6), ClipContext.ShapeType.COLLIDER, ClipContext.FluidHandling.NONE, Minecraft.getInstance().cameraEntity));

        BlockState block  = Minecraft.getInstance().cameraEntity.getWorld().getBlockState(result.getBlockPos());
        if((Minecraft.getInstance().cameraEntity.getPos().distanceTo(pos.toCenterPos())) < 16 &&  (block != null && !result.getType().equals(HitResult.Type.MISS) && block != state )) {

            float[] brightnesses = this.quadLightData.br;

            for(int i = 0; i < 4; ++i) {
                quad.color(i, ColorARGB.mulRGB(quad.color(i), (float) (Math.max(0,(0.5+0.5*Minecraft.getInstance().cameraEntity.getPos().distanceTo(pos.toCenterPos())/16))*brightnesses[i])));
            }
        }

    }*/
}
