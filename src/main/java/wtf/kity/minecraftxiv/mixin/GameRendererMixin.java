package wtf.kity.minecraftxiv.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wtf.kity.minecraftxiv.ClientInit;
import wtf.kity.minecraftxiv.config.Config;
import wtf.kity.minecraftxiv.mod.Mod;

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
        if (Config.GSON.instance().lockOnTargeting && Mod.lockOnTarget != null) {
            camera.lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, Mod.lockOnTarget.getEyePos());
        } else if (Config.GSON.instance().targetFromCamera && ClientInit.getCapabilities().targetFromCamera()) {
            HitResult target = Mod.crosshairTarget;
            if (target == null) return;
            if (!Config.GSON.instance().unlimitedReach) {
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
