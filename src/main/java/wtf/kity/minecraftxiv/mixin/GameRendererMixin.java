package wtf.kity.minecraftxiv.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wtf.kity.minecraftxiv.ClientInit;
import wtf.kity.minecraftxiv.config.Config;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(method = "findCrosshairTarget", at = @At("HEAD"), cancellable = true)
    public void findCrosshairTarget(
            Entity camera,
            double blockInteractionRange,
            double entityInteractionRange,
            float tickDelta,
            CallbackInfoReturnable<HitResult> cir
    ) {
        if (Config.GSON.instance().targetFromCamera && ClientInit.getCapabilities().targetFromCamera()) {
            HitResult target = ClientInit.mod.getCrosshairTarget();
            if (target != null && !Config.GSON.instance().unlimitedReach) {
                target = ((GameRendererAccessor) MinecraftClient.getInstance().gameRenderer).callEnsureTargetInRange(
                        target,
                        camera.getCameraPosVec(tickDelta),
                        blockInteractionRange
                );
            }
            cir.setReturnValue(target);
            cir.cancel();
        }
    }
}
