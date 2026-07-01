package com.cleannrooster.dungeons_iso.mixin;

import com.cleannrooster.dungeons_iso.api.CameraAccessor;
import com.cleannrooster.dungeons_iso.compat.DragonCompat;
import com.cleannrooster.dungeons_iso.config.Config;
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
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import static com.cleannrooster.dungeons_iso.mod.Mod.*;

import F;

@Mixin(Camera.class)
public abstract class CameraMixin implements CameraAccessor {
    @Override
    public void setPosInterfae(Vec3 pos) {
        this.pos = pos;
    }


    @Shadow
    private BlockGetter area;

    @Shadow
    private  Vector3f horizontalPlane;

    @SuppressWarnings("unused")
    @Shadow
    private float yaw;
    @Shadow
    private float pitch;
    private Vec3 vec3d;
    @Shadow

    private Vec3 pos;
    @Shadow
    private Entity focusedEntity;

    private Vec3 posBeforeModulation;

    @Override
    public Vec3 getPosBeforeModulation() {
        return posBeforeModulation;
    }

    private Vec3 cachedMovement = Vec3.ZERO;

    int i = 0;
    @Shadow

    private  BlockPos.MutableBlockPos blockPos;

    @Inject(
            method = "getPitch",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    public void getPitch45(CallbackInfoReturnable<Float> ci) {
        if(Mod.enabled && !Config.GSON.instance().XIV) {
            ci.setReturnValue( (float)  45);
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
            args.set(1,Mod.pitch);
        }
    }

    @ModifyArgs(
            method = "update",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;clipToSpace(F)F", ordinal = 0)
    )
    public void b(Args args) {
        if (Mod.enabled && focusedEntity != null) {
            if(ClientInit.isoBinding.consumeClick()){
                this.setRotation((float) (Math.ceil(Mod.yaw / 90) * 90 - 45),  45);

                Mod.yaw = this.yaw;
                Mod.pitch = this.pitch;

            }else {
                if(ClientInit.rotateClockwise.consumeClick()) {
                    this.setRotation(Mod.yaw+5, Mod.pitch);
                }
                else if(ClientInit.rotateCounterClockwise.consumeClick()){
                    this.setRotation(Mod.yaw-5, Mod.pitch);

                }
                else{
                    this.setRotation(Mod.yaw,Mod.pitch);

                }

            }
            float g = 0.1F;
            float f = args.get(0);

            Vector3f vector3f = (new Vector3f(0, 0, (float)((float) args.get(0) * Mod.getZoom()))).rotate(Minecraft.getInstance().gameRenderer.getMainCamera().rotation());
            Vec3 vec = (new Vec3(this.pos.x + (double)vector3f.x, this.pos.y + (double)vector3f.y, this.pos.z + (double)vector3f.z));
            Vector3f vector3f2 = (new Vector3f(0, 0, (float)((float) args.get(0) * zoom))).rotate(Minecraft.getInstance().gameRenderer.getMainCamera().rotation());
            Vec3 vec2 = (new Vec3(this.pos.x + (double)vector3f.x, this.pos.y + (double)vector3f.y, this.pos.z + (double)vector3f.z));

            Mod.preMod = vec;
            BlockHitResult result = Minecraft.getInstance().cameraEntity.level().clip(new ClipContext(Minecraft.getInstance().cameraEntity.getEyePosition(), vec, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, Minecraft.getInstance().cameraEntity));
            Mod.hit = this.area.clip(new ClipContext(Minecraft.getInstance().cameraEntity.getEyePosition(),vec2, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, Minecraft.getInstance().cameraEntity));

            if (result.getType().equals(HitResult.Type.BLOCK) ) {
                Mod.isBlocked = true;
                Mod.shouldReload = true;
                Mod.startZoomNoDelay = System.currentTimeMillis();

                if(!dirty) {
                    Mod.startTime = (Minecraft.getInstance().level.getGameTime());
                    Mod.startZoom = System.currentTimeMillis();
                }
                Mod.endTime = 0;

                if(zoomOutTime >= 10){
                    Mod.zoomTime =  ((1000F - (System.currentTimeMillis() -  Mod.startZoom))/1000F)-(10-zoomOutTime)/10F;

                }
                else{

                    Mod.zoomTime =  ((1000F - (System.currentTimeMillis() -  Mod.startZoom))/1000F)-(10-zoomOutTime)/10F;

                }
                if(zoomOutTimeNoDelay >= 10){
                    zoomTimeNoDelay =  ((1000F - (System.currentTimeMillis() -  startZoomNoDelay))/1000F)-(10-zoomOutTimeNoDelay)/10F;

                }
                else{

                    zoomTimeNoDelay =  ((1000F - (System.currentTimeMillis() -  Mod.startZoomNoDelay))/1000F)-(10-zoomOutTimeNoDelay)/10F;

                }
                zoomOutTimeNoDelay = 0;

                Mod.dirty = true;
                Mod.dirtyTime = Minecraft.getInstance().level.getGameTime();

                prevblock = Minecraft.getInstance().level.getBlockState(result.getBlockPos());



            } else {
                Mod.isBlocked = false;
                if (!Mod.dirty) {
                    Mod.shouldReload = false;
                }

                Mod.zoomTime =  ((1000F - (System.currentTimeMillis() -  Mod.startZoom))/1000F)-(10-zoomOutTime)/10F;
                Mod.zoomTimeNoDelay =  ((1000F - (System.currentTimeMillis() -  Mod.startZoom))/1000F)-(10-zoomOutTimeNoDelay)/10F;

            }
            frustrumZoom = Math.clamp(frustrumZoom,0,20);

            zoomMetric = args.get(0);
            args.set(0, (float) args.get(0) * getZoom());



        }
    }
    @Inject(
            method = "update",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    private void updateXIVHead(BlockGetter area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta,CallbackInfo ci) {
            if( focusedEntity==  null){
            ci.cancel();
        }
    }
    @Inject(
            method = "clipToSpace",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    private void clipToSpaceXIV(float a, CallbackInfoReturnable<Float> callbackInfoReturnable) {

        if (Minecraft.getInstance().gameRenderer.getMainCamera() instanceof Camera camera && Mod.enabled ) {

                callbackInfoReturnable.setReturnValue(a);



            Mod.yaw = this.yaw;
            if(!Config.GSON.instance().XIV){
            Mod.pitch =  45;
            }
            LocalPlayer f = (Minecraft.getInstance().player);

            if (Mod.enabled ) {
                this.posBeforeModulation = pos;

                Minecraft client = Minecraft.getInstance();
                assert client.player != null;
                float tickDelta = client.gameRenderer.getMainCamera().getPartialTickTime();

                Vec3 movement = client.player.getKnownMovement().subtract(0,client.player.getKnownMovement().y(),0).scale(5.5).scale(1+2*Mod.zoom);

                if(f.getVehicle() != null){
                   movement = client.player.getVehicle().getDeltaMovement().subtract(0,client.player.getVehicle().getDeltaMovement().y(),0).scale(5.5).scale(2*Mod.zoom);
                }
                if(vec3d == null){
                    vec3d = new Vec3(this.pos.x(),pos.y(),pos.z());
                }

                var delta = 1F ;


                if(!(Config.GSON.instance().dynamicCamera || Mod.contextToggle)){
                    delta *= tickDelta;
                    Mod.x=0F;
                    Mod.z=0F;
                    vec3d = new Vec3(pos.x(),pos.y, pos.z());
                }else
                if(!(Minecraft.getInstance().options.keyPickItem.isDown()||ClientInit.moveCameraBinding.isDown())) {
                    delta *= (float) (((0.10)) * Config.GSON.instance().moveFactor_v3);
                    vec3d = new Vec3(Mth.lerp(delta,vec3d.x(),client.player.getX()+Mod.x),Mth.lerp(delta,vec3d.y(),client.player.getEyeY()), Mth.lerp(delta,vec3d.z(),client.player.getZ()+Mod.z));

                }
                else{
                    delta *= 0F;
                    vec3d = new Vec3(Mth.lerp(delta,vec3d.x(),client.player.getX()+Mod.x),Mth.lerp(delta,vec3d.y(),client.player.getEyeY()), Mth.lerp(delta,vec3d.z(),client.player.getZ()+Mod.z));

                }

            /*    if ( dot> 0.02){
                    vec3d = new Vec3d(MathHelper.lerp(delta, vec3d.getX(), this.pos.getX()), this.pos.getY(), MathHelper.lerp(delta, vec3d.getZ(), this.pos.getZ()));

                }*/

                this.pos = vec3d;
                this.blockPos.set(vec3d.x(), vec3d.y(), vec3d.z());



            }

        }


    }
    public final Vec3 getRotationVector(float pitch, float yaw) {
        float f = pitch * 0.017453292F;
        float g = -yaw * 0.017453292F;
        float h = Mth.cos(g);
        float i = Mth.sin(g);
        float j = Mth.cos(f);
        float k = Mth.sin(f);
        return new Vec3((double)(i * j), (double)(-k), (double)(h * j));
    }

    @Inject(
            method = "update",
            at = @At(value = "TAIL"),
            cancellable = true
    )
    public void updateXIV(BlockGetter area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo info) {
        Camera camera = (Camera)  (Object) this;
        if (!Mod.enabled && Config.GSON.instance().dynamicCamera && focusedEntity != null) {

            vec3d = Minecraft.getInstance().cameraEntity.getEyePosition();
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
