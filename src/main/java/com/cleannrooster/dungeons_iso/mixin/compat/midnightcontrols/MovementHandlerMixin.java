package com.cleannrooster.dungeons_iso.mixin.compat.midnightcontrols;

import com.cleannrooster.dungeons_iso.mod.Mod;
import eu.midnightdust.midnightcontrols.client.MidnightControlsConfig;
import eu.midnightdust.midnightcontrols.client.controller.ButtonBinding;
import eu.midnightdust.midnightcontrols.client.controller.MovementHandler;
import eu.midnightdust.midnightcontrols.client.enums.ButtonState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix2f;
import org.joml.Vector2f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MovementHandler.class)
public class MovementHandlerMixin {
    @Shadow
    private boolean shouldOverrideMovement ;
    @Shadow
    private boolean pressingForward;
    @Shadow
    private boolean pressingBack ;
    @Shadow
    private boolean pressingLeft;
    @Shadow
    private boolean pressingRight ;
    @Shadow
    private float slowdownFactor ;
    @Shadow
    private float movementForward ;
    @Shadow
    private float movementSideways;
    @Inject(at = @At("HEAD"), method = "applyMovement", cancellable = true)
    public void applyMovementXIV(@NotNull ClientPlayerEntity player, CallbackInfo callbackInfo) {
        if (Mod.enabled && Mod.noMouse && Mod.useTimer > 40) {
            float yaw = MinecraftClient.getInstance().gameRenderer.getCamera().getYaw() - player.getYaw(MinecraftClient.getInstance().gameRenderer.getCamera().getLastTickDelta());

            Vector2f vec2f = new Vector2f(movementForward,movementSideways);
            vec2f.mul(new Matrix2f().rotate((float) Math.toRadians(-yaw)));

            if(pressingForward || pressingBack || pressingLeft || pressingRight) {

                player.input.movementForward = vec2f.x;
                player.input.movementSideways = vec2f.y;
            }
            callbackInfo.cancel();
        }
    }

}
