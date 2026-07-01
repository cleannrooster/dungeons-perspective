package com.cleannrooster.dungeons_iso.mixin;

import com.cleannrooster.dungeons_iso.mod.Mod;
import dev.kosmx.playerAnim.core.util.Vec3f;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractSoundInstance.class)
public class AbstractSoundInstanceMixin {
    @Shadow
    protected double x;
    @Shadow
    protected double y;
    @Shadow

    protected double z;
    @Shadow
    protected boolean relative;


}
