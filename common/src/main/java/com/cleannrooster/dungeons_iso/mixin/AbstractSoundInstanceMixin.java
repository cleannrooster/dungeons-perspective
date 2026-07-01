package com.cleannrooster.dungeons_iso.mixin;

import com.cleannrooster.dungeons_iso.mod.Mod;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

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
