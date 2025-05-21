package com.cleannrooster.dungeons_iso.mixin.compat.sodium;

import com.cleannrooster.dungeons_iso.api.BlockCullerUser;
import com.cleannrooster.dungeons_iso.api.MinecraftClientAccessor;
import com.cleannrooster.dungeons_iso.mod.Mod;
import net.caffeinemc.mods.sodium.client.render.frapi.render.AbstractBlockRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractBlockRenderContext.class)
public abstract class AbstractRenderContextMixin implements BlockCullerUser {
    @Shadow    protected BlockPos pos;


    @Inject(at = @At("RETURN"), method = "isFaceCulled", cancellable = true)
    protected final void isFaceCulledDungeons(@Nullable Direction direction, CallbackInfoReturnable<Boolean> ci) {
        if(MinecraftClient.getInstance() != null && MinecraftClient.getInstance().player instanceof ClientPlayerEntity player && Mod.enabled && ((MinecraftClientAccessor)MinecraftClient.getInstance()).shouldRebuild()) {

                    ci.setReturnValue(false);

        }
    }
}
