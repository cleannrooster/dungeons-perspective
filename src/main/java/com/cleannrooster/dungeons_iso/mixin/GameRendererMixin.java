package com.cleannrooster.dungeons_iso.mixin;

import com.cleannrooster.dungeons_iso.api.Ortho;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.VertexSorter;
import net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer;
import net.caffeinemc.mods.sodium.client.render.frapi.SodiumRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ChunkTicketManager;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.RaycastContext;
import net.spell_engine.api.spell.Spell;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.cleannrooster.dungeons_iso.ClientInit;
import com.cleannrooster.dungeons_iso.config.Config;
import com.cleannrooster.dungeons_iso.mod.Mod;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(method = "shouldRenderBlockOutline", at = @At("HEAD"),cancellable = true)

    private void shouldRenderBlockOutlineXIV(CallbackInfoReturnable<Boolean>cir) {
        if (Mod.enabled && Mod.crosshairTarget instanceof BlockHitResult result) {
            cir.setReturnValue(true);
        }
    }
    @ModifyArg(
            method = "renderWorld",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/WorldRenderer;setupFrustum(Lnet/minecraft/util/math/Vec3d;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V"
            ),
            index = 2
    )

    private Matrix4f orthoFrustumProjMat(Matrix4f projMat) {
        if (Config.GSON.instance().ortho && Mod.enabled) {
            return Ortho.createOrthoMatrix(1.0F, 20.0F);
        }

        return projMat;
    }


    @Inject(method = "getBasicProjectionMatrix", at = @At("HEAD"),cancellable = true)

    public void getBasicProjectionMatrixXIV(double fov, CallbackInfoReturnable<Matrix4f> cir) {
        if(Mod.enabled && Config.GSON.instance().frustumCulling) {
            Matrix4f matrix4f = new Matrix4f();
            if (this.zoom != 1.0F) {
                matrix4f.translate(this.zoomX, -this.zoomY, 0.0F);
                matrix4f.scale(this.zoom, this.zoom, 1.0F);
            }
            Mod.factorScale = Math.max(0F,( 1F-Math.max(Mod.zoomTime , 0F)))*Config.GSON.instance().zNearFactor;
            float mod = 1;
            float mod2 = 1;
            if(MinecraftClient.getInstance().cameraEntity instanceof LivingEntity living){
                mod = (float) living.getBoundingBox().getLengthY();
                mod2 = (float) living.getBoundingBox().getLengthY();

            }
            HitResult result = MinecraftClient.getInstance().player.getWorld().raycast(
                    new RaycastContext(
                            MinecraftClient.getInstance().player.getEyePos(),MinecraftClient.getInstance().gameRenderer.getCamera().getPos(), RaycastContext.ShapeType.VISUAL, RaycastContext.FluidHandling.NONE,MinecraftClient.getInstance().cameraEntity));
            Mod.factor = Math.max(0F,( 1F-Math.max(Mod.zoomTime , 0F)))*(float) ((float) Mod.getZoom()*Mod.zoomMetric - Math.max(MinecraftClient.getInstance().cameraEntity.getHeight(),result.getPos().distanceTo(MinecraftClient.getInstance().cameraEntity.getEyePos())));
            Mod.factor2 = Math.clamp((Mod.frustrumZoom+(Mod.shouldReload ?1F : -1F )*MinecraftClient.getInstance().gameRenderer.getCamera().getLastTickDelta())/20F,0.1F,1F) *(float) ((float) Mod.getZoom()*Mod.zoomMetric-Mod.clipMetric -0.15F );

            cir.setReturnValue( matrix4f.perspective((float) (fov * 0.01745329238474369) , (float)MinecraftClient.getInstance().getWindow().getFramebufferWidth() / (float)MinecraftClient.getInstance().getWindow().getFramebufferHeight(),((0.25F*Mod.clipMetric)), MinecraftClient.getInstance().gameRenderer.getFarPlaneDistance()));
        }
    }
    @Shadow
    private float zoom;
    @Shadow
    private float zoomX;
    @Shadow
    private float zoomY;
    public Matrix4f projMatrixCleann(double fov,float znear) {
        Matrix4f matrix4f = new Matrix4f();
        if (this.zoom != 1.0F) {
            matrix4f.translate(this.zoomX, -this.zoomY, 0.0F);
            matrix4f.scale(this.zoom, this.zoom, 1.0F);
        }

        return matrix4f.perspective((float)(fov * 0.01745329238474369), (float)MinecraftClient.getInstance().getWindow().getFramebufferWidth() / (float)MinecraftClient.getInstance().getWindow().getFramebufferHeight(),Mod.getZoom(), MinecraftClient.getInstance().gameRenderer.getFarPlaneDistance());
    }
    @ModifyArg(
            method = "renderWorld",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/WorldRenderer;render(Lnet/minecraft/client/render/RenderTickCounter;ZLnet/minecraft/client/render/Camera;Lnet/minecraft/client/render/GameRenderer;Lnet/minecraft/client/render/LightmapTextureManager;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V"

            ),
            index = 6
    )
    private Matrix4f orthoProjMat(Matrix4f projMat, @Local(argsOnly = true) RenderTickCounter tickCounter) {
        if (Config.GSON.instance().ortho && Mod.enabled) {
            Matrix4f mat = Ortho.createOrthoMatrix(tickCounter.getTickDelta(false), 0.0F);
            RenderSystem.setProjectionMatrix(mat, VertexSorter.BY_Z);
            return mat;
        }

        return projMat;
    }
}
