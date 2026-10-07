package com.cleannrooster.dungeons_iso.config;

/**
 * Where the config actually lives on disk.
 *
 * <p>Two implementations: {@code YaclConfigBackend} when YACL is installed, which is the original
 * behaviour untouched, and {@link GsonConfigBackend} when it is not. Both read and write the same
 * file in the same format, so a world can move between the two without losing settings.
 */
public interface ConfigBackend<T> {

    T instance();

    void load();

    void save();

    /**
     * The underlying {@code ConfigClassHandler}, or null when there isn't one.
     *
     * <p>Untyped because naming the type here would drag YACL's classes into a class that has to
     * load without them. Only {@link ConfigScreen} consumes this, and only when YACL is present.
     */
    Object handler();
}
