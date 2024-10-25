package wtf.kity.minecraftxiv.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import org.joml.Matrix2f;
import org.joml.Vector2f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wtf.kity.minecraftxiv.config.Config;
import wtf.kity.minecraftxiv.mod.Mod;

@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin extends Input {
    @Inject(
            method = "tick", at = @At("TAIL")
    )
    private void movement(boolean slowDown, float slowDownFactor, CallbackInfo ci) {
        if (Mod.enabled && Config.GSON.instance().movementCameraRelative) {
            MinecraftClient client = MinecraftClient.getInstance();
            assert client.player != null;
            Vector2f movement = new Vector2f(this.movementForward, this.movementSideways);
            float yaw = client.gameRenderer.getCamera().getYaw() - client.player.getBodyYaw();
            movement.mul(new Matrix2f().rotate((float) Math.toRadians(-yaw)));
            this.movementForward = movement.x;
            this.movementSideways = movement.y;
        }
    }
}
