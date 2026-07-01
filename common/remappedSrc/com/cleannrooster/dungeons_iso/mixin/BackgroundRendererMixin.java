package com.cleannrooster.dungeons_iso.mixin;

import com.cleannrooster.dungeons_iso.mod.Mod;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.mojang.blaze3d.systems.RenderSystem.assertOnRenderThread;

import F;

@Mixin(FogRenderer.class)
public class BackgroundRendererMixin {
    @Shadow
    private static float red;
    @Shadow
    private static float green;
    @Shadow
    private static float blue;
    @Inject(method = "applyFog", at = @At("HEAD"), cancellable = true)
    private static void applyFogFogOfWar(Camera camera, FogRenderer.FogMode fogType, float viewDistance, boolean thickFog, float tickDelta, CallbackInfo ci) {
        if(Mod.enabled && Minecraft.getInstance().getCameraEntity() != null && Minecraft.getInstance().getCameraEntity() instanceof LivingEntity living && living.hasEffect(MobEffects.DARKNESS)){
            ci.cancel();
        }

    }
    @Inject(method = "render", at = @At("TAIL"), cancellable = true)
    private static void renderXIV(Camera camera, float tickDelta, ClientLevel world, int viewDistance, float skyDarkness,CallbackInfo ci) {
        if(Mod.enabled) {

        }
    }
        @Inject(method = "applyFog", at = @At("TAIL"), cancellable = true)
    private static void applyFogFogOfWarEND(Camera camera, FogRenderer.FogMode fogType, float viewDistance, boolean thickFog, float tickDelta, CallbackInfo ci) {
        if(Mod.enabled){
            var flot = (Mod.zoomMetric*(Mod.getZoom()+1) +(float)scal);
            RenderSystem.setShaderFogStart(flot*1.5F);
            RenderSystem.setShaderFogEnd(flot*3F);


        }

    }

    private static float scal = 0;
    private static void method(){
        var camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        var entity = Minecraft.getInstance().cameraEntity;
        Vector3d forward = camera.rotation().transform(new Vector3d(0.0, 0.0, -1.0));
        Vector3d right = camera.rotation().transform(new Vector3d(1.0, 0.0, 0.0));
        Vector3d up = camera.rotation().transform(new Vector3d(0.0, 1.0, 0.0));
        Vector3d orth =  camera.rotation().transform(new Vector3d(0.0, 0.0, -1.0)).normalize();
        Vector3d dir =  forward.add(right.mul(0).add(up.mul(0))).normalize();
        var dirScal = dir.mul(2*Minecraft.getInstance().gameRenderer.getDepthFar());
        var end =  entity.position().add(dirScal.x,dirScal.y,dirScal.z);
        var hit = Minecraft.getInstance().cameraEntity.level().clip(new ClipContext( entity.position(),end, ClipContext.Block.VISUAL, ClipContext.Fluid.ANY,entity));
        scal = (float) hit.getLocation().distanceTo(entity.position());
    }
}
