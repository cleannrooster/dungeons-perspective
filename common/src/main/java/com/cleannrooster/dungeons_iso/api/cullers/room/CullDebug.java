package com.cleannrooster.dungeons_iso.api.cullers.room;

import com.cleannrooster.dungeons_iso.config.Config;
import com.cleannrooster.dungeons_iso.mod.Mod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Dumps the state of both culling tiers to chat.
 *
 * <p>Every decision in this system is invisible from inside the game: a scan that bails, a shape
 * judged unresolved and skipped, a snapshot that is correct but whose sections were never
 * re-meshed, and a disabled gate all look identical — nothing happens. This prints the gates and
 * the last outcome of each scan so those cases can be told apart in one glance.
 */
public final class CullDebug {

    private static final org.apache.logging.log4j.Logger LOG =
            org.apache.logging.log4j.LogManager.getLogger("dungeons_iso/cull");

    private static String lastLogged = null;
    private static int tickCounter = 0;

    private CullDebug() {
    }

    /**
     * Logs a one-line summary whenever the culling state changes, and once a second while nothing
     * is being culled. Goes quiet as soon as both tiers report a healthy snapshot, so it costs
     * nothing once things work. Called every client tick.
     */
    public static void tickLog() {
        // Off unless asked for. This never falls silent on its own — the scan counter advances
        // every second, so the summary always differs from the last one and always logs.
        if (!Config.GSON.instance().cullDebugLog) {
            return;
        }
        if (++tickCounter % 20 != 0) {
            return;
        }

        RoomScanner room = RoomScanner.INSTANCE;
        SightlineScanner sight = SightlineScanner.INSTANCE;
        RoomSnapshot snap = room.snapshot();
        SightlineMask mask = sight.mask();

        boolean healthy = snap != null && mask != null
                && "ok".equals(room.lastResult) && "ok".equals(sight.lastResult);

        String summary = "enabled=" + Mod.enabled
                + " shouldRebuild=" + Mod.shouldRebuild()
                + " (shouldReload=" + Mod.shouldReload + " endTime=" + Mod.endTime + ")"
                + " | room active=" + room.isActive() + " scans=" + room.scanCount
                + " snap=" + (snap == null ? "none" : snap.columnCount() + "col/" + snap.sections().size() + "sec")
                + " last=[" + room.lastResult + "]"
                + " air=" + room.lastAirColumns + " sky=" + room.lastOpenSkyColumns
                + " | shape active=" + sight.isActive() + " casts=" + sight.castCount
                + " mask=" + (mask == null ? "none" : mask.blockCount() + "blk/" + mask.sections().size() + "sec")
                + " last=[" + sight.lastResult + "]"
                + " shapes=" + sight.lastShapesFound + " unresolved=" + sight.lastShapesIncomplete
                + "[cap=" + sight.lastUnresolvedByCap + " span=" + sight.lastUnresolvedBySpan
                + " height=" + sight.lastUnresolvedByHeight + "]"
                + " fellback=" + sight.lastShapesFellBack
                + " culled=" + sight.lastShapesCulled + " " + sight.lastShapeClasses
                + " | ghost verts=" + GhostRenderer.lastVertexCount
                + " | queue=" + SectionRebuildQueue.INSTANCE.size();

        if (healthy && summary.equals(lastLogged)) {
            return;
        }
        if (!summary.equals(lastLogged)) {
            lastLogged = summary;
            LOG.info(summary);
        } else if (!healthy) {
            LOG.info(summary);
        }
    }

    public static void report() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }

        line(client, Formatting.GOLD, "── dungeons_iso culling ──");

        // Gates first: all three must hold before either tier does anything at all.
        boolean rebuild = Mod.shouldRebuild();
        gate(client, "Mod.enabled", Mod.enabled);
        gate(client, "shouldRebuild (camera blocked)", rebuild);
        if (!rebuild) {
            line(client, Formatting.GRAY,
                    "   shouldReload=" + Mod.shouldReload + " endTime=" + Mod.endTime);
        }

        // Tier 1 — room roof.
        RoomScanner room = RoomScanner.INSTANCE;
        RoomSnapshot snap = room.snapshot();
        line(client, Formatting.AQUA, "Room culling");
        gate(client, "  config roomCulling", Config.GSON.instance().roomCulling);
        gate(client, "  active", room.isActive());
        line(client, Formatting.GRAY, "   scans=" + room.scanCount + "  last: " + room.lastResult);
        line(client, Formatting.GRAY, "   air columns=" + room.lastAirColumns
                + "  open sky=" + room.lastOpenSkyColumns
                + "  final columns=" + room.lastFinalColumns
                + "  sections=" + room.lastSectionCount);
        line(client, snap == null ? Formatting.RED : Formatting.GREEN,
                "   snapshot: " + (snap == null
                        ? "none published"
                        : snap.columnCount() + " columns, " + snap.sections().size()
                                + " sections, origin " + snap.origin().toShortString()));

        // Tier 2 — shape culling.
        SightlineScanner sight = SightlineScanner.INSTANCE;
        SightlineMask mask = sight.mask();
        line(client, Formatting.AQUA, "Shape culling");
        gate(client, "  config shapeCulling", Config.GSON.instance().shapeCulling);
        gate(client, "  active", sight.isActive());
        line(client, Formatting.GRAY, "   casts=" + sight.castCount + "  last: " + sight.lastResult);
        line(client, Formatting.GRAY, "   rays=" + sight.lastRays + " (clear " + sight.lastClearRays + ")"
                + "  shapes=" + sight.lastShapesFound
                + "  unresolved=" + sight.lastShapesIncomplete
                + "  below threshold=" + sight.lastShapesBelowThreshold
                + "  culled=" + sight.lastShapesCulled);
        line(client, mask == null ? Formatting.RED : Formatting.GREEN,
                "   mask: " + (mask == null
                        ? "none published"
                        : mask.blockCount() + " blocks, " + mask.sections().size() + " sections, visible "
                                + String.format("%.2f", mask.visibleFraction())
                                + (mask.suppressesCulling() ? " (suppressed)" : "")));

        // If the snapshots are right but nothing changed on screen, the backlog is the culprit.
        line(client, Formatting.YELLOW, "Rebuild queue: " + SectionRebuildQueue.INSTANCE.size()
                + " sections pending, " + Config.GSON.instance().roomSectionsPerTick + "/tick");
    }

    private static void gate(MinecraftClient client, String name, boolean value) {
        line(client, value ? Formatting.GREEN : Formatting.RED, (value ? "  ✔ " : "  ✘ ") + name);
    }

    private static void line(MinecraftClient client, Formatting colour, String text) {
        client.player.sendMessage(Text.literal(text).formatted(colour), false);
    }
}
