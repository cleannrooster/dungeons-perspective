package com.cleannrooster.dungeons_iso.mixin;

import com.cleannrooster.dungeons_iso.mod.Mod;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.world.level.ClipContext;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// 26.2: the fog system was rewritten. FogRenderer#applyFog/render and the static red/green/blue
// fields no longer exist; fog is now described by a mutable FogData returned from setupFog, and
// RenderSystem#setShaderFogStart/End were removed. We adjust the returned FogData to push the fog
// band out for the isometric zoom. (Darkness suppression now lives in LightmapTextureManagerMixin,
// which targets DarknessFogEnvironment.)
@Mixin(FogRenderer.class)
public class BackgroundRendererMixin {

    @Inject(method = "setupFog", at = @At("RETURN"))
    private void setupFogXIV(Camera camera, int renderDistance, DeltaTracker deltaTracker, float skyDarkness, ClientLevel level, CallbackInfoReturnable<FogData> cir) {
        if (Mod.enabled) {
            FogData data = cir.getReturnValue();
            if (data == null) {
                return;
            }
            var flot = (Mod.zoomMetric * (Mod.getZoom() + 1) + (float) scal);
            data.environmentalStart = flot * 1.5F;
            data.environmentalEnd = flot * 3F;
            data.renderDistanceStart = flot * 1.5F;
            data.renderDistanceEnd = flot * 3F;
        }
    }

    private static float scal = 0;
    private static void method(){
        var camera = Minecraft.getInstance().gameRenderer.mainCamera();
        var entity = Minecraft.getInstance().getCameraEntity();
        Vector3d forward = camera.rotation().transform(new Vector3d(0.0, 0.0, -1.0));
        Vector3d right = camera.rotation().transform(new Vector3d(1.0, 0.0, 0.0));
        Vector3d up = camera.rotation().transform(new Vector3d(0.0, 1.0, 0.0));
        Vector3d orth =  camera.rotation().transform(new Vector3d(0.0, 0.0, -1.0)).normalize();
        Vector3d dir =  forward.add(right.mul(0).add(up.mul(0))).normalize();
        var dirScal = dir.mul(2*(float)(Minecraft.getInstance().options.getEffectiveRenderDistance() * 16));
        var end =  entity.position().add(dirScal.x,dirScal.y,dirScal.z);
        var hit = Minecraft.getInstance().getCameraEntity().level().clip(new ClipContext( entity.position(),end, ClipContext.Block.VISUAL, ClipContext.Fluid.ANY,entity));
        scal = (float) hit.getLocation().distanceTo(entity.position());
    }
}
