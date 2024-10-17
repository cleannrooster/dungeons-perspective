package wtf.kity.minecraftxiv.mixin;

import wtf.kity.minecraftxiv.ClientInit;
import wtf.kity.minecraftxiv.mod.Mod;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(Camera.class)
public class CameraMixin {
    @SuppressWarnings("unused")
    @Shadow
    private float yaw;
    @SuppressWarnings("unused")
    @Shadow
    private float pitch;

    @ModifyArgs(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;setRotation(FF)V", ordinal = 0))
    public void a(Args args) {
        Mod mod = ClientInit.mod;
        if (mod.isEnabled()) {
            args.setAll(mod.getYaw(), mod.getPitch());
        }
    }

    @ModifyArgs(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;clipToSpace(F)F", ordinal = 0))
    public void b(Args args) {
        Mod mod = ClientInit.mod;
        if (mod.isEnabled()) {
            args.set(0, (float) args.get(0) * mod.getZoom());
        }
    }

    @Inject(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;moveBy(FFF)V", ordinal = 0))
    public void c(BlockView area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
        Mod mod = ClientInit.mod;
        if (mod.isEnabled()) {
            this.yaw = mod.getYaw();
            this.pitch = mod.getPitch();
        }
    }
}
