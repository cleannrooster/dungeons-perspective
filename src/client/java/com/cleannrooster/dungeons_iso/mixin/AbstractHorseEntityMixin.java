package com.cleannrooster.dungeons_iso.mixin;

import com.cleannrooster.dungeons_iso.api.MinecraftAccessor;
import com.cleannrooster.dungeons_iso.config.Config;
import com.cleannrooster.dungeons_iso.mod.Mod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractHorse.class)
public abstract class AbstractHorseEntityMixin {

    @Shadow
    protected abstract Vec2 getControlledRotation(LivingEntity controllingPassenger);


    private static Vec3 movementInputToVelocityCleann(Vec3 movementInput, float speed, float yaw) {
        double d = movementInput.lengthSquared();
        if (d < 1.0E-7) {
            return Vec3.ZERO;
        } else {
            Vec3 vec3d = (d > 1.0 ? movementInput.normalize() : movementInput).multiply((double)speed);
            float f = Mth.sin(yaw * 0.017453292F);
            float g = Mth.cos(yaw * 0.017453292F);
            return new Vec3(vec3d.x * (double)g - vec3d.z * (double)f, vec3d.y, vec3d.z * (double)g + vec3d.x * (double)f);
        }
    }
    @Inject(
            method = "getControlledMovementInput",
            at = @At(value = "RETURN"),
            cancellable = true
    )
    protected void getControlledMovementInputCleann(Player controllingPlayer, Vec3 movementInput,
                                                     CallbackInfoReturnable<Vec3> vec3dCallbackInfo) {
        AbstractHorse entity = (AbstractHorse)  (Object) this;
        if (Mod.enabled && controllingPlayer instanceof LocalPlayer player) {

            if ((Config.GSON.instance().turnToMouse && player.input.getMovementInput().length() > 0.1) || ((MinecraftAccessor)Minecraft.getInstance()).getMouseCooldown() >= 0 && player.getVehicle() != null && player.input.getMovementInput().length() > 0.1) {
                float f = entity.sidewaysSpeed ;
                float g = entity.forwardSpeed;
                vec3dCallbackInfo.setReturnValue( new Vec3(f, 0,g).normalize());
            }
        }
    }
    @Inject(
            method = "getControlledRotation",
            at = @At(value = "RETURN"),
            cancellable = true
    )
    protected void getControlledRotationXIV(LivingEntity controllingPassenger, CallbackInfoReturnable<Vec2> vec2fCallbackInfo) {
        AbstractHorse entity = (AbstractHorse)  (Object) this;
        if (Mod.enabled && controllingPassenger instanceof LocalPlayer player) {

            if ((Config.GSON.instance().turnToMouse && player.input.getMovementInput().length() > 0.1) || ((MinecraftAccessor)Minecraft.getInstance()).getMouseCooldown() >= 0 && player.getVehicle() != null && player.input.getMovementInput().length() > 0.1) {
                Vec3 vec3d = player.getDeltaMovement();
                double d = vec3d.x;
                double e = vec3d.y;
                double f = vec3d.z;
                double g = Math.sqrt(d * d + f * f);
                float pitch = (Mth.wrapDegrees((float)(-(Mth.atan2(e, g) * 57.2957763671875)))  );
                float yaw =  (Mth.wrapDegrees((float)(Mth.atan2(f, d) * 57.2957763671875) - 90.0F)  );
                vec2fCallbackInfo.setReturnValue( new Vec2(0,yaw));

            }

        }
    }
}
