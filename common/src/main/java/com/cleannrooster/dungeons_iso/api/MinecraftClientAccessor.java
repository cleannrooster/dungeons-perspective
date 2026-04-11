package com.cleannrooster.dungeons_iso.api;

import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;

public abstract interface MinecraftClientAccessor {
    int getMouseCooldown();
    boolean shouldRebuild();
    void setLocation(HitResult vec3d);
    HitResult getLocation();
    Vec3d getOriginalLocation();
    void setOriginalLocation(Vec3d vec3d);

    int setMouseCooldown(int cooldown);
}