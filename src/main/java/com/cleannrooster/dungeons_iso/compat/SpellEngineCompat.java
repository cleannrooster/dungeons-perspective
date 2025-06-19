package com.cleannrooster.dungeons_iso.compat;

import net.minecraft.client.MinecraftClient;
import net.spell_engine.api.item.weapon.SpellSwordItem;
import net.spell_engine.api.item.weapon.StaffItem;
import net.spell_engine.internals.casting.SpellCasterClient;

public class SpellEngineCompat {
    public static boolean isCasting(){

        return MinecraftClient.getInstance().player != null
                && ((SpellCasterClient)MinecraftClient.getInstance().player).getCurrentSpell() != null;
    }

    public static boolean isHoldingStaff(){

        return MinecraftClient.getInstance().player != null
                && MinecraftClient.getInstance().player.getMainHandStack().getItem() instanceof StaffItem;
    }
}
