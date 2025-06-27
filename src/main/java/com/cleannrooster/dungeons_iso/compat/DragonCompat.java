package com.cleannrooster.dungeons_iso.compat;

import com.cleannrooster.dungeons_iso.config.Config;
import com.cleannrooster.dungeons_iso.mod.Mod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;

public class DragonCompat {

    public static double getDragonDistanceMultiplier(){
        double dub = 0;
        for (Entity entity: MinecraftClient.getInstance().world.getEntities()){
            if(entity.getType().getTranslationKey().contains("dungeons-dragon")  ){
                double d = 1 + entity.distanceTo(MinecraftClient.getInstance().cameraEntity)/5F;
                if(d > dub){
                    dub = d;
                }

            }
        }
        return dub;
    }
}
