package com.cleannrooster.dungeons_iso.mixin;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GameRenderer.class)
public interface GameRendererAccessor {
    @Invoker("getFov")
    float callGetFov(Camera camera, float tickDelta, boolean changingFov);

    @Invoker("ensureTargetInRange")
    HitResult callEnsureTargetInRange(HitResult hitResult, Vec3 cameraPos, double interactionRange);

}
