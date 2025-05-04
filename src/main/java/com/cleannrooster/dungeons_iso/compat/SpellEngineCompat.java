package com.cleannrooster.dungeons_iso.compat;

import net.minecraft.client.MinecraftClient;
import net.spell_engine.internals.casting.SpellCasterEntity;

public class SpellEngineCompat {
    public static boolean isCasting(){

        return MinecraftClient.getInstance().player != null
                && ((SpellCasterEntity)MinecraftClient.getInstance().player).isCastingSpell();
    }
}
