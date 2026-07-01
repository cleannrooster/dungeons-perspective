package com.cleannrooster.dungeons_iso.api;

import net.minecraft.world.phys.Vec3;

public abstract interface CameraAccessor {
     void setPosInterfae(Vec3 vec3d);
     Vec3 getPosBeforeModulation();
}