package com.cleannrooster.dungeons_iso.mixin;

import com.cleannrooster.dungeons_iso.ClientInit;
import com.cleannrooster.dungeons_iso.ModCompat;
import com.cleannrooster.dungeons_iso.api.MinecraftClientAccessor;
import com.cleannrooster.dungeons_iso.compat.MidnightControlsCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
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
            if(ModCompat.isModLoaded("midnightcontrols") && Mod.noMouse && Mod.useTimer > 40){
                if(MidnightControlsCompat.isEnabled()){
                    return;
                }
            }
            Minecraft client = Minecraft.getInstance();
            assert client.player != null;
            Vector2f movement = new Vector2f(this.forwardImpulse, this.leftImpulse);

            float tickDelta = client.gameRenderer.getMainCamera().getPartialTickTime();

            boolean bool = ((MinecraftClientAccessor)client).getLocation() instanceof EntityHitResult result && result.getEntity() instanceof ItemEntity;
            float yaw = client.gameRenderer.getMainCamera().getYRot() - client.player.getViewYRot(tickDelta);
            if((Config.GSON.instance().clickToMove || bool) &&  ((MinecraftClientAccessor)client).getOriginalLocation() != null  && ((MinecraftClientAccessor)client).getLocation() != null &&((MinecraftClientAccessor)client).getLocation().getLocation() instanceof Vec3 vec3d
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
                            if (interact.shouldSwing()) {
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
                   Mod.unModMovement = new Vector2f(1.0F,0F).mul(new Matrix2f().rotate((float) Math.toRadians(+Minecraft.getInstance().gameRenderer.getMainCamera().getYRot())));

                   this.forwardImpulse = movement.x;
                   this.leftImpulse = movement.y;
                   if(slowDown){
                       this.forwardImpulse *= slowDownFactor;
                       this.leftImpulse *= slowDownFactor;

                   }

                return;

            }
            else{
                forwardImpulse=0;
                leftImpulse=0;
            }
            if(Config.GSON.instance().cameraRelative) {
                Mod.relativeYaw = yaw;
                movement.mul(new Matrix2f().rotate((float) Math.toRadians(-yaw)));
            }


            this.forwardImpulse = movement.x;
            this.leftImpulse = movement.y;

        }
        if(this.down || this.up || this.left || this.right){
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
