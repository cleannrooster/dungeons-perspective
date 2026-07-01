package com.cleannrooster.dungeons_iso.compat;

import net.minecraft.client.Minecraft;
import net.spell_engine.api.item.weapon.SpellSwordItem;
import net.spell_engine.api.item.weapon.StaffItem;
import net.spell_engine.internals.casting.SpellCasterEntity;

public class SpellEngineCompat {
    public static boolean isCasting(){

        return Minecraft.getInstance().player != null
                && ((SpellCasterEntity)Minecraft.getInstance().player).isCastingSpell();
    }

    public static boolean isHoldingStaff(){

        return Minecraft.getInstance().player != null
                && Minecraft.getInstance().player.getMainHandItem().getItem() instanceof StaffItem;
    }
}
