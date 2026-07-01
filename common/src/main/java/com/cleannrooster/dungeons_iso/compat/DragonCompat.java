package com.cleannrooster.dungeons_iso.compat;

import com.cleannrooster.dungeons_iso.config.Config;
import com.cleannrooster.dungeons_iso.mod.Mod;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

public class DragonCompat {
    public static boolean bool;

    public static void getDragonDistanceMultiplier(){
         bool = false;
        for (Entity entity: Minecraft.getInstance().level.entitiesForRendering()){
            if(entity.getType().getDescriptionId().contains("dungeons-dragon")  ){
                bool = true;

            }
        }
        if(bool){
            Mod.dragonTime = Minecraft.getInstance().level.getGameTime();
        }
        else{

            Mod.dragonTimeSince = Minecraft.getInstance().level.getGameTime();
        }
    }
}
