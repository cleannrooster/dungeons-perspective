package com.cleannrooster.dungeons_iso.api;

import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.gui.components.LerpingBossEvent;

public abstract interface ClientBossBarAccessor {
     Map<UUID, LerpingBossEvent> getBossBars();

}