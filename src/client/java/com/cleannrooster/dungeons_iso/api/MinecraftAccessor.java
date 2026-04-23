package com.cleannrooster.dungeons_iso.api;

import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public abstract interface MinecraftAccessor {
    int getMouseCooldown();
    boolean shouldRebuild();
    void setLocation(HitResult vec3d);
    HitResult getLocation();
    Vec3 getOriginalLocation();
    void setOriginalLocation(Vec3 vec3d);

    int setMouseCooldown(int cooldown);
}