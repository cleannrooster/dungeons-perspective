package com.cleannrooster.dungeons_iso.mixin;

import com.cleannrooster.dungeons_iso.api.ClientBossBarAccessor;
import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.client.gui.components.LerpingBossEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;
import java.util.UUID;

@Mixin(BossHealthOverlay.class)
public abstract class BossBarHudMixin implements ClientBossBarAccessor {
    @Shadow
    Map<UUID, LerpingBossEvent> events;

    @Override
    public Map<UUID, LerpingBossEvent> getBossBars() {
        return events;
    }
}
