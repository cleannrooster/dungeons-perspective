package com.cleannrooster.dungeons_iso.mixin.compat.combat_roll;

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
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RollManager.class)
public class RollManagerMixin {

    @Inject(at = @At("HEAD"), method = "onRoll", cancellable = true)

    public void onRollCleann(ClientPlayerEntity player, CallbackInfo info) {
        if(Mod.enabled && Mod.crosshairTarget != null && Config.GSON.instance().rollTowardsCursor){
            var speed = player.getMovement().length();
            var vec = player.getMovement();
            var vec2 = Mod.crosshairTarget.getPos().subtract(player.getPos());
            player.setVelocity(vec2.subtract(0,vec2.getY(),0).normalize().multiply(speed));
        }
    }

}
