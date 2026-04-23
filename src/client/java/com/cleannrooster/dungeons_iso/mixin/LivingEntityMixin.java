package com.cleannrooster.dungeons_iso.mixin;

import com.cleannrooster.dungeons_iso.api.CameraAccessor;
import com.cleannrooster.dungeons_iso.mod.Mod;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class LivingEntityMixin {
    @Inject(method = "teleport", at = @At("TAIL"))
    @Nullable
    public void teleportToCleann(TeleportTransition teleportTransition, CallbackInfoReturnable<Entity> callbackInfoReturnable) {
        Entity entity = (Entity) (Object) this;
        if (Mod.enabled && Minecraft.getInstance().gameRenderer.getMainCamera() instanceof CameraAccessor camera &&  (entity == Minecraft.getInstance().cameraEntity || entity == Minecraft.getInstance().player)){
            camera.setPosInterfae(teleportTransition.position());
        }
    }


}
