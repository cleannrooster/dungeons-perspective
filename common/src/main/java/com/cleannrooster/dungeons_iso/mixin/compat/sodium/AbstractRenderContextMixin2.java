package com.cleannrooster.dungeons_iso.mixin.compat.sodium;

import com.cleannrooster.dungeons_iso.api.BlockCullerUser;
import com.cleannrooster.dungeons_iso.mod.Mod;
import net.caffeinemc.mods.sodium.client.model.light.LightPipelineProvider;
import net.caffeinemc.mods.sodium.client.model.light.data.QuadLightData;
import net.caffeinemc.mods.sodium.client.render.model.AbstractBlockRenderContext;
import net.caffeinemc.mods.sodium.client.render.model.AbstractRenderContext;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractRenderContext.class)
public abstract class AbstractRenderContextMixin2  {
    // NOTE (Sodium 0.9.0 port): AbstractRenderContext#hasTransform was removed (the FRAPI quad
    // transform stack was reworked), so this injection has no target. Disabling FRAPI transforms
    // during a rebuild needs reimplementing against Sodium 0.9.0's transform model.
    /*
    @Inject(at = @At("RETURN"), method = "hasTransform", cancellable = true, remap = false)
    public void hasTransformXIV(CallbackInfoReturnable<Boolean> cir) {
        if(Mod.enabled && Mod.shouldRebuild()){
            cir.setReturnValue(false);
        }
    }
    */
}
