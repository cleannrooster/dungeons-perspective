package com.cleannrooster.dungeons_iso.api.cullers.room;

import com.cleannrooster.dungeons_iso.ModCompat;

/**
 * Re-meshes a chunk section, whichever chunk renderer is installed.
 *
 * <p>Culling is baked into chunk meshes, so a change in what the scanners cull only becomes visible
 * once the affected sections are rebuilt. Sodium and vanilla expose unrelated APIs for that, and
 * this is the seam between them.
 */
public interface ChunkRebuildScheduler {

    /** Temporary test switch matching the disabled Sodium mixins in {@code MixinPlugin}. */
    boolean ENABLE_SODIUM_COMPAT = false;

    /** Schedules a rebuild of the section at the given section coordinates. */
    void scheduleSection(int sectionX, int sectionY, int sectionZ);

    /**
     * The implementation for this installation, resolved once on first use.
     *
     * <p>Loaded reflectively on purpose. A direct {@code new SodiumRebuildScheduler()} here would
     * be resolved when <em>this</em> method is verified, not when the branch runs, which means a
     * no-Sodium install would hit a {@link NoClassDefFoundError} on the way to deciding it does not
     * have Sodium. Going through {@link Class#forName} keeps every reference to Sodium's classes
     * inside a class that is only ever loaded when Sodium is actually present.
     */
    static ChunkRebuildScheduler get() {
        return Holder.INSTANCE;
    }

    final class Holder {
        static final ChunkRebuildScheduler INSTANCE = resolve();

        private Holder() {
        }

        private static ChunkRebuildScheduler resolve() {
            if (ENABLE_SODIUM_COMPAT && ModCompat.isModLoaded("sodium")) {
                try {
                    return (ChunkRebuildScheduler) Class
                            .forName("com.cleannrooster.dungeons_iso.compat.SodiumRebuildScheduler")
                            .getDeclaredConstructor()
                            .newInstance();
                } catch (Throwable ignored) {
                    // Sodium present but not the version we compile against — fall back rather
                    // than leaving culling with no way to show its results.
                }
            }
            return new VanillaRebuildScheduler();
        }
    }
}
