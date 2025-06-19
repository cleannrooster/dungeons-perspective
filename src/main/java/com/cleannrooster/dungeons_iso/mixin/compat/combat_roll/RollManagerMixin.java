package com.cleannrooster.dungeons_iso.mixin.compat.combat_roll;

import com.cleannrooster.dungeons_iso.config.Config;
import com.cleannrooster.dungeons_iso.mod.Mod;

import net.combatroll.internals.RollManager;
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
            var speed = player.getVelocity().length();
            var vec = player.getVelocity();
            var vec2 = Mod.crosshairTarget.getPos().subtract(player.getPos());
            player.setVelocity(vec2.subtract(0,vec2.getY(),0).normalize().multiply(speed));
        }
    }

}
