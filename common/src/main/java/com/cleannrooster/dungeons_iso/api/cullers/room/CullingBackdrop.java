package com.cleannrooster.dungeons_iso.api.cullers.room;

import com.cleannrooster.dungeons_iso.mod.Mod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;

/**
 * Decides when intentionally removed terrain should reveal cave fog instead of the day sky.
 *
 * <p>A room snapshot is authoritative when one exists. Caves and tunnels often have no useful
 * room snapshot, so a small sample of vertical sky visibility supplies the fallback. The result
 * is eased and has separate enter/leave thresholds so walking through a cave mouth cannot make
 * the background chatter between two states.
 */
public final class CullingBackdrop {

    private static final int[][] SAMPLE_OFFSETS = {
            {0, 0}, {3, 0}, {-3, 0}, {0, 3}, {0, -3},
            {3, 3}, {3, -3}, {-3, 3}, {-3, -3}
    };
    private static final float FADE_IN_PER_SECOND = 3.5F;
    private static final float FADE_OUT_PER_SECOND = 2.5F;

    private static boolean enclosed;
    private static float strength;
    private static float red;
    private static float green;
    private static float blue;
    private static long lastUpdateNanos;

    private CullingBackdrop() {
    }

    /** Called once per rendered frame, on the render thread. */
    public static void update() {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientWorld world = client.world;
        if (!Mod.enabled || world == null || client.cameraEntity == null || !hasRemovedTerrain()) {
            enclosed = false;
            approach(false);
            return;
        }

        BlockPos origin = client.cameraEntity.getBlockPos();
        int covered = 0;
        for (int[] offset : SAMPLE_OFFSETS) {
            if (!world.isSkyVisible(origin.add(offset[0], 1, offset[1]))) {
                covered++;
            }
        }

        boolean detectedRoom = hasRemovedRoomTerrain();
        boolean playerColumnCovered = !world.isSkyVisible(origin.up());

        // A published room means the roof culler has positively identified an enclosure, so it is
        // stronger evidence than samples which may land outside a small building. For the fallback,
        // the player's own column carries the same weight: this catches houses narrower than the
        // sample grid. Nearby samples retain hysteresis for broad caves and cave entrances.
        enclosed = detectedRoom || playerColumnCovered || (enclosed ? covered >= 3 : covered >= 5);
        approach(enclosed);
    }

    public static float strength() {
        return strength;
    }

    public static boolean hidesSky() {
        return strength > 0.01F;
    }

    public static void setColor(float red, float green, float blue) {
        CullingBackdrop.red = red;
        CullingBackdrop.green = green;
        CullingBackdrop.blue = blue;
    }

    public static float red() {
        return red;
    }

    public static float green() {
        return green;
    }

    public static float blue() {
        return blue;
    }

    private static boolean hasRemovedTerrain() {
        if (hasRemovedRoomTerrain()) {
            return true;
        }
        SightlineMask sight = SightlineScanner.INSTANCE.isActive()
                ? SightlineScanner.INSTANCE.mask() : null;
        return sight != null && !sight.suppressesCulling() && !sight.sections().isEmpty();
    }

    private static boolean hasRemovedRoomTerrain() {
        RoomSnapshot room = RoomScanner.INSTANCE.isActive() ? RoomScanner.INSTANCE.snapshot() : null;
        return room != null && !room.sections().isEmpty();
    }

    private static void approach(boolean active) {
        long now = System.nanoTime();
        if (lastUpdateNanos == 0L) {
            lastUpdateNanos = now;
        }
        float seconds = Math.min(0.1F, (now - lastUpdateNanos) / 1_000_000_000.0F);
        lastUpdateNanos = now;
        float step = seconds * (active ? FADE_IN_PER_SECOND : FADE_OUT_PER_SECOND);
        strength = active ? Math.min(1.0F, strength + step) : Math.max(0.0F, strength - step);
    }
}
