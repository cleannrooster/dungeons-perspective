package com.cleannrooster.dungeons_iso.api;

public abstract interface MinecraftClientAccessor {
    int getMouseCooldown();
    boolean shouldRebuild();

    int setMouseCooldown(int cooldown);
}