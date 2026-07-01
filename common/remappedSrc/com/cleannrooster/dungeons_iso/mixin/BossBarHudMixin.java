package com.cleannrooster.dungeons_iso.mixin;

import com.cleannrooster.dungeons_iso.api.ClientBossBarAccessor;
import com.cleannrooster.dungeons_iso.mod.Mod;
import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.UUID;
import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.client.gui.components.LerpingBossEvent;

@Mixin(BossHealthOverlay.class)
public abstract class BossBarHudMixin implements ClientBossBarAccessor {
    @Shadow
     Map<UUID, LerpingBossEvent> bossBars ;
    @Override
    public Map<UUID, LerpingBossEvent> getBossBars() {

        return bossBars;
    }
}
