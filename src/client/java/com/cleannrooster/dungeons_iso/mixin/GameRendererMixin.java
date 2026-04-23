package com.cleannrooster.dungeons_iso.mixin;

import com.cleannrooster.dungeons_iso.api.Ortho;
import com.cleannrooster.dungeons_iso.config.Config;
import com.cleannrooster.dungeons_iso.mod.Mod;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(method = "shouldRenderBlockOutline", at = @At("HEAD"),cancellable = true)

    private void shouldRenderBlockOutlineXIV(CallbackInfoReturnable<Boolean>cir) {
        if (Mod.enabled && Mod.crosshairTarget instanceof BlockHitResult result) {
            cir.setReturnValue(true);
        }
    }

    // MC 26.1 (1.21.11+): setupFrustum signature changed from
    //   setupFrustum(Vec3, Matrix4f, Matrix4f) void
    // to:
    //   setupFrustum(Matrix4f, Matrix4f, Vec3) Frustum
    // The projMatrix moved from index 2 to index 1.
    @ModifyArg(
            method = "renderWorld",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/LevelRenderer;setupFrustum(Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lnet/minecraft/util/math/Vec3;)Lnet/minecraft/client/render/Frustum;"
            ),
            index = 1,
            require = 0
    )

    private Matrix4f orthoFrustumProjMat(Matrix4f projMat) {
        if (Config.GSON.instance().ortho && Mod.enabled) {
            return Ortho.createOrthoMatrix(1.0F, 20.0F);
        }

        return projMat;
    }


    // MC 26.1 (1.21.11+): getBasicProjectionMatrix parameter changed from double to float
    @Inject(method = "getBasicProjectionMatrix", at = @At("HEAD"),cancellable = true)

    public void getBasicProjectionMatrixXIV(float fov, CallbackInfoReturnable<Matrix4f> cir) {
        if(Mod.enabled && Config.GSON.instance().frustumCulling) {
            Matrix4f matrix4f = new Matrix4f();
            if (this.zoom != 1.0F) {
                matrix4f.translate(this.zoomX, -this.zoomY, 0.0F);
                matrix4f.scale(this.zoom, this.zoom, 1.0F);
            }
            Mod.factorScale = Math.max(0F,( 1F-Math.max(Mod.zoomTime , 0F)))*Config.GSON.instance().zNearFactor;
            float mod = 1;
            float mod2 = 1;
            if(Minecraft.getInstance().cameraEntity instanceof LivingEntity living){
                mod = (float) living.getBoundingBox().getLengthY();
                mod2 = (float) living.getBoundingBox().getLengthY();

            }
            HitResult result = Minecraft.getInstance().player.level().clip(
                    new ClipContext(
                            Minecraft.getInstance().player.getEyePos(),Minecraft.getInstance().gameRenderer.getMainCamera().getPos(), ClipContext.ShapeType.VISUAL, ClipContext.FluidHandling.NONE,Minecraft.getInstance().cameraEntity));
            Mod.factor = Math.max(0F,( 1F-Math.max(Mod.zoomTime , 0F)))*(float) ((float) Mod.getZoom()*Mod.zoomMetric - Math.max(Minecraft.getInstance().cameraEntity.getHeight(),result.getPos().distanceTo(Minecraft.getInstance().cameraEntity.getEyePos())));
            Mod.factor2 = Math.clamp((Mod.frustrumZoom+(Mod.shouldReload ?1F : -1F )*Minecraft.getInstance().gameRenderer.getMainCamera().getLastTickDelta())/20F,0.1F,1F) *(float) ((float) Mod.getZoom()*Mod.zoomMetric-Mod.clipMetric -0.15F );

            cir.setReturnValue( matrix4f.perspective((float) (fov * 0.01745329238474369) , (float)Minecraft.getInstance().getWindow().getFramebufferWidth() / (float)Minecraft.getInstance().getWindow().getFramebufferHeight(),((0.05F*Mod.clipMetric)), Minecraft.getInstance().gameRenderer.getFarPlaneDistance()));
        }
    }
    @Shadow
    private float zoom;
    @Shadow
    private float zoomX;
    @Shadow
    private float zoomY;
    public Matrix4f projMatrixCleann(float fov, float znear) {
        Matrix4f matrix4f = new Matrix4f();
        if (this.zoom != 1.0F) {
            matrix4f.translate(this.zoomX, -this.zoomY, 0.0F);
            matrix4f.scale(this.zoom, this.zoom, 1.0F);
        }

        return matrix4f.perspective((float)(fov * 0.01745329238474369), (float)Minecraft.getInstance().getWindow().getFramebufferWidth() / (float)Minecraft.getInstance().getWindow().getFramebufferHeight(),Mod.getZoom(), Minecraft.getInstance().gameRenderer.getFarPlaneDistance());
    }

    // MC 26.1 (1.21.11+): LevelRenderer.render signature changed significantly.
    // New signature: render(ObjectAllocator, RenderTickCounter, boolean, Camera,
    //                       Matrix4f, Matrix4f, Matrix4f, GpuBufferSlice, Vector4f, boolean)
    // The projectionMatrix (what we want to inject into) is now at index 6 (unchanged).
    @ModifyArg(
            method = "renderWorld",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/LevelRenderer;render(Lnet/minecraft/client/gl/ObjectAllocator;Lnet/minecraft/client/render/RenderTickCounter;ZLnet/minecraft/client/render/Camera;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lorg/joml/Vector4f;Z)V"

            ),
            index = 6,
            require = 0
    )
    private Matrix4f orthoProjMat(Matrix4f projMat, @Local(argsOnly = true) DeltaTracker tickCounter) {
        if (Config.GSON.instance().ortho && Mod.enabled) {
            Matrix4f mat = Ortho.createOrthoMatrix(tickCounter.getGameTimeDeltaPartialTick(false), 0.0F);
            RenderSystem.setProjectionMatrix(mat, VertexSorting.DISTANCE_TO_ORIGIN);
            return mat;
        }

        return projMat;
    }
}
