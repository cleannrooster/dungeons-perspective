package com.cleannrooster.dungeons_iso.mixin.compat.sodium;

import com.cleannrooster.dungeons_iso.mod.Mod;
import net.caffeinemc.mods.sodium.client.render.model.AbstractRenderContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractRenderContext.class)
public abstract class AbstractRenderContextMixin2  {
    @Inject(at = @At("RETURN"), method = "hasTransform", cancellable = true, remap = false)
    public void hasTransformXIV(CallbackInfoReturnable<Boolean> cir) {
        if(Mod.enabled && Mod.shouldRebuild()){
            cir.setReturnValue(false);
        }
    }
}
