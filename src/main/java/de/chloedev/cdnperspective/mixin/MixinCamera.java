package de.chloedev.cdnperspective.mixin;

import de.chloedev.cdnperspective.Client;
import de.chloedev.cdnperspective.mod.Mod;
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
public class MixinCamera {
    @Shadow
    private float yaw;
    @Shadow
    private float pitch;

    @ModifyArgs(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;setRotation(FF)V", ordinal = 0))
    public void a(Args args) {
        Mod mod = Client.getInstance().getMod();
        if (mod.isEnabled()) {
            args.setAll(mod.getYaw(), mod.getPitch());
        }
    }

    @Inject(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;moveBy(FFF)V", ordinal = 0))
    public void b(BlockView area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
        Mod mod = Client.getInstance().getMod();
        if (mod.isEnabled()) {
            this.yaw = mod.getYaw();
            this.pitch = mod.getPitch();
        }
    }
}
