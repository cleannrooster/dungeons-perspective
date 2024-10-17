package wtf.kity.minecraftxiv.mixin;

import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wtf.kity.minecraftxiv.ClientInit;
import wtf.kity.minecraftxiv.Config;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Inject(method = "findCrosshairTarget", at = @At("HEAD"), cancellable = true)
    public void findCrosshairTarget(Entity camera, double blockInteractionRange, double entityInteractionRange, float tickDelta, CallbackInfoReturnable<HitResult> cir) {
        if (Config.targetFromCamera && ClientInit.capabilities.targetFromCamera()) {
                cir.setReturnValue(ClientInit.mod.getCrosshairTarget());
                cir.cancel();
        }
    }
}
