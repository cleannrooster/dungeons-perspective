package com.cleannrooster.dungeons_iso.api;

public abstract interface MinecraftClientAccessor {
    int getMouseCooldown();
    int setMouseCooldown(int cooldown);
}