package com.cleannrooster.dungeons_iso.mixin.compat.combat_roll;

import com.cleannrooster.dungeons_iso.config.Config;
import com.cleannrooster.dungeons_iso.mod.Mod;
import net.combat_roll.internals.RollManager;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RollManager.class)
public class RollManagerMixin {

    @Inject(at = @At("HEAD"), method = "onRoll", cancellable = true)

    public void onRollCleann(LocalPlayer player, CallbackInfo info) {
        if(Mod.enabled && Mod.mouseTarget != null && Config.GSON.instance().rollTowardsCursor){
            var speed = player.getMovement().length();
            var vec = player.getMovement();
            var vec2 = Mod.mouseTarget.getPos().subtract(player.getPos());
            player.setVelocity(vec2.subtract(0,vec2.getY(),0).normalize().multiply(speed));
        }
    }

}
