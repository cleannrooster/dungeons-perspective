package wtf.kity.minecraftxiv.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.Perspective;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.server.SaveLoader;
import net.minecraft.world.level.storage.LevelStorage;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wtf.kity.minecraftxiv.ClientInit;
import wtf.kity.minecraftxiv.mod.Mod;
import wtf.kity.minecraftxiv.network.Capabilities;
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
        if (this.player == null) return;
        Mod mod = ClientInit.mod;

        // For some reason, KeyBinding#wasPressed doesn't work here, so I'm using KeyBinding#isPressed, which doesn't seem to break anything.
        //if (ClientInit.getInstance().getKeyBinding().wasPressed() || (this.options.togglePerspectiveKey.wasPressed() && mod.isEnabled())) {
        if (ClientInit.toggleBinding.wasPressed() || (this.options.togglePerspectiveKey.isPressed() && mod.isEnabled())) {
            if (mod.isEnabled()) {
                options.setPerspective(mod.getLastPerspective());
                Util.debug("Disabled Minecraft XIV");
            } else {
                mod.setLastPerspective(this.options.getPerspective());
                this.options.setPerspective(Perspective.THIRD_PERSON_BACK);
                if (mod.getLastPerspective() == Perspective.THIRD_PERSON_FRONT) {
                    mod.setYawAndPitch(((180 + this.player.getYaw() + 180) % 360) - 180, -this.player.getPitch());
                } else {
                    mod.setYawAndPitch(this.player.getYaw(), this.player.getPitch());
                }
                Util.debug("Enabled Minecraft XIV");
            }
            mod.setEnabled(!mod.isEnabled());

            // Re-lock the cursor so it correctly changes state
            MinecraftClient.getInstance().mouse.lockCursor();
        }

        if (ClientInit.zoomInBinding.wasPressed()) {
            if (mod.isEnabled()) {
                mod.setZoom(Math.max(mod.getZoom() - 0.1f, 0.0f));
            }
        }

        if (ClientInit.zoomOutBinding.wasPressed()) {
            if (mod.isEnabled()) {
                mod.setZoom(Math.min(mod.getZoom() + 0.1f, 2.0f));
            }
        }
    }

    @Inject(method = "startIntegratedServer", at = @At("HEAD"))
    public void startIntegratedServerPre(LevelStorage.Session session, ResourcePackManager dataPackManager, SaveLoader saveLoader, boolean newWorld, CallbackInfo ci) {
        ClientInit.setCapabilities(Capabilities.all());
    }

    @Inject(method = "disconnect(Lnet/minecraft/client/gui/screen/Screen;)V", at = @At("HEAD"))
    public void disconnectPre(Screen screen, CallbackInfo ci) {
        ClientInit.setCapabilities(Capabilities.none());
    }
}