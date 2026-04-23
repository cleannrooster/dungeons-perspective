package com.cleannrooster.dungeons_iso.mixin.compat.sodium;

import com.cleannrooster.dungeons_iso.compat.SodiumWorldRendererAccessor;
import net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(SodiumWorldRenderer.class)
public abstract class SodiumWorldRendererMixin  implements SodiumWorldRendererAccessor {


}
