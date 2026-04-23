package com.cleannrooster.dungeons_iso.api;

import net.minecraft.client.gui.components.LerpingBossEvent;

import java.util.Map;
import java.util.UUID;

public interface ClientBossBarAccessor {
    Map<UUID, LerpingBossEvent> getBossBars();
}
