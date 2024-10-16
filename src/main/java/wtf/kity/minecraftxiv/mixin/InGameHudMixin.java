package wtf.kity.minecraftxiv.mixin;

import wtf.kity.minecraftxiv.Client;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.*;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class InGameHudMixin {
    @Shadow
    @Final
    private MinecraftClient client;

    @Redirect(
            method = "renderCrosshair",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/option/Perspective;isFirstPerson()Z"
            )
    )
    private boolean isFirstPerson(Perspective perspective) {
        return perspective.isFirstPerson() || Client.getInstance().getMod().isEnabled() && !Client.getInstance().getMoveCameraBinding().isPressed();
    }

    @Inject(
            method = "renderCrosshair",
            at = @At("HEAD")
    )
    private void crosshairPre(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        double scaleFactor = client.getWindow().getScaleFactor();
        Mouse mouse = client.mouse;

        //Using RenderSystem on purpose.
        //The f3 "axes" debug cursor calls RenderSystem directly instead of using matrix stack.
        context.getMatrices().push();
        context.getMatrices().translate(-context.getScaledWindowWidth() / 2d + mouse.getX() / scaleFactor, -context.getScaledWindowHeight() / 2f + mouse.getY() / scaleFactor, 0);
    }

    @Inject(
            method = "renderCrosshair",
            at = @At("RETURN")
    )
    private void crosshairPost(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        context.getMatrices().pop();
    }
}
