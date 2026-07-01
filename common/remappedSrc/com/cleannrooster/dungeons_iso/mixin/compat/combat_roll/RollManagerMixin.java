package com.cleannrooster.dungeons_iso.mixin.compat.combat_roll;

import D;
import com.cleannrooster.dungeons_iso.config.Config;
import com.cleannrooster.dungeons_iso.mod.Mod;
import net.combat_roll.CombatRollMod;
import net.combat_roll.Platform;
import net.combat_roll.api.CombatRoll;
import net.combat_roll.client.Keybindings;
import net.combat_roll.client.RollEffect;
import net.combat_roll.compatibility.BetterCombatHelper;
import net.combat_roll.internals.RollManager;
import net.combat_roll.internals.RollingEntity;
import net.combat_roll.mixin.MinecraftClientMixin;
import net.combat_roll.network.Packets;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RollManager.class)
public class RollManagerMixin {

    @Inject(at = @At("HEAD"), method = "onRoll", cancellable = true)

    public void onRollCleann(LocalPlayer player, CallbackInfo info) {
        if(Mod.enabled && Mod.mouseTarget != null && Config.GSON.instance().rollTowardsCursor){
            var speed = player.getKnownMovement().length();
            var vec = player.getKnownMovement();
            var vec2 = Mod.mouseTarget.getLocation().subtract(player.position());
            player.setDeltaMovement(vec2.subtract(0,vec2.y(),0).normalize().scale(speed));
        }
    }

}
