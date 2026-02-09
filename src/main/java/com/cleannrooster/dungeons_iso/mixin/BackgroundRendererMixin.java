package com.cleannrooster.dungeons_iso.mixin;

import com.cleannrooster.dungeons_iso.mod.Mod;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.world.RaycastContext;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.mojang.blaze3d.systems.RenderSystem.assertOnRenderThread;

@Mixin(BackgroundRenderer.class)
public class BackgroundRendererMixin {
    @Shadow
    private static float red;
    @Shadow
    private static float green;
    @Shadow
    private static float blue;
    @Inject(method = "applyFog", at = @At("HEAD"), cancellable = true)
    private static void applyFogFogOfWar(Camera camera, BackgroundRenderer.FogType fogType, float viewDistance, boolean thickFog, float tickDelta, CallbackInfo ci) {
        if(Mod.enabled && MinecraftClient.getInstance().getCameraEntity() != null && MinecraftClient.getInstance().getCameraEntity() instanceof LivingEntity living && living.hasStatusEffect(StatusEffects.DARKNESS)){
            ci.cancel();
        }

    }
    @Inject(method = "render", at = @At("TAIL"), cancellable = true)
    private static void renderXIV(Camera camera, float tickDelta, ClientWorld world, int viewDistance, float skyDarkness,CallbackInfo ci) {
        if(Mod.enabled) {

        }
    }
        @Inject(method = "applyFog", at = @At("TAIL"), cancellable = true)
    private static void applyFogFogOfWarEND(Camera camera, BackgroundRenderer.FogType fogType, float viewDistance, boolean thickFog, float tickDelta, CallbackInfo ci) {
        if(Mod.enabled){
            var flot = (Mod.zoomMetric*(Mod.getZoom()+1) +(float)scal);
            RenderSystem.setShaderFogStart(flot*1.5F);
            RenderSystem.setShaderFogEnd(flot*3F);


        }

    }

    private static float scal = 0;
    private static void method(){
        var camera = MinecraftClient.getInstance().gameRenderer.getCamera();
        var entity = MinecraftClient.getInstance().cameraEntity;
        Vector3d forward = camera.getRotation().transform(new Vector3d(0.0, 0.0, -1.0));
        Vector3d right = camera.getRotation().transform(new Vector3d(1.0, 0.0, 0.0));
        Vector3d up = camera.getRotation().transform(new Vector3d(0.0, 1.0, 0.0));
        Vector3d orth =  camera.getRotation().transform(new Vector3d(0.0, 0.0, -1.0)).normalize();
        Vector3d dir =  forward.add(right.mul(0).add(up.mul(0))).normalize();
        var dirScal = dir.mul(2*MinecraftClient.getInstance().gameRenderer.getFarPlaneDistance());
        var end =  entity.getPos().add(dirScal.x,dirScal.y,dirScal.z);
        var hit = MinecraftClient.getInstance().cameraEntity.getWorld().raycast(new RaycastContext( entity.getPos(),end, RaycastContext.ShapeType.VISUAL, RaycastContext.FluidHandling.ANY,entity));
        scal = (float) hit.getPos().distanceTo(entity.getPos());
    }
}
