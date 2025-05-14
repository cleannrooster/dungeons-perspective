package com.cleannrooster.dungeons_iso.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
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
            movement.mul(new Matrix2f().rotate((float) Math.toRadians(-yaw)));
            this.movementForward = movement.x;
            this.movementSideways = movement.y;
        }
    }
}
