package com.cleannrooster.dungeons_iso.mixin;

import com.cleannrooster.dungeons_iso.ClientInit;
import com.cleannrooster.dungeons_iso.ModCompat;
import com.cleannrooster.dungeons_iso.api.MinecraftAccessor;
import com.cleannrooster.dungeons_iso.compat.MidnightControlsCompat;
import com.cleannrooster.dungeons_iso.config.Config;
import com.cleannrooster.dungeons_iso.mod.Mod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix2f;
import org.joml.Vector2f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin extends ClientInput {

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
            Vector2f movement = new Vector2f(this.movementForward, this.movementSideways);

            float tickDelta = client.gameRenderer.getMainCamera().getLastTickProgress();

            boolean bool = ((MinecraftAccessor)client).getLocation() instanceof EntityHitResult result && result.getEntity() instanceof ItemEntity;
            float yaw = client.gameRenderer.getMainCamera().yRot() - client.player.getYRot(tickDelta);
            if((Config.GSON.instance().clickToMove || bool) &&  ((MinecraftAccessor)client).getOriginalLocation() != null  && ((MinecraftAccessor)client).getLocation() != null &&((MinecraftAccessor)client).getLocation().getPos() instanceof Vec3 vec3d
                    && client.player.distanceToSqr(((MinecraftAccessor)client).getOriginalLocation()) < (((MinecraftAccessor)client).getOriginalLocation()).distanceToSqr(((MinecraftAccessor)client).getLocation().getPos())-1) {
                   if(((MinecraftAccessor)client).getLocation() instanceof EntityHitResult && ((MinecraftAccessor)client).getLocation().getPos().subtract(0,((MinecraftAccessor)client).getLocation().getPos().getY()-(client.player.getPos()).getY(),0)
                           .distanceToSqr(client.player.getPos()) < (bool ? client.player.getWidth()/2 :(client.player.getEntityInteractionRange() * client.player.getEntityInteractionRange()/4))){
                       return;
                   }
                if(((MinecraftAccessor)client).getLocation() instanceof BlockHitResult result && Mod.isInteractable(result)){
                    InteractionHand[] var1 = InteractionHand.values();
                    for (InteractionHand hand : var1) {
                        var interact = client.interactionManager.interactBlock(client.player,InteractionHand.MAIN_HAND, result);

                        if (interact.isAccepted()) {
                            if (interact.shouldSwingHand()) {
                                client.player.swing(hand);
                            }

                            Mod.crosshairTarget = null;
                            ((MinecraftAccessor)client).setLocation(null);

                            ((MinecraftAccessor)client).setOriginalLocation(null);
                            ClientInit.clickToMove.setPressed(false);
                            return;
                        }
                    }
                    return;
                }
                    movement = new Vector2f(1.0F, 0F);
                   yaw = getAngle(new Vec3(0, 0, 0), vec3d.subtract(client.player.getPos()).subtract(0, vec3d.subtract(client.player.getPos()).getY(), 0));

                   movement.mul(new Matrix2f().rotate((float) Math.toRadians(yaw)));
                   movement.mul(new Matrix2f().rotate((float) Math.toRadians(+client.player.getYRot(tickDelta))));
                   Mod.unModMovement = new Vector2f(1.0F,0F).mul(new Matrix2f().rotate((float) Math.toRadians(+Minecraft.getInstance().gameRenderer.getMainCamera().yRot())));

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
            if(Config.GSON.instance().cameraRelative) {
                Mod.relativeYaw = yaw;
                movement.mul(new Matrix2f().rotate((float) Math.toRadians(-yaw)));
            }


            this.movementForward = movement.x;
            this.movementSideways = movement.y;

        }
        if(this.pressingBack || this.pressingForward || this.pressingLeft || this.pressingRight){
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
