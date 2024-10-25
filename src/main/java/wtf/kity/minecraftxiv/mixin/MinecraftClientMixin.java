package wtf.kity.minecraftxiv.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.Perspective;
import net.minecraft.entity.Entity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wtf.kity.minecraftxiv.ClientInit;
import wtf.kity.minecraftxiv.mod.Mod;
import wtf.kity.minecraftxiv.util.Util;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
    @Shadow
    @Nullable
    public ClientPlayerEntity player;

    @Shadow
    @Final
    public GameOptions options;

    @Inject(method = "tick", at = @At("TAIL"))
    public void tick(CallbackInfo ci) {
        if (this.player == null) {
            return;
        }
        // For some reason, KeyBinding#wasPressed doesn't work here, so I'm using KeyBinding#isPressed, which doesn't
        // seem to break anything.
        //if (ClientInit.getInstance().getKeyBinding().wasPressed() || (this.options.togglePerspectiveKey.wasPressed
        // () && mod.isEnabled())) {
        if (ClientInit.toggleBinding.wasPressed() || (
                this.options.togglePerspectiveKey.isPressed() && Mod.enabled
        )) {
            if (Mod.enabled) {
                options.setPerspective(Mod.lastPerspective);
                Util.debug("Disabled Minecraft XIV");
            } else {
                Mod.lastPerspective = this.options.getPerspective();
                this.options.setPerspective(Perspective.THIRD_PERSON_BACK);
                if (Mod.lastPerspective == Perspective.THIRD_PERSON_FRONT) {
                    Mod.yaw = ((180 + this.player.getYaw() + 180) % 360) - 180;
                    Mod.pitch = -this.player.getPitch();
                } else {
                    Mod.yaw = this.player.getYaw();
                    Mod.pitch = this.player.getPitch();
                }
                Util.debug("Enabled Minecraft XIV");
            }
            Mod.enabled = !Mod.enabled;

            // Re-lock the cursor so it correctly changes state
            MinecraftClient.getInstance().mouse.lockCursor();
        }

        if (ClientInit.zoomInBinding.wasPressed()) {
            if (Mod.enabled) {
                Mod.zoom = Math.max(Mod.zoom - 0.1f, 0.0f);
            }
        }

        if (ClientInit.zoomOutBinding.wasPressed()) {
            if (Mod.enabled) {
                Mod.zoom = Math.min(Mod.zoom + 0.1f, 2.0f);
            }
        }

        if (Mod.lockOnTarget != null && !Mod.lockOnTarget.isAlive()) {
            Mod.lockOnTarget = null;
        }
    }

    @Inject(method = "hasOutline", at = @At("HEAD"), cancellable = true)
    public void hasOutline(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (entity == Mod.lockOnTarget) {
            cir.setReturnValue(true);
            cir.cancel();
        }
    }

    @Inject(method = "disconnect(Lnet/minecraft/client/gui/screen/Screen;)V", at = @At("HEAD"))
    public void disconnectPre(Screen screen, CallbackInfo ci) {
        ClientInit.capabilities = null;
    }
}