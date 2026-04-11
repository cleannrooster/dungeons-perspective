package com.cleannrooster.dungeons_iso.api;

import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.gui.hud.ClientBossBar;
import net.minecraft.client.render.chunk.ChunkBuilder;

import java.util.Map;
import java.util.UUID;

public abstract interface ClientBossBarAccessor {
     Map<UUID, ClientBossBar> getBossBars();

}