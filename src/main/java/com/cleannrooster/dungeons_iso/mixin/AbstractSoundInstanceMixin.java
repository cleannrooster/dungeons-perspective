package com.cleannrooster.dungeons_iso.mixin;

import com.cleannrooster.dungeons_iso.mod.Mod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.AbstractSoundInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractSoundInstance.class)
public class AbstractSoundInstanceMixin {
    @Shadow
    protected double x;
    @Shadow
    protected double y;
    @Shadow

    protected double z;
    @Inject(
            method = "getVolume",
            at = @At(value = "RETURN"),
            cancellable = true
    )
    public void getVolumeCleann(CallbackInfoReturnable<Float> ci) {
        if(MinecraftClient.getInstance().cameraEntity != null && MinecraftClient.getInstance().cameraEntity.squaredDistanceTo(x,y,z) < 16* 16){
            if(Mod.enabled && ci.getReturnValue() != null){
                ci.setReturnValue(ci.getReturnValue() * Mod.zoom);
            }
        }

    }
}
