package com.cleannrooster.dungeons_iso.mixin;

import com.cleannrooster.dungeons_iso.api.CameraAccessor;
import com.cleannrooster.dungeons_iso.mod.Mod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.world.TeleportTarget;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class LivingEntityMixin {
    @Inject(method = "teleportTo", at = @At("TAIL"))
    @Nullable
    public void teleportToCleann(TeleportTarget teleportTarget, CallbackInfoReturnable<Entity> callbackInfoReturnable) {
        Entity entity = (Entity) (Object) this;
        if (Mod.enabled && MinecraftClient.getInstance().gameRenderer.getCamera() instanceof CameraAccessor camera &&  (entity == MinecraftClient.getInstance().cameraEntity || entity == MinecraftClient.getInstance().player)){
            camera.setPosInterfae(teleportTarget.pos());
        }
    }
}
