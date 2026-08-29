package com.cleannrooster.dungeons_iso.api.cullers.room;

import com.cleannrooster.dungeons_iso.config.Config;
import com.cleannrooster.dungeons_iso.mod.Mod;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Compact in-game and log diagnostics for the asynchronous culling pipeline. */
public final class CullDebug {
    private static final Logger LOG = LogManager.getLogger("dungeons_iso/cull");
    private static int ticks;
    private static String last;

    private CullDebug() {
    }

    public static void tickLog() {
        if (!Config.GSON.instance().cullDebugLog || ++ticks % 20 != 0) return;
        String summary = summary();
        if (!summary.equals(last)) {
            last = summary;
            LOG.info(summary);
        }
    }

    public static void report() {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            client.player.sendSystemMessage(Component.literal(summary()).withStyle(ChatFormatting.GOLD));
        }
    }

    private static String summary() {
        RoomScanner room = RoomScanner.INSTANCE;
        SightlineScanner sight = SightlineScanner.INSTANCE;
        RoomSnapshot snapshot = room.snapshot();
        SightlineMask mask = sight.mask();
        return "Cull: enabled=" + Mod.enabled + " blocked=" + Mod.shouldRebuild()
                + " | room active=" + room.isActive() + " scans=" + room.scanCount
                + " result=" + room.lastResult
                + " snapshot=" + (snapshot == null ? "none" : snapshot.columnCount() + "col")
                + " | shape active=" + sight.isActive() + " casts=" + sight.castCount
                + " result=" + sight.lastResult
                + " mask=" + (mask == null ? "none" : mask.blockCount() + "blk")
                + " | ghost=" + GhostRenderer.lastVertexCount + "v"
                + " queue=" + SectionRebuildQueue.INSTANCE.size();
    }
}
