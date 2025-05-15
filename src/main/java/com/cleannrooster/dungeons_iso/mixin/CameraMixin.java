package com.cleannrooster.dungeons_iso.mixin;

import com.cleannrooster.dungeons_iso.api.CameraAccessor;
import com.cleannrooster.dungeons_iso.config.Config;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import org.joml.Vector3d;
import org.joml.Vector3f;
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
            args.setAll(Mod.yaw, Mod.pitch);
        }
    }

    @ModifyArgs(
            method = "update",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;clipToSpace(F)F", ordinal = 0)
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

                args.set(0, (float) args.get(0) * Mod.zoom);
        }
    }
    @Inject(
            method = "clipToSpace",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    private void clipToSpaceXIV(float a, CallbackInfoReturnable<Float> callbackInfoReturnable) {

        if (MinecraftClient.getInstance().gameRenderer.getCamera() instanceof Camera camera && Mod.enabled ) {

            callbackInfoReturnable.setReturnValue(a);

            if(camera.getPitch() != 45){
                this.setRotation(this.yaw,45);
            }
            Mod.yaw = this.yaw;
            Mod.pitch = 45;
            ClientPlayerEntity f = (MinecraftClient.getInstance().player);

            if (Mod.enabled ) {

                MinecraftClient client = MinecraftClient.getInstance();
                assert client.player != null;
                float tickDelta = client.gameRenderer.getCamera().getLastTickDelta();

                Vec3d movement = f.getVelocity().subtract(0,f.getVelocity().getY(),0).normalize().multiply(Math.sqrt(Math.max(0.1,Config.GSON.instance().moveFactor) *2*f.getVelocity().horizontalLength())*
                        f.getRotationVec(tickDelta).subtract(0,f.getRotationVec(tickDelta).getY(),0).normalize().dotProduct(f.getVelocity().subtract(0,f.getVelocity().getY(),0)));

                if(f.getVehicle() != null){
                     movement = f.getVehicle().getVelocity().subtract(0,f.getVehicle().getVelocity().getY(),0).normalize().multiply(Math.sqrt(Math.max(0.1,Config.GSON.instance().moveFactor) *2*f.getVehicle().getVelocity().horizontalLength())*
                            f.getRotationVec(tickDelta).subtract(0,f.getRotationVec(tickDelta).getY(),0).normalize().dotProduct(f.getVehicle().getVelocity().subtract(0,f.getVehicle().getVelocity().getY(),0)));

                }
                if(vec3d == null){
                    vec3d = new Vec3d(this.pos.getX(),pos.getY(),pos.getZ());
                }
                float delta = 1F/(Math.max(0.1F,Config.GSON.instance().moveFactor) *40F);


                Mod.x = movement.x;
                Mod.z = movement.z;
                Vector3f vector3f = (new Vector3f(a, 0, 0)).rotate((camera.getRotation()));
                Vec3d d = (new Vec3d(this.pos.x + (double)vector3f.x, this.pos.y + (double)vector3f.y, this.pos.z + (double)vector3f.z));

                double dot = Math.abs(getRotationVector(45,this.yaw).subtract(0,getRotationVector(45,this.yaw).getY(),0).normalize().dotProduct(f.getEyePos().subtract( d).normalize()));
                if(!Config.GSON.instance().dynamicCamera){
                    delta = 1;
                    vec3d = new Vec3d(MathHelper.lerp(delta,vec3d.getX(),this.pos.getX()),this.pos.getY(), MathHelper.lerp(delta,vec3d.getZ(),this.pos.getZ()));

                }else
                if(!(MinecraftClient.getInstance().options.pickItemKey.isPressed()||ClientInit.moveCameraBinding.isPressed())) {

                    vec3d = new Vec3d(MathHelper.lerp(delta,vec3d.getX(),this.pos.getX()+Mod.x*Math.max(0.1,Config.GSON.instance().moveFactor) *(1+Mod.zoom)),this.pos.getY(), MathHelper.lerp(delta,vec3d.getZ(),this.pos.getZ()+Mod.z*Math.max(0.1,Config.GSON.instance().moveFactor) *(1+Mod.zoom)));
                }


                else{
                    delta = 1F/10F;
                        vec3d = new Vec3d(MathHelper.lerp(delta, vec3d.getX(), this.pos.getX()), this.pos.getY(), MathHelper.lerp(delta, vec3d.getZ(), this.pos.getZ()));


                }
                if ( dot> 0.02){
                    vec3d = new Vec3d(MathHelper.lerp(delta, vec3d.getX(), this.pos.getX()), this.pos.getY(), MathHelper.lerp(delta, vec3d.getZ(), this.pos.getZ()));

                }

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
