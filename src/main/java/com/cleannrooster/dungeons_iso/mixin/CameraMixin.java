package com.cleannrooster.dungeons_iso.mixin;

import com.cleannrooster.dungeons_iso.api.CameraAccessor;
import com.cleannrooster.dungeons_iso.compat.DragonCompat;
import com.cleannrooster.dungeons_iso.config.Config;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import net.minecraft.world.RaycastContext;
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

import static com.cleannrooster.dungeons_iso.mod.Mod.*;

@Mixin(Camera.class)
public abstract class CameraMixin implements CameraAccessor {
    @Override
    public void setPosInterfae(Vec3d pos) {
        this.pos = pos;
    }


    @Shadow
    private BlockView area;

    @Shadow
    private  Vector3f horizontalPlane;

    @SuppressWarnings("unused")
    @Shadow
    private float yaw;
    @Shadow
    private float pitch;
    private Vec3d vec3d;
    @Shadow

    private Vec3d pos;
    @Shadow
    private Entity focusedEntity;

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
        if (Mod.enabled) {
            if(ClientInit.isoBinding.wasPressed()){
                this.setRotation((float) (Math.ceil(Mod.yaw / 90) * 90 - 45),  45);

                Mod.yaw = this.yaw;
                Mod.pitch = this.pitch;
            }else {
                if(ClientInit.rotateClockwise.wasPressed()) {
                    this.setRotation(Mod.yaw+5, Mod.pitch);
                }
                if(ClientInit.rotateCounterClockwise.wasPressed()){
                    this.setRotation(Mod.yaw-5, Mod.pitch);

                }

            }
            float g = 0.1F;
            float f = args.get(0);

            Vector3f vector3f = (new Vector3f(0, 0, (float)((float) args.get(0) * Mod.getZoom()))).rotate(MinecraftClient.getInstance().gameRenderer.getCamera().getRotation());
            Vec3d vec = (new Vec3d(this.pos.x + (double)vector3f.x, this.pos.y + (double)vector3f.y, this.pos.z + (double)vector3f.z));
            Mod.preMod = vec;
            BlockHitResult result = MinecraftClient.getInstance().cameraEntity.getWorld().raycast(new RaycastContext(MinecraftClient.getInstance().cameraEntity.getEyePos(), vec, RaycastContext.ShapeType.VISUAL, RaycastContext.FluidHandling.NONE, MinecraftClient.getInstance().cameraEntity));
            Mod.hit = this.area.raycast(new RaycastContext(this.focusedEntity.getEyePos(),vec, RaycastContext.ShapeType.VISUAL, RaycastContext.FluidHandling.NONE, this.focusedEntity));

            if (result.getType().equals(HitResult.Type.BLOCK) ) {
                Mod.isBlocked = true;
                Mod.shouldReload = true;
                Mod.startZoomNoDelay = System.currentTimeMillis();

                if(!dirty) {
                    Mod.startTime = (MinecraftClient.getInstance().world.getTime());
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
                Mod.dirtyTime = MinecraftClient.getInstance().world.getTime();

                prevblock = MinecraftClient.getInstance().world.getBlockState(result.getBlockPos());



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
            method = "clipToSpace",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    private void clipToSpaceXIV(float a, CallbackInfoReturnable<Float> callbackInfoReturnable) {

        if (MinecraftClient.getInstance().gameRenderer.getCamera() instanceof Camera camera && Mod.enabled ) {

                callbackInfoReturnable.setReturnValue(a);



            Mod.yaw = this.yaw;
            if(!Config.GSON.instance().XIV){
            Mod.pitch =  45;
            }
            ClientPlayerEntity f = (MinecraftClient.getInstance().player);

            if (Mod.enabled ) {
                this.posBeforeModulation = pos;

                MinecraftClient client = MinecraftClient.getInstance();
                assert client.player != null;
                float tickDelta = client.gameRenderer.getCamera().getLastTickDelta();

                Vec3d movement = client.player.getMovement().subtract(0,client.player.getMovement().getY(),0).multiply(5.5).multiply(1+2*Mod.zoom);

                if(f.getVehicle() != null){
                   movement = client.player.getVehicle().getVelocity().subtract(0,client.player.getVehicle().getVelocity().getY(),0).multiply(5.5).multiply(2*Mod.zoom);
                }
                if(vec3d == null){
                    vec3d = new Vec3d(this.pos.getX(),pos.getY(),pos.getZ());
                }

                var delta = 1F ;


                if(!(Config.GSON.instance().dynamicCamera || Mod.contextToggle)){
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
