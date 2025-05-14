package com.cleannrooster.dungeons_iso.mixin;

import com.cleannrooster.dungeons_iso.mod.Mod;
import dev.kosmx.playerAnim.core.util.Vec3f;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.sound.AbstractSoundInstance;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractSoundInstance.class)
public class AbstractSoundInstanceMixin {
    @Shadow
    protected double x;
    @Shadow
    protected double y;
    @Shadow

    protected double z;
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
            method = "getX",
            at = @At(value = "RETURN"),
            cancellable = true
    )
    public void getXCleann(CallbackInfoReturnable<Double> ci) {
        if( MinecraftClient.getInstance().gameRenderer.getCamera() instanceof Camera camera && MinecraftClient.getInstance().cameraEntity instanceof Entity entity    && MinecraftClient.getInstance().cameraEntity.squaredDistanceTo(x,y,z) < 16* 16) {
            if (Mod.enabled && ci.getReturnValue() != null) {

                ci.setReturnValue((double)camera.getPos().getX()+x-entity.getEyePos().getX());


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

                ci.setReturnValue((double)camera.getPos().getY()+y-entity.getEyePos().getY());


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

                ci.setReturnValue((double)camera.getPos().getZ()+z-entity.getEyePos().getZ());


            }
        }
    }

}
