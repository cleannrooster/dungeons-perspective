package com.cleannrooster.dungeons_iso.config;

/**
 * The config, however it happens to be stored.
 *
 * <p>Stands in for YACL's {@code ConfigClassHandler} at every call site — {@code instance()},
 * {@code load()} and {@code save()} keep the signatures the rest of the mod already uses — so
 * whether YACL is installed is a question only this class and its backends have to answer.
 */
public final class ConfigHolder<T> {

    private final ConfigBackend<T> backend;

    private ConfigHolder(ConfigBackend<T> backend) {
        this.backend = backend;
    }

    /**
     * Picks a backend for this installation.
     *
     * <p>Presence is tested by looking for YACL's own class rather than by asking the loader
     * whether the mod is loaded. This runs during {@link Config}'s static init, which the earliest
     * mixins can trigger before NeoForge's {@code ModList} exists — at which point a mod-id query
     * answers "no" for a YACL that is sitting right there on the classpath, and the config screen
     * would then have no handler to bind to. The class is either loadable or it is not, at any
     * point in startup.
     */
    @SuppressWarnings("unchecked")
    public static ConfigHolder<Config> create(Config defaults) {
        try {
            Class.forName("dev.isxander.yacl3.config.v2.api.ConfigClassHandler");
            return new ConfigHolder<>((ConfigBackend<Config>) Class
                    .forName("com.cleannrooster.dungeons_iso.compat.YaclConfigBackend")
                    .getDeclaredConstructor()
                    .newInstance());
        } catch (Throwable ignored) {
            // No YACL, or a YACL too different to talk to. Settings still persist; only the
            // settings *screen* is unavailable, and ConfigScreen is gated on the same test.
        }
        return new ConfigHolder<>(new GsonConfigBackend<>(Config.class, defaults, "dungeons_iso_v5.json"));
    }

    public T instance() {
        return this.backend.instance();
    }

    public void load() {
        this.backend.load();
    }

    public void save() {
        this.backend.save();
    }

    /** True when the YACL-backed config screen can be built. */
    public boolean hasScreen() {
        return this.backend.handler() != null;
    }

    /** The YACL {@code ConfigClassHandler}, or null. Only {@link ConfigScreen} should call this. */
    public Object handler() {
        return this.backend.handler();
    }
}
