package com.cleannrooster.dungeons_iso.compat;

import net.caffeinemc.mods.sodium.client.render.chunk.RenderSectionManager;

public abstract interface SodiumWorldRendererAccessor {
    RenderSectionManager sectionManager();
}
