package com.cleannrooster.dungeons_iso.mixin;

import com.cleannrooster.dungeons_iso.api.CameraAccessor;
import com.cleannrooster.dungeons_iso.api.ClientBossBarAccessor;
import com.cleannrooster.dungeons_iso.config.Config;
import com.google.common.collect.Lists;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ClientBossBar;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.boss.BossBarManager;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import com.cleannrooster.dungeons_iso.ClientInit;
import com.cleannrooster.dungeons_iso.mod.Mod;

import java.util.*;
import java.util.stream.Stream;

@Mixin(Camera.class)
public abstract class CameraMixin implements CameraAccessor {
    @Override
    public void setPosInterfae(Vec3d pos) {
        this.pos = pos;
    }

    @SuppressWarnings("unused")
    @Shadow
    private float yaw;
    @Shadow
    private float pitch;
    private Vec3d vec3d;
    @Shadow

    private Vec3d pos;
    private Vec3d posBeforeModulation;

    @Override
    public Vec3d getPosBeforeModulation() {
        return posBeforeModulation;
    }

    private Vec3d cachedMovement = Vec3d.ZERO;

    int i = 0;
    @Shadow

    private  BlockPos.Mutable blockPos;

    @Inject(
            method = "getPitch",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    public void getPitch45(CallbackInfoReturnable<Float> ci) {
        if(Mod.enabled) {
            ci.setReturnValue( Mod.enabled ? 45 : this.pitch);
        }
    }

    @Shadow
    protected abstract void setRotation(float yaw, float pitch);

    @ModifyArgs(
            method = "update",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;setRotation(FF)V", ordinal = 0)
    )
    public void a(Args args) {
        if (Mod.enabled) {
            args.set(0,Mod.yaw);
            args.set(1, Mod.pitch);
        }
    }

    @ModifyArgs(
            method = "update",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;clipToSpace(D)D", ordinal = 0)
    )
    public void b(Args args) {
        if (Mod.enabled) {
            if(ClientInit.isoBinding.wasPressed()){
                this.setRotation((float) (Math.ceil(Mod.yaw / 90) * 90 - 45),45);

                Mod.yaw = this.yaw;
                Mod.pitch = this.pitch;
            }else {
                if(ClientInit.rotateClockwase.wasPressed()) {
                    this.setRotation(Mod.yaw+5, Mod.pitch);
                }
                if(ClientInit.rotateCounterClockwise.wasPressed()){
                    this.setRotation(Mod.yaw-5, Mod.pitch);

                }

            }

                args.set(0, (double) args.get(0) * MathHelper.clamp(Config.GSON.instance().zoomFactor,1F,1.5F)*Mod.zoom);
        }
    }
    @Inject(
            method = "clipToSpace",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    private void clipToSpaceXIV(double a, CallbackInfoReturnable<Double> callbackInfoReturnable) {

        if (MinecraftClient.getInstance().gameRenderer.getCamera() != null && Mod.enabled ) {

            callbackInfoReturnable.setReturnValue(a);

            if(MinecraftClient.getInstance().gameRenderer.getCamera().getPitch() != 45){
                this.setRotation(this.yaw,45);
            }
            Mod.yaw = this.yaw;
            Mod.pitch = 45;
            ClientPlayerEntity f = (MinecraftClient.getInstance().player);

            if (Mod.enabled ) {
                this.posBeforeModulation = pos;

                MinecraftClient client = MinecraftClient.getInstance();
                assert client.player != null;
                float tickDelta = client.getTickDelta();

                Vec3d movement = client.player.getVelocity().subtract(0,client.player.getVelocity().getY(),0).multiply(5.5).multiply(1+2*Mod.zoom);

                if(f.getVehicle() != null){
                   movement = client.player.getVehicle().getVelocity().subtract(0,client.player.getVehicle().getVelocity().getY(),0).multiply(5.5).multiply(2*Mod.zoom);
                }
                if(vec3d == null){
                    vec3d = new Vec3d(this.pos.getX(),pos.getY(),pos.getZ());
                }

                var delta = 1F ;


                if(!Config.GSON.instance().dynamicCamera){
                    delta *= tickDelta;
                    Mod.x=0F;
                    Mod.z=0F;
                    vec3d = new Vec3d(pos.getX(),pos.y, pos.getZ());
                }else
                if(!(MinecraftClient.getInstance().options.pickItemKey.isPressed()||ClientInit.moveCameraBinding.isPressed())) {
                    delta *= (float) (((0.10)) * Config.GSON.instance().moveFactor_v3);
                    vec3d = new Vec3d(MathHelper.lerp(delta,vec3d.getX(),client.player.getX()+Mod.x),MathHelper.lerp(delta,vec3d.getY(),client.player.getEyeY()), MathHelper.lerp(delta,vec3d.getZ(),client.player.getZ()+Mod.z));

                }
                else{
                    delta *= 0F;
                    vec3d = new Vec3d(MathHelper.lerp(delta,vec3d.getX(),client.player.getX()+Mod.x),MathHelper.lerp(delta,vec3d.getY(),client.player.getEyeY()), MathHelper.lerp(delta,vec3d.getZ(),client.player.getZ()+Mod.z));

                }

            /*    if ( dot> 0.02){
                    vec3d = new Vec3d(MathHelper.lerp(delta, vec3d.getX(), this.pos.getX()), this.pos.getY(), MathHelper.lerp(delta, vec3d.getZ(), this.pos.getZ()));

                }*/

                this.pos = vec3d;
                this.blockPos.set(vec3d.getX(), vec3d.getY(), vec3d.getZ());



            }

        }


    }
    public final Vec3d getRotationVector(float pitch, float yaw) {
        float f = pitch * 0.017453292F;
        float g = -yaw * 0.017453292F;
        float h = MathHelper.cos(g);
        float i = MathHelper.sin(g);
        float j = MathHelper.cos(f);
        float k = MathHelper.sin(f);
        return new Vec3d((double)(i * j), (double)(-k), (double)(h * j));
    }

    @Inject(
            method = "update",
            at = @At(value = "TAIL"),
            cancellable = true
    )
    public void updateXIV(BlockView area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo info) {
        Camera camera = (Camera)  (Object) this;
        if (!Mod.enabled && Config.GSON.instance().dynamicCamera) {

            vec3d = focusedEntity.getEyePos();
        }

    }
  /*  @Inject(
            method = "getPos",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    public void posXIV(CallbackInfoReturnable<Vec3d> vec3dCallbackInfoReturnable) {
        if((MinecraftClient.getInstance().options.pickItemKey.isPressed()||ClientInit.moveCameraBinding.isPressed())){
            vec3dCallbackInfoReturnable.setReturnValue(pos);
        }
        else if(Mod.enabled && vec3d != null){
            vec3dCallbackInfoReturnable.setReturnValue(vec3d);
        }
    }*/
}
