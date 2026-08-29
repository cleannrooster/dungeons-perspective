package com.cleannrooster.dungeons_iso.config;

public final class ConfigHolder<T> {
    private final ConfigBackend<T> backend;

    private ConfigHolder(ConfigBackend<T> backend) {
        this.backend = backend;
    }

    @SuppressWarnings("unchecked")
    public static ConfigHolder<Config> create(Config defaults) {
        try {
            Class.forName("dev.isxander.yacl3.config.v2.api.ConfigClassHandler");
            return new ConfigHolder<>((ConfigBackend<Config>) Class
                    .forName("com.cleannrooster.dungeons_iso.compat.YaclConfigBackend")
                    .getDeclaredConstructor().newInstance());
        } catch (Throwable ignored) {
        }
        return new ConfigHolder<>(new GsonConfigBackend<>(Config.class, defaults, "dungeons_iso_v5.json"));
    }

    public T instance() { return backend.instance(); }
    public void load() { backend.load(); }
    public void save() { backend.save(); }
    public boolean hasScreen() { return backend.handler() != null; }
    public Object handler() { return backend.handler(); }
}
