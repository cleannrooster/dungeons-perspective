package com.cleannrooster.dungeons_iso.mixin;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

// 26.2: the active Screen moved from a public Minecraft#screen field into a private Gui#screen field.
@Mixin(Gui.class)
public interface GuiScreenAccessor {
    @Accessor("screen")
    Screen getScreen();
}
