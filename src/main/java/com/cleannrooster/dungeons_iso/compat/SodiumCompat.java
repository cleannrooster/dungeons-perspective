package com.cleannrooster.dungeons_iso.compat;

import com.cleannrooster.dungeons_iso.api.MinecraftClientAccessor;
import it.unimi.dsi.fastutil.longs.LongCollection;
import net.caffeinemc.mods.sodium.client.SodiumClientMod;
import net.caffeinemc.mods.sodium.client.gl.device.RenderDevice;
import net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSectionManager;
import net.caffeinemc.mods.sodium.client.render.chunk.map.ChunkTracker;
import net.caffeinemc.mods.sodium.client.render.chunk.map.ChunkTrackerHolder;
import net.caffeinemc.mods.sodium.client.render.frapi.SodiumRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Box;
import org.spongepowered.asm.mixin.Shadow;

public class SodiumCompat {

    public static void run(){

        Box box = new Box(MinecraftClient.getInstance().player.getEyePos(),MinecraftClient.getInstance().gameRenderer.getCamera().getPos()).expand(1,0,1);
            SodiumWorldRenderer.instance().scheduleRebuildForBlockArea((int) box.minX - 8, (int) box.minY - 8, (int) box.minZ - 8, (int) box.maxX + 8, (int) box.maxY + 8, (int) box.maxZ + 8, true);
        if(!((MinecraftClientAccessor)MinecraftClient.getInstance()).shouldRebuild()){
            if(SodiumClientMod.options().performance.alwaysDeferChunkUpdates) {
                SodiumClientMod.options().performance.alwaysDeferChunkUpdates = false;
            }

        }
        else{
            if(!SodiumClientMod.options().performance.alwaysDeferChunkUpdates) {
                SodiumClientMod.options().performance.alwaysDeferChunkUpdates = true;
            }

        }
    }
}
