package com.cleannrooster.dungeons_iso.mixin;

import com.cleannrooster.dungeons_iso.api.MinecraftClientAccessor;
import com.cleannrooster.dungeons_iso.mod.Mod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.sound.AbstractSoundInstance;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractHorseEntity.class)
public abstract class AbstractHorseEntityMixin {

    @Shadow
    protected abstract Vec2f getControlledRotation(LivingEntity controllingPassenger);


    private static Vec3d movementInputToVelocityCleann(Vec3d movementInput, float speed, float yaw) {
        double d = movementInput.lengthSquared();
        if (d < 1.0E-7) {
            return Vec3d.ZERO;
        } else {
            Vec3d vec3d = (d > 1.0 ? movementInput.normalize() : movementInput).multiply((double)speed);
            float f = MathHelper.sin(yaw * 0.017453292F);
            float g = MathHelper.cos(yaw * 0.017453292F);
            return new Vec3d(vec3d.x * (double)g - vec3d.z * (double)f, vec3d.y, vec3d.z * (double)g + vec3d.x * (double)f);
        }
    }
    @Inject(
            method = "getControlledMovementInput",
            at = @At(value = "RETURN"),
            cancellable = true
    )
    protected void getControlledMovementInputCleann(PlayerEntity controllingPlayer, Vec3d movementInput,
                                                     CallbackInfoReturnable<Vec3d> vec3dCallbackInfo) {
        AbstractHorseEntity entity = (AbstractHorseEntity)  (Object) this;
        if (Mod.enabled && controllingPlayer instanceof ClientPlayerEntity player) {

            if (((MinecraftClientAccessor)MinecraftClient.getInstance()).getMouseCooldown() > 0 && player.getVehicle() != null && player.input.getMovementInput().length() > 0.1) {
                float f = entity.sidewaysSpeed ;
                float g = entity.forwardSpeed;
                vec3dCallbackInfo.setReturnValue( new Vec3d(f, 0,g).normalize());
            }
        }
    }
    @Inject(
            method = "getControlledRotation",
            at = @At(value = "RETURN"),
            cancellable = true
    )
    protected void getControlledRotationXIV(LivingEntity controllingPassenger, CallbackInfoReturnable<Vec2f> vec2fCallbackInfo) {
        AbstractHorseEntity entity = (AbstractHorseEntity)  (Object) this;
        if (Mod.enabled && controllingPassenger instanceof ClientPlayerEntity player) {

            if (((MinecraftClientAccessor)MinecraftClient.getInstance()).getMouseCooldown() > 0 && player.getVehicle() != null && player.input.getMovementInput().length() > 0.1) {
                Vec3d vec3d = movementInputToVelocityCleann(new Vec3d(player.input.movementSideways, 0, player.input.movementForward), 1.0F, player.getYaw());
                double d = vec3d.x;
                double e = vec3d.y;
                double f = vec3d.z;
                double g = Math.sqrt(d * d + f * f);
                float pitch = (MathHelper.wrapDegrees((float)(-(MathHelper.atan2(e, g) * 57.2957763671875)))  );
                float yaw =  (MathHelper.wrapDegrees((float)(MathHelper.atan2(f, d) * 57.2957763671875) - 90.0F)  );

                vec2fCallbackInfo.setReturnValue( new Vec2f(pitch, yaw));
            }
        }
    }
}
