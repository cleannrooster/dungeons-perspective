package com.cleannrooster.dungeons_iso.mixin;

import com.cleannrooster.dungeons_iso.ClientInit;
import com.cleannrooster.dungeons_iso.ModCompat;
import com.cleannrooster.dungeons_iso.api.MinecraftClientAccessor;
import com.cleannrooster.dungeons_iso.compat.MidnightControlsCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix2f;
import org.joml.Vector2f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.cleannrooster.dungeons_iso.config.Config;
import com.cleannrooster.dungeons_iso.mod.Mod;
import com.cleannrooster.dungeons_iso.util.JoystickInput;

@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin extends ClientInput  {

    // 26.2: ClientInput exposes movement as a Vec2 (x = strafe/left, y = forward) and key state
    // as the keyPresses record, replacing the old float forwardImpulse/leftImpulse and the
    // boolean up/down/left/right fields.


    @Inject(
            method = "tick", at = @At("TAIL")
    )
    private void movementXIV(CallbackInfo ci) {
        if (Mod.enabled ) {
            if(ModCompat.isModLoaded("midnightcontrols") && Mod.noMouse && Mod.useTimer > 40){
                if(MidnightControlsCompat.isEnabled()){
                    return;
                }
            }
            Minecraft client = Minecraft.getInstance();
            assert client.player != null;
            Vector2f joystick = (Config.GSON.instance().isJoystickMovement()
                    && ((GuiScreenAccessor) client.gui).getScreen() == null)
                    ? JoystickInput.readLeftStick() : null;
            if (joystick != null) {
                Mod.joystickRawFwd = joystick.x;
                Mod.joystickRawSide = joystick.y;
                this.moveVector = new Vec2(joystick.y, joystick.x);
            } else {
                Mod.joystickRawFwd = 0F;
                Mod.joystickRawSide = 0F;
            }
            Vector2f movement = new Vector2f(this.moveVector.y, this.moveVector.x);

            float tickDelta = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true);

            boolean bool = ((MinecraftClientAccessor)client).getLocation() instanceof EntityHitResult result && result.getEntity() instanceof ItemEntity;
            float yaw = client.gameRenderer.mainCamera().yRot() - client.player.getViewYRot(tickDelta);
            if((Config.GSON.instance().isClickToMove() || bool) &&  ((MinecraftClientAccessor)client).getOriginalLocation() != null  && ((MinecraftClientAccessor)client).getLocation() != null &&((MinecraftClientAccessor)client).getLocation().getLocation() instanceof Vec3 vec3d
                    && client.player.distanceToSqr(((MinecraftClientAccessor)client).getOriginalLocation()) < (((MinecraftClientAccessor)client).getOriginalLocation()).distanceToSqr(((MinecraftClientAccessor)client).getLocation().getLocation())-1) {
                   if(((MinecraftClientAccessor)client).getLocation() instanceof EntityHitResult && ((MinecraftClientAccessor)client).getLocation().getLocation().subtract(0,((MinecraftClientAccessor)client).getLocation().getLocation().y()-(client.player.position()).y(),0)
                           .distanceToSqr(client.player.position()) < (bool ? client.player.getBbWidth()/2 :(client.player.entityInteractionRange() * client.player.entityInteractionRange()/4))){
                       return;
                   }
                if(((MinecraftClientAccessor)client).getLocation() instanceof BlockHitResult result && Mod.isInteractable(result)){
                    InteractionHand[] var1 = InteractionHand.values();
                    for (InteractionHand hand : var1) {
                        var interact = client.gameMode.useItemOn(client.player,InteractionHand.MAIN_HAND, result);

                        if (interact.consumesAction()) {
                            if (interact instanceof net.minecraft.world.InteractionResult.Success) {
                                client.player.swing(hand);
                            }

                            Mod.crosshairTarget = null;
                            ((MinecraftClientAccessor)client).setLocation(null);

                            ((MinecraftClientAccessor)client).setOriginalLocation(null);
                            ClientInit.clickToMove.setDown(false);
                            return;
                        }
                    }
                    return;
                }
                    movement = new Vector2f(1.0F, 0F);
                   yaw = getAngle(new Vec3(0, 0, 0), vec3d.subtract(client.player.position()).subtract(0, vec3d.subtract(client.player.position()).y(), 0));

                   movement.mul(new Matrix2f().rotate((float) Math.toRadians(yaw)));
                   movement.mul(new Matrix2f().rotate((float) Math.toRadians(+client.player.getViewYRot(tickDelta))));
                   Mod.unModMovement = new Vector2f(1.0F,0F).mul(new Matrix2f().rotate((float) Math.toRadians(+Minecraft.getInstance().gameRenderer.mainCamera().yRot())));

                   this.moveVector = new Vec2(movement.y, movement.x);

                return;

            }
            else{
                this.moveVector = Vec2.ZERO;
            }
            if(Config.GSON.instance().cameraRelative) {
                Mod.relativeYaw = yaw;
                movement.mul(new Matrix2f().rotate((float) Math.toRadians(-yaw)));
            }


            this.moveVector = new Vec2(movement.y, movement.x);

        }
        if(this.keyPresses.backward() || this.keyPresses.forward() || this.keyPresses.left() || this.keyPresses.right()
                || Mod.joystickRawFwd != 0F || Mod.joystickRawSide != 0F){
            Mod.useTimer = 0;
        }
        else{
            Mod.useTimer++;
        }
    }
    public float getAngle(Vec3 start, Vec3 target) {
        return (float) Math.toDegrees(Math.atan2(target.x - start.x, target.z - start.z));
    }

}
