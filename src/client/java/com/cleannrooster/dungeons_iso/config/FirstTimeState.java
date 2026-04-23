package com.cleannrooster.dungeons_iso.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.isxander.yacl3.platform.YACLPlatform;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Stores the first-time prompt state in the game root directory (NOT in config/),
 * so modpack updates that overwrite the config folder won't reset this.
 * If the file is absent, choiceMade defaults to false (prompt will show).
 *
 * Game root is resolved as the parent of the config directory, which is
 * &lt;gamedir&gt;/config on both Fabric and NeoForge.
 */
public class FirstTimeState {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = YACLPlatform.getConfigDir().getParent().resolve("dungeons_iso_firsttime.json");

    private static FirstTimeState instance;

    public boolean choiceMade = false;

    public static FirstTimeState get() {
        if (instance == null) {
            load();
        }
        return instance;
    }

    public static void load() {
        if (Files.exists(PATH)) {
            try {
                String json = Files.readString(PATH);
                instance = GSON.fromJson(json, FirstTimeState.class);
                if (instance == null) {
                    instance = new FirstTimeState();
                }
            } catch (Exception e) {
                instance = new FirstTimeState();
            }
        } else {
            instance = new FirstTimeState();
        }
    }

    public static void save() {
        try {
            Files.createDirectories(PATH.getParent());
            Files.writeString(PATH, GSON.toJson(get()));
        } catch (IOException e) {
            throw new RuntimeException("Failed to save first-time state", e);
        }
    }

    public static void reset() {
        get().choiceMade = false;
        save();
    }
}
