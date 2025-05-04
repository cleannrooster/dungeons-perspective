package com.cleannrooster.dungeons_iso.mixin;

import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import com.cleannrooster.dungeons_iso.ClientInit;
import com.cleannrooster.dungeons_iso.mod.Mod;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @SuppressWarnings("unused")
    @Shadow
    private float yaw;
    @SuppressWarnings("unused")
    @Shadow
    private float pitch;
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
                this.setRotation(Mod.yaw, Mod.pitch);

            }
                args.set(0, (float) args.get(0) * Mod.zoom);
        }
    }
    @Inject(
            method = "clipToSpace",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    private void clipToSpaceXIV(float f, CallbackInfoReturnable<Float> callbackInfoReturnable) {
        Camera camera = (Camera)  (Object) this;

        if (Mod.enabled) {

            callbackInfoReturnable.setReturnValue(f);

            if(camera.getPitch() != 45){
                this.setRotation(this.yaw,45);
            }
            Mod.yaw = this.yaw;
            Mod.pitch = 45;
        }
    }
}
