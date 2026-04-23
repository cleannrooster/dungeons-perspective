package com.cleannrooster.dungeons_iso.mixin;

import com.cleannrooster.dungeons_iso.mod.Mod;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FogRenderer.class)
public class BackgroundRendererMixin {

    // MC 26.1 (Vibrant Visuals): BackgroundRenderer was replaced by FogRenderer.
    // The applyFog signature changed; require=0 so missing targets fail silently.
    @Inject(method = "applyFog", at = @At("HEAD"), cancellable = true, require = 0)
    private static void applyFogFogOfWar(Camera camera, CallbackInfo ci) {
        if (Mod.enabled && Minecraft.getInstance().getCameraEntity() != null
                && Minecraft.getInstance().getCameraEntity() instanceof LivingEntity living
                && living.hasEffect(MobEffects.DARKNESS)) {
            ci.cancel();
        }
    }

    private static float scal = 0;

    @Inject(method = "applyFog", at = @At("TAIL"), require = 0)
    private static void applyFogFogOfWarEND(Camera camera, CallbackInfo ci) {
        if (Mod.enabled) {
            var flot = (Mod.zoomMetric * (Mod.getZoom() + 1) + (float) scal);
            // TODO-26.1: RenderSystem.setShaderFogStart/End were removed in 26.1 Vibrant Visuals.
            // Fog distance control needs to be reimplemented using the new FogRenderer modifier system.
            // RenderSystem.setShaderFogStart(flot * 1.5F);
            // RenderSystem.setShaderFogEnd(flot * 3F);
        }
    }

    private static void method() {
        var camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        var entity = Minecraft.getInstance().cameraEntity;
        Vector3d forward = camera.getRotation().transform(new Vector3d(0.0, 0.0, -1.0));
        Vector3d right = camera.getRotation().transform(new Vector3d(1.0, 0.0, 0.0));
        Vector3d up = camera.getRotation().transform(new Vector3d(0.0, 1.0, 0.0));
        Vector3d dir = forward.add(right.mul(0).add(up.mul(0))).normalize();
        var dirScal = dir.mul(2 * Minecraft.getInstance().gameRenderer.getFarPlaneDistance());
        var end = entity.getPos().add(dirScal.x, dirScal.y, dirScal.z);
        var hit = Minecraft.getInstance().cameraEntity.level().clip(new ClipContext(entity.getPos(), end, ClipContext.ShapeType.VISUAL, ClipContext.FluidHandling.ANY, entity));
        scal = (float) hit.getPos().distanceTo(entity.getPos());
    }
}
