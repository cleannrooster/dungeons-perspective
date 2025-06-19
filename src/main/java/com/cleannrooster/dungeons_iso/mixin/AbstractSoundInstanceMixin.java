package com.cleannrooster.dungeons_iso.mixin;

import com.cleannrooster.dungeons_iso.mod.Mod;
import dev.kosmx.playerAnim.core.util.Vec3f;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.sound.AbstractSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractSoundInstance.class)
public class AbstractSoundInstanceMixin {
    @Shadow
    protected double x;
    @Shadow
    protected double y;
    @Shadow

    protected double z;
    @Shadow
    protected boolean relative;

    /*    @Inject(
            method = "getVolume",
            at = @At(value = "RETURN"),
            cancellable = true
    )*/
 /*   public void getVolumeCleann(CallbackInfoReturnable<Float> ci) {
        if(MinecraftClient.getInstance().cameraEntity != null && MinecraftClient.getInstance().cameraEntity.squaredDistanceTo(x,y,z) < 16* 16){
            if(Mod.enabled && ci.getReturnValue() != null){
                ci.setReturnValue(ci.getReturnValue() * Mod.zoom);
            }
        }

    }
  */
    @Inject(
            method = "getVolume",
            at = @At(value = "RETURN"),
            cancellable = true
    )
    public void getVolumeCleann(CallbackInfoReturnable<Float> cir) {
        AbstractSoundInstance instance = (AbstractSoundInstance) (Object) this;

        if(!this.relative && MinecraftClient.getInstance().gameRenderer != null && MinecraftClient.getInstance().gameRenderer.getCamera() != null && Mod.enabled && cir.getReturnValue() != null){
            cir.setReturnValue((float) (cir.getReturnValue()*(1+Mod.zoom)*Math.max(0,1-MinecraftClient.getInstance().player.getPos().distanceTo(new Vec3d(instance.getX(),instance.getY(),instance.getZ()))/16F)));

        }
    }

/*    @Inject(
            method = "getX",
            at = @At(value = "RETURN"),
            cancellable = true
    )
    public void getXCleann(CallbackInfoReturnable<Double> ci) {
        if( MinecraftClient.getInstance().gameRenderer.getCamera() instanceof Camera camera && MinecraftClient.getInstance().cameraEntity instanceof Entity entity    && MinecraftClient.getInstance().cameraEntity.squaredDistanceTo(x,y,z) < 16* 16) {
            if (Mod.enabled && ci.getReturnValue() != null) {
                if(!this.relative) {
                    ci.setReturnValue((double) camera.getPos().getX() + x - entity.getEyePos().getX());

                }
                else{
                    ci.setReturnValue(camera.getPos().getX());
                }

            }
        }
    }
    @Inject(
            method = "getY",
            at = @At(value = "RETURN"),
            cancellable = true
    )
    public void getYCleann(CallbackInfoReturnable<Double> ci) {
        if( MinecraftClient.getInstance().gameRenderer.getCamera() instanceof Camera camera && MinecraftClient.getInstance().cameraEntity instanceof Entity entity    && MinecraftClient.getInstance().cameraEntity.squaredDistanceTo(x,y,z) < 16* 16) {
            if (Mod.enabled && ci.getReturnValue() != null) {
                if(!this.relative) {

                ci.setReturnValue((double)camera.getPos().getY()+y-entity.getEyePos().getY());
                }
                else{
                    ci.setReturnValue(camera.getPos().getY());
                }

            }
        }
    }
    @Inject(
            method = "getZ",
            at = @At(value = "RETURN"),
            cancellable = true
    )
    public void getZCleann(CallbackInfoReturnable<Double> ci) {
        if( MinecraftClient.getInstance().gameRenderer.getCamera() instanceof Camera camera && MinecraftClient.getInstance().cameraEntity instanceof Entity entity    && MinecraftClient.getInstance().cameraEntity.squaredDistanceTo(x,y,z) < 16* 16) {
            if (Mod.enabled && ci.getReturnValue() != null) {
                if(!this.relative) {

                ci.setReturnValue((double)camera.getPos().getZ()+z-entity.getEyePos().getZ());
            }
            else{
                ci.setReturnValue(camera.getPos().getZ());
            }

            }
        }
    }

    @Inject(
            method = "getAttenuationType",
            at = @At(value = "RETURN"),
            cancellable = true
    )
    public void getAttenuationTypeCleann(CallbackInfoReturnable<SoundInstance.AttenuationType> cir) {
        if(Mod.enabled && cir.getReturnValue() != null){
            cir.setReturnValue(SoundInstance.AttenuationType.NONE);

        }
    }*/


}
