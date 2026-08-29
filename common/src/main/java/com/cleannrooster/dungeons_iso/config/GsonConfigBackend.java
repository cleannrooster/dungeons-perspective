package com.cleannrooster.dungeons_iso.config;

import com.cleannrooster.dungeons_iso.ModCompat;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;

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

    public T instance() { return instance; }

    public void load() {
        if (!Files.isRegularFile(path)) return;
        try (Reader reader = Files.newBufferedReader(path)) {
            T loaded = GSON.fromJson(reader, type);
            if (loaded != null) copyFields(loaded, instance);
        } catch (Exception ignored) {
        }
    }

    public void save() {
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(instance, writer);
            }
        } catch (Exception ignored) {
        }
    }

    public Object handler() { return null; }

    private void copyFields(T from, T to) {
        for (Field field : type.getDeclaredFields()) {
            int modifiers = field.getModifiers();
            if (Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers)) continue;
            try {
                field.setAccessible(true);
                field.set(to, field.get(from));
            } catch (Exception ignored) {
            }
        }
    }
}
