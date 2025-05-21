package com.cleannrooster.dungeons_iso.mod;

import com.cleannrooster.dungeons_iso.config.Config;
import net.minecraft.client.option.Perspective;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.HitResult;

public class Mod {
    public static float yaw;
    public static float pitch;
    public static double x;
    public static double flyingYAddition;

    public static double z;
    public static float zoom = 5.0F;
    public static boolean enabled = false;
    public static Perspective lastPerspective;
    public static HitResult crosshairTarget;
    public static HitResult prevCrosshairTarget;
    public static Entity lockOnTarget;
}