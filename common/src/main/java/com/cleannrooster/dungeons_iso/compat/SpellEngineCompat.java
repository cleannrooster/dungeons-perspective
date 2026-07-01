package com.cleannrooster.dungeons_iso.compat;

// Spell Engine/Power classes not available for 26.2; returns safe defaults until updated.
public class SpellEngineCompat {
    public static boolean isCasting() {
        return false;
    }

    public static boolean isHoldingStaff() {
        return false;
    }
}
