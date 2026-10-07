package com.cleannrooster.dungeons_iso.mixin;

import com.cleannrooster.dungeons_iso.api.Ortho;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.VertexSorter;
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
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.RaycastContext;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Unique;
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

            // clipMetric is zero while the first world frame is being prepared. JOML accepts a
            // zero near plane, but the resulting projection has an invalid frustum. Vanilla then
            // loops forever in Frustum#coverBoxAroundSetPosition trying to expand that frustum,
            // which presents as the client hanging on the world-login screen.
            float nearPlane = Math.max(0.05F, 0.05F * Mod.clipMetric);
            cir.setReturnValue(matrix4f.perspective(
                    (float) (fov * 0.01745329238474369),
                    (float) MinecraftClient.getInstance().getWindow().getFramebufferWidth()
                            / (float) MinecraftClient.getInstance().getWindow().getFramebufferHeight(),
                    nearPlane,
                    MinecraftClient.getInstance().gameRenderer.getFarPlaneDistance()));
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

    /**
     * Decouple interaction targeting from the view vector.
     *
     * updateCrosshairTarget writes MinecraftClient#crosshairTarget and #targetedEntity from the
     * camera entity's rotation. In this mod the player's rotation is a cosmetic "look at" driven by
     * the mouse target, so we overwrite the pick result at TAIL with the mouse target
     * ({@link Mod#crosshairTarget}) directly. Placement, mining, use and attack then land exactly
     * where the cursor points, independent of head pitch.
     *
     * A target is accepted only when it is both in reach and in line of sight from the player's eye;
     * otherwise it becomes a MISS. The mouse ray originates at the top-down camera, so it can select
     * things the player's eye cannot reach in a straight line (e.g. a chest behind a wall, visible
     * from above) — walking to those is handled by click-to-move, not direct interaction.
     *
     *  - Reach is geometry-aware: squared distance from the eye to the nearest point of the target's
     *    bounding box (Box#squaredDistanceTo), matching vanilla's interaction-range checks, so a
     *    big/near hitbox is reachable at its edge.
     *  - Line of sight is a COLLIDER raycast from the eye to the hit point; a non-solid interactable
     *    (lever, button, plant) is never treated as its own occluder, so it stays usable.
     */
    @Inject(method = "updateCrosshairTarget", at = @At("TAIL"))
    private void decoupleHitResultXIV(float tickDelta, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!Mod.enabled || client.player == null || client.world == null) {
            return;
        }
        Vec3d playerEye = client.player.getEyePos();

        // When contextual combat targeting has acquired an entity, make the interaction hit result
        // point at that entity too — so vanilla attacks/interactions land on the same enemy the
        // character faces, not just the cosmetic facing. Gated by the same reach + line-of-sight
        // check as any other target; if the contextual target is out of reach or occluded, fall
        // through to the normal cursor-based result below.
        if (Config.GSON.instance().isContextualTargeting()
                && Mod.targeted instanceof LivingEntity contextTarget && contextTarget.isAlive()) {
            EntityHitResult contextHit = new EntityHitResult(contextTarget, contextTarget.getEyePos());
            if (client.player.canInteractWithEntity(contextTarget, 0.0)
                    && !isOccludedFromEye(client, playerEye, contextHit.getPos(), null)) {
                client.crosshairTarget = contextHit;
                client.targetedEntity = contextTarget;
                return;
            }
        }

        HitResult target = Mod.crosshairTarget;
        if (target == null) {
            return;
        }
        Vec3d eye = client.player.getEyePos();
        if (target instanceof EntityHitResult entityHit) {
            // canInteractWithEntity measures to the entity's bounding box (geometry-aware), honoring
            // getEntityInteractionRange().
            if (client.player.canInteractWithEntity(entityHit.getEntity(), 0.0)
                    && !isOccludedFromEye(client, eye, entityHit.getPos(), null)) {
                client.crosshairTarget = target;
                client.targetedEntity = entityHit.getEntity();
                return;
            }
        } else if (target instanceof BlockHitResult blockHit && blockHit.getType() == HitResult.Type.BLOCK) {
            // canInteractWithBlockAt measures to the block's box (geometry-aware), honoring
            // getBlockInteractionRange().
            if (client.player.canInteractWithBlockAt(blockHit.getBlockPos(), 0.0)
                    && !isOccludedFromEye(client, eye, blockHit.getPos(), blockHit.getBlockPos())) {
                client.crosshairTarget = target;
                client.targetedEntity = null;
                return;
            }
        }
        // Out of reach, occluded, or already a miss: interact with nothing.
        client.crosshairTarget = BlockHitResult.createMissed(eye, client.player.getHorizontalFacing(), BlockPos.ofFloored(eye));
        client.targetedEntity = null;
    }

    /**
     * True if a solid block sits between {@code eye} and {@code targetPoint}, breaking line of sight.
     * {@code targetPos}, when non-null, is the block being targeted and is excluded from counting as
     * its own occluder (a COLLIDER raycast toward a solid target block would otherwise hit the
     * target's near face at a shorter distance than the mouse-ray hit point and false-positive). A
     * non-solid target block produces a miss raycast (the ray passes through it) → not occluded.
     */
    @Unique
    private boolean isOccludedFromEye(MinecraftClient client, Vec3d eye, Vec3d targetPoint, @Nullable BlockPos targetPos) {
        BlockHitResult los = client.world.raycast(new RaycastContext(
                eye, targetPoint, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, client.player));
        if (los.getType() != HitResult.Type.BLOCK) {
            return false;
        }
        if (targetPos != null && los.getBlockPos().equals(targetPos)) {
            return false;
        }
        return eye.squaredDistanceTo(los.getPos()) < eye.squaredDistanceTo(targetPoint) - 1.0E-4;
    }
}
