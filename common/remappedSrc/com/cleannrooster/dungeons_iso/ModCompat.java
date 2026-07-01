package com.cleannrooster.dungeons_iso;

/**
 * Cross-platform mod detection and environment utilities.
 * Works at any point in the startup lifecycle — including mixin application time.
 *
 * Strategy: try Fabric's FabricLoader first (compiles fine since fabric-loader is on
 * the common compile classpath); if it's missing at runtime (NeoForge without FFAPI),
 * NoClassDefFoundError is thrown and caught, then we fall back to NeoForge's ModList
 * via reflection.
 */
public class ModCompat {

    public static boolean isModLoaded(String modId) {
        // Fabric
        try {
            return net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded(modId);
        } catch (Throwable ignored) {}
        // NeoForge
        try {
            Class<?> modListClass = Class.forName("net.neoforged.fml.ModList");
            Object modList = modListClass.getMethod("get").invoke(null);
            if (modList != null) {
                return (boolean) modListClass.getMethod("isLoaded", String.class).invoke(modList, modId);
            }
        } catch (Throwable ignored) {}
        return false;
    }

    public static boolean isDevelopmentEnvironment() {
        // Fabric
        try {
            return net.fabricmc.loader.api.FabricLoader.getInstance().isDevelopmentEnvironment();
        } catch (Throwable ignored) {}
        // NeoForge — FMLLoader.isProduction() returns true when NOT in dev
        try {
            Class<?> fmlLoaderClass = Class.forName("net.neoforged.fml.loading.FMLLoader");
            return !(boolean) fmlLoaderClass.getMethod("isProduction").invoke(null);
        } catch (Throwable ignored) {}
        return false;
    }
}
