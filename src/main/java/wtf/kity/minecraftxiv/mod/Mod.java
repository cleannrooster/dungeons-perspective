package wtf.kity.minecraftxiv.mod;

import net.minecraft.client.option.Perspective;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.HitResult;

public class Mod {
    public static float yaw;
    public static float pitch;
    public static float zoom = 1.0f;
    public static boolean enabled = false;
    public static Perspective lastPerspective;
    public static HitResult crosshairTarget;
    public static Entity lockOnTarget;
}