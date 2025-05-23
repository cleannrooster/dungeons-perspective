package com.cleannrooster.dungeons_iso.mixin;

import com.cleannrooster.dungeons_iso.api.MinecraftClientAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.spell_engine.client.SpellEngineClient;
import org.joml.Matrix2f;
import org.joml.Vector2f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.cleannrooster.dungeons_iso.config.Config;
import com.cleannrooster.dungeons_iso.mod.Mod;

@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin extends Input {

    @Inject(
            method = "tick", at = @At("TAIL")
    )
    private void movementXIV(boolean slowDown, float slowDownFactor, CallbackInfo ci) {
        if (Mod.enabled ) {
            MinecraftClient client = MinecraftClient.getInstance();
            assert client.player != null;
            Vector2f movement = new Vector2f(this.movementForward, this.movementSideways);

            float tickDelta = client.gameRenderer.getCamera().getLastTickDelta();

            float yaw = client.gameRenderer.getCamera().getYaw() - client.player.getYaw(tickDelta);
            if(Config.GSON.instance().clickToMove &&  ((MinecraftClientAccessor)client).getOriginalLocation() != null  && ((MinecraftClientAccessor)client).getLocation() != null &&((MinecraftClientAccessor)client).getLocation().getPos() instanceof Vec3d vec3d
                    && client.player.squaredDistanceTo(((MinecraftClientAccessor)client).getOriginalLocation()) < (((MinecraftClientAccessor)client).getOriginalLocation()).squaredDistanceTo(((MinecraftClientAccessor)client).getLocation().getPos())-1) {
                   movement = new Vector2f(1.0F, 0F);
                   yaw = getAngle(new Vec3d(0, 0, 0), vec3d.subtract(client.player.getPos()).subtract(0, vec3d.subtract(client.player.getPos()).getY(), 0));

                   movement.mul(new Matrix2f().rotate((float) Math.toRadians(yaw)));
                   movement.mul(new Matrix2f().rotate((float) Math.toRadians(+client.player.getYaw(tickDelta))));

                   this.movementForward = movement.x;
                   this.movementSideways = movement.y;
                   if(slowDown){
                       this.movementForward *= slowDownFactor;
                       this.movementSideways *= slowDownFactor;

                   }
                   return;

            }
            else{
                movementForward=0;
                movementSideways=0;
            }

            movement.mul(new Matrix2f().rotate((float) Math.toRadians(-yaw)));
            this.movementForward = movement.x;
            this.movementSideways = movement.y;


        }
    }
    public float getAngle(Vec3d start, Vec3d target) {
        return (float) Math.toDegrees(Math.atan2(target.x - start.x, target.z - start.z));
    }

}
