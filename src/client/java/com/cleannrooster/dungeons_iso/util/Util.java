package com.cleannrooster.dungeons_iso.util;

import com.cleannrooster.dungeons_iso.ModCompat;
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
}
