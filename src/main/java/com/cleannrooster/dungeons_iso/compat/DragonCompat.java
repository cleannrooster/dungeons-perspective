package com.cleannrooster.dungeons_iso.compat;

import com.cleannrooster.dungeons_iso.config.Config;
import com.cleannrooster.dungeons_iso.mod.Mod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;

public class DragonCompat {
    public static boolean bool;

    public static void getDragonDistanceMultiplier(){
         bool = false;
        for (Entity entity: MinecraftClient.getInstance().world.getEntities()){
            if(entity.getType().getTranslationKey().contains("dungeons-dragon")  ){
                bool = true;

            }
        }
        if(bool){
            Mod.dragonTime = MinecraftClient.getInstance().world.getTime();
        }
        else{

            Mod.dragonTimeSince = MinecraftClient.getInstance().world.getTime();
        }
    }
}
