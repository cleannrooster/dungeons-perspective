package com.cleannrooster.dungeons_iso.util;

import com.cleannrooster.dungeons_iso.ModCompat;
import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.LogManager;

/**
 * @author ChloeCDN
 */
public class Util {

    public static void debug(String s) {
        if (ModCompat.isDevelopmentEnvironment()) {
            LogManager.getLogger("Minecraft XIV").info(s);
        }
    }

    /** Rebuilds chunk geometry without tearing down the active level renderer. */
    public static void refreshLevelRenderer() {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) {
            return;
        }
        client.levelRenderer.invalidateCompiledGeometry(client.level, client.options,
                client.gameRenderer.mainCamera(), client.getBlockColors());
    }
}
