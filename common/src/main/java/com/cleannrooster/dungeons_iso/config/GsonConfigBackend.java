package com.cleannrooster.dungeons_iso.config;

import com.cleannrooster.dungeons_iso.ModCompat;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Config persistence without YACL, using the Gson that Minecraft already ships.
 *
 * <p>Writes the same file YACL's serialiser writes. Every instance field on {@link Config} carries
 * {@code @SerialEntry}, and Gson serialises exactly the non-static non-transient instance fields,
 * so the two produce the same key set — settings survive installing or removing YACL. (The
 * annotation itself cannot be read here: with YACL absent the JVM silently drops annotations whose
 * class is missing, so there is nothing to filter on even if we wanted to.)
 */
public final class GsonConfigBackend<T> implements ConfigBackend<T> {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Class<T> type;
    private final T instance;
    private final Path path;

    public GsonConfigBackend(Class<T> type, T instance, String fileName) {
        this.type = type;
        this.instance = instance;
        this.path = ModCompat.getConfigDir().resolve(fileName);
    }

    @Override
    public T instance() {
        return this.instance;
    }

    @Override
    public void load() {
        if (!Files.isRegularFile(this.path)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(this.path)) {
            T loaded = GSON.fromJson(reader, this.type);
            if (loaded != null) {
                // Copied onto the live object rather than swapping it, so any reference taken
                // before load() keeps seeing current values. Keys absent from the file simply
                // never get copied, leaving the field default in place.
                copyFields(loaded, this.instance);
            }
        } catch (Exception ignored) {
            // A corrupt or half-written file leaves the defaults standing, which is the same
            // outcome YACL produces and strictly better than refusing to start.
        }
    }

    @Override
    public void save() {
        try {
            Files.createDirectories(this.path.getParent());
            try (Writer writer = Files.newBufferedWriter(this.path)) {
                GSON.toJson(this.instance, writer);
            }
        } catch (IOException ignored) {
        }
    }

    @Override
    public Object handler() {
        return null;
    }

    private void copyFields(T from, T to) {
        for (Field field : this.type.getDeclaredFields()) {
            int mods = field.getModifiers();
            if (Modifier.isStatic(mods) || Modifier.isTransient(mods)) {
                continue;
            }
            try {
                field.setAccessible(true);
                field.set(to, field.get(from));
            } catch (Exception ignored) {
            }
        }
    }
}
