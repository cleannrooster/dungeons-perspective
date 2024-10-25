package wtf.kity.minecraftxiv.mixin;

import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import wtf.kity.minecraftxiv.mod.Mod;

@Mixin(Keyboard.class)
public class KeyboardMixin {
    @Redirect(
            method = "onKey",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;openGameMenu(Z)V")
    )
    void openGameMenu(MinecraftClient instance, boolean pauseOnly) {
        if (Mod.lockOnTarget != null) {
            Mod.lockOnTarget = null;
        } else {
            instance.openGameMenu(pauseOnly);
        }
    }
}
