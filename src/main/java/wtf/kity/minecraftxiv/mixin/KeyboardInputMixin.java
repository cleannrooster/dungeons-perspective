package wtf.kity.minecraftxiv.mixin;

import wtf.kity.minecraftxiv.Client;
import wtf.kity.minecraftxiv.mod.Mod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import org.joml.Matrix2f;
import org.joml.Vector2f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin extends Input {
    @Inject(
            method = "tick",
            at = @At("TAIL")
    )
    private void movement(boolean slowDown, float slowDownFactor, CallbackInfo ci) {
        Mod mod = Client.getInstance().getMod();
        if (mod.isEnabled()) {
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
