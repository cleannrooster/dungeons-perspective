package com.cleannrooster.dungeons_iso.mixin;

import com.cleannrooster.dungeons_iso.api.*;
import com.cleannrooster.dungeons_iso.compat.MidnightControlsCompat;
import com.cleannrooster.dungeons_iso.ModCompat;
import net.minecraft.client.Camera;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.tutorial.Tutorial;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.math.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector2d;
import org.joml.Vector3d;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.cleannrooster.dungeons_iso.ClientInit;
import com.cleannrooster.dungeons_iso.config.Config;
import com.cleannrooster.dungeons_iso.mod.Mod;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import java.util.Iterator;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

@Mixin(MouseHandler.class)
public class MouseMixin implements MouseAccessor {
    @Shadow
    private boolean cursorLocked;
    @Shadow
    private boolean rightButtonClicked;

    @Shadow
    @Final
    private Minecraft client;
    @SuppressWarnings("unused")
    @Shadow
    private boolean hasResolutionChanged;
    @Shadow
    private double x;
    @Shadow
    private double y;


    private static int lockontime;
    @Unique
    @Nullable
    private Double lastX;
    @Unique
    @Nullable
    private Double lastY;


    @Inject(
            method = "lockCursor",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    public void lockCursorXIV(CallbackInfo info) {

        if (client.isWindowActive()) {
            if (!cursorLocked) {
                if (!Minecraft.ON_OSX) {
                    KeyMapping.setAll();
                }

                cursorLocked = true;

                if (Mod.enabled) {
                    // Merely hide the cursor instead of "disabling" it
                    InputConstants.grabOrReleaseMouse(client.getWindow().getWindow(), GLFW.GLFW_CURSOR_NORMAL, x, y);
                } else {
                    InputConstants.grabOrReleaseMouse(client.getWindow().getWindow(), GLFW.GLFW_CURSOR_DISABLED, x, y);

                    x = client.getWindow().getScreenWidth() / 2.0;
                    y = client.getWindow().getScreenHeight() / 2.0;
                }

                client.setScreen(null);

                // This has protected access and i don't wanna AW it lmao hope this works
                //client.attackCooldown = 10000;

                hasResolutionChanged = true;
            }
        }
    }

    /**
     * It doesn't make sense to "unlock" the cursor of an absolute pointing device.
     *
     * @author quaternary
     */
    @Inject(
            method = "unlockCursor",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    public void unlockCursorXIV(CallbackInfo info) {
        if (cursorLocked) {
            cursorLocked = false;
            if (!Mod.enabled) {
                x = client.getWindow().getScreenWidth() / 2.0;
                y = client.getWindow().getScreenHeight() / 2.0;
            }
            InputConstants.grabOrReleaseMouse(client.getWindow().getWindow(), GLFW.GLFW_CURSOR_NORMAL, x, y);
        }
    }
    int lasti;
    int timeGone = 0;

    /**
     * Computes the ray origin for the current mouse position.
     * In orthographic mode, the origin is offset from the camera along the view plane.
     * In perspective mode, the origin is the camera position.
     */
    private Vec3 getRayOrigin(Camera camera, double coordsX, double coordsY) {
        if (Config.GSON.instance().ortho) {
            // Must match Ortho.createOrthoMatrix: halfWidth = getZoom()*2*aspect, halfHeight = getZoom()*2
            // coordsX already has aspect baked in (ndcX * aspect), coordsY is raw ndcY
            double orthoScale = Mod.getZoom() * 2;
            return camera.getPosition()
                    .add(new Vec3(camera.getLeftVector()).scale(-orthoScale * coordsX))
                    .add(new Vec3(camera.getUpVector()).scale(orthoScale * coordsY));
        }
        return camera.getPosition();
    }

    /**
     * Block raycast handler — shared between elytra and normal raycasts.
     */
    private static final BiFunction<RaycastContextCull, BlockPos, BlockHitResult> BLOCK_HIT_FACTORY = (innerContext, pos) -> {
        Minecraft client = Minecraft.getInstance();
        BlockState blockState = client.player.level().getBlockState(pos);
        FluidState fluidState = client.player.level().getFluidState(pos);
        Vec3 vec3d = innerContext.getFrom();
        Vec3 vec3d2 = innerContext.getTo();
        VoxelShape voxelShape = innerContext.getBlockShape(blockState, client.player.level(), pos);
        BlockHitResult firstResult = client.level.clipWithInteractionOverride(vec3d, vec3d2, pos, voxelShape, blockState);

        VoxelShape voxelShape2 = innerContext.getFluidShape(fluidState, client.player.level(), pos);
        BlockHitResult blockHitResult2 = voxelShape2.clip(vec3d, vec3d2, pos);
        double d = firstResult == null ? Double.MAX_VALUE : innerContext.getFrom().distanceToSqr(firstResult.getLocation());
        double e = blockHitResult2 == null ? Double.MAX_VALUE : innerContext.getFrom().distanceToSqr(blockHitResult2.getLocation());
        return d <= e ? firstResult : blockHitResult2;
    };

    private static final BiFunction<RaycastContextCull, BlockPos, BlockHitResult> ELYTRA_HIT_FACTORY = (innerContext, pos) -> {
        Minecraft client = Minecraft.getInstance();
        BlockState blockState = client.player.level().getBlockState(pos);
        FluidState fluidState = client.player.level().getFluidState(pos);
        Vec3 vec3d = innerContext.getFrom();
        Vec3 vec3d2 = innerContext.getTo();
        VoxelShape voxelShape = innerContext.getBlockShape(blockState, client.player.level(), pos);
        BlockHitResult firstResult = voxelShape.clip(vec3d, vec3d2, pos);

        VoxelShape voxelShape2 = innerContext.getFluidShape(fluidState, client.player.level(), pos);
        BlockHitResult blockHitResult2 = voxelShape2.clip(vec3d, vec3d2, pos);
        double d = firstResult == null ? Double.MAX_VALUE : innerContext.getFrom().distanceToSqr(firstResult.getLocation());
        double e = blockHitResult2 == null ? Double.MAX_VALUE : innerContext.getFrom().distanceToSqr(blockHitResult2.getLocation());
        return d <= e ? firstResult : blockHitResult2;
    };

    private static final Function<RaycastContextCull, BlockHitResult> MISS_FACTORY = (innerContext) -> {
        Vec3 vec3d = innerContext.getFrom().subtract(innerContext.getTo());
        return BlockHitResult.miss(innerContext.getTo(), Direction.getNearest(vec3d.x, vec3d.y, vec3d.z), BlockPos.containing(innerContext.getTo()));
    };


    /**
     * Intercept the sensitivity-adjusted mouse deltas at the exact point they are passed
     * to TutorialManager.onUpdateMouse(double, double).  Using @Redirect here guarantees
     * that 'i' and 'j' are the real delta values on every platform — no @Local ordinal
     * fragility that breaks when NeoForge patches add extra locals to the method.
     *
     * Historical note: the previous @Inject + @Local(ordinal=1/2) approach worked on
     * Fabric but failed on NeoForge because NeoForge inserts additional double locals
     * before these, shifting ordinals and causing 'i' to capture timeDelta (~1.0/tick)
     * instead of the actual mouse delta, producing a constant-rate camera spin.
     */
    @Redirect(
            method = "updateMouse",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/tutorial/TutorialManager;onUpdateMouse(DD)V")
    )
    private void updateMouseAXIV(Tutorial tutorialManager, double i, double j) {
        // Always call the original so tutorial tracking still works
        tutorialManager.onMouse(i, j);

        GameRenderer renderer = client.gameRenderer;
        Window window = client.getWindow();
        MouseHandler mouse = client.mouseHandler;

        Camera camera = renderer.getMainCamera();
        float tickDelta = camera.getPartialTickTime();
        Entity cameraEntity = client.cameraEntity;

        if (Mod.enabled && cameraEntity != null && client.player != null) {

            boolean isController = false;
            if (ModCompat.isModLoaded("midnightcontrols")) {
                isController = MidnightControlsCompat.isEnabled();
            }
            if (isController) {
                if (timeGone > 40) {
                    InputConstants.grabOrReleaseMouse(client.getWindow().getWindow(), InputConstants.CURSOR_DISABLED,
                            x, y
                    );
                    Mod.noMouse = true;
                }
                if (i == lasti) {
                    timeGone++;
                } else {
                    if (timeGone > 40) {
                        timeGone = 0;
                        InputConstants.grabOrReleaseMouse(client.getWindow().getWindow(), InputConstants.CURSOR_NORMAL,
                                x, y
                        );
                    }
                }
            }
            if (client.options.keyPickItem.isDown() || ClientInit.moveCameraBinding.isDown() || Mod.rotateToggle) {
                if (lastX == null || lastY == null) {
                    InputConstants.grabOrReleaseMouse(client.getWindow().getWindow(), InputConstants.CURSOR_DISABLED,
                            x, y
                    );
                    lastX = x;
                    lastY = y;
                }
                float yaw1 = (float) (Mod.yaw + i / 8.0D);
                float zoom = (float) (j / 8.0D) / 45F;
                Mod.yaw = yaw1;
                if (!Config.GSON.instance().XIV) {
                    Mod.pitch = 45;
                    Mod.zoom += zoom;
                    Mod.zoom = Math.clamp(Mod.zoom, 1F, 10F);
                } else {
                    Mod.pitch = Mod.pitch + zoom * 45F;
                    Mod.pitch = Mth.clamp(Mod.pitch, 15F, 90F);
                }

            } else {
                if (lastX != null && lastY != null) {
                    InputConstants.grabOrReleaseMouse(
                            client.getWindow().getWindow(),
                            GLFW.GLFW_CURSOR_NORMAL,
                            lastX != null ? lastX : x,
                            lastY != null ? lastY : y
                    );
                    x = lastX;
                    y = lastY;
                    lastX = null;
                    lastY = null;
                }
                if (Mod.horizontalTarget != null) {
                    Mod.horizontalTarget = new BlockHitResult(Mod.horizontalTarget.getLocation().add(0, ((-j) / 40), 0), Mod.horizontalTarget.getDirection(), BlockPos.containing(Mod.horizontalTarget.getLocation().add(0, ((-j) / 40), 0)), true);
                }

                // --- Screen-to-world ray computation ---
                int fbWidth = window.getWidth();
                int fbHeight = window.getHeight();
                double aspect = (double) fbWidth / fbHeight;
                double fov2 = Math.toRadians(((GameRendererAccessor) renderer).callGetFov(camera, tickDelta, true)) / 2.0;
                double tanFov2 = Math.tan(fov2);

                // Normalize mouse position to [-1, 1] NDC
                double ndcX = (mouse.xpos() / fbWidth) * 2.0 - 1.0;
                double ndcY = -((mouse.ypos() / fbHeight) * 2.0 - 1.0);

                // Apply aspect ratio and FOV to get view-space offsets
                double offsetX = ndcX * aspect * tanFov2;
                double offsetY = ndcY * tanFov2;

                // Build camera-space basis vectors
                Vector3d forward = camera.rotation().transform(new Vector3d(0.0, 0.0, -1.0));
                Vector3d right = camera.rotation().transform(new Vector3d(1.0, 0.0, 0.0));
                Vector3d up = camera.rotation().transform(new Vector3d(0.0, 1.0, 0.0));

                // Construct ray direction
                Vec3 rayDir;
                if (Config.GSON.instance().ortho) {
                    rayDir = new Vec3(forward.x, forward.y, forward.z);
                } else {
                    Vector3d dir = new Vector3d(forward).add(new Vector3d(right).mul(offsetX)).add(new Vector3d(up).mul(offsetY)).normalize();
                    rayDir = new Vec3(dir.x, dir.y, dir.z);
                }

                // Compute ray origin (offset in ortho mode, camera pos in perspective)
                Vec3 rayOrigin = getRayOrigin(camera, ndcX * aspect, ndcY);

                // End point of the ray
                Vec3 end = rayOrigin.add(rayDir.scale(renderer.getDepthFar()));

                // Bounding box for entity search along the ray
                AABB box = AABB.ofSize(rayOrigin, 2, 2, 2)
                        .expandTowards(rayDir.scale(renderer.getDepthFar() * 2))
                        .inflate(1.0, 1.0, 1.0);

                // Elytra flight: adjust endpoint to hit ground level
                if (cameraEntity instanceof Player player && player.isFallFlying()) {
                    if (((CameraAccessor) camera).getPosBeforeModulation() != null) {
                        HitResult hitResult0 = raycast(rayOrigin, end, new RaycastContextCull(
                                rayOrigin, end,
                                CustomShapeTypes.AIR_AT_LEVEL,
                                ClipContext.Block.OUTLINE,
                                ClipContext.Fluid.NONE,
                                cameraEntity
                        ), ELYTRA_HIT_FACTORY, MISS_FACTORY);
                        if (hitResult0 instanceof BlockHitResult result) {
                            Mod.flyingYAddition = -2 * Math.log(result.getLocation().distanceTo(player.getEyePosition()) / 8F);
                        }
                        end = hitResult0.getLocation().add(0, Mod.flyingYAddition, 0);
                    }
                }

                // Main block raycast
                HitResult hitResult0 = raycast(rayOrigin, end, new RaycastContextCull(
                        rayOrigin, end,
                        CustomShapeTypes.CULLED,
                        ClipContext.Block.OUTLINE,
                        ClipContext.Fluid.NONE,
                        cameraEntity
                ), BLOCK_HIT_FACTORY, MISS_FACTORY);

                // Scan back toward camera, then forward again to find the nearest visible surface
                BlockHitResult scanUp = client.player.level().clip(
                        new ClipContext(
                                hitResult0.getLocation(), rayOrigin, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, client.player)
                );
                BlockHitResult scanDown = client.player.level().clip(
                        new ClipContext(
                                scanUp.getLocation(), scanUp.getLocation().add(hitResult0.getLocation().subtract(scanUp.getLocation()).normalize().scale(renderer.getDepthFar() * 2)), ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, client.player)
                );

                if (Mod.crosshairTarget == null) {
                    Mod.crosshairTarget = client.hitResult;
                }

                // Entity targeting: first try scanDown->camera ray, then camera->end ray
                HitResult hitResult = raycastExpanded(
                        cameraEntity,
                        scanDown.getLocation(),
                        rayOrigin,
                        box,
                        entity -> !entity.isSpectator() && entity.isPickable(),
                        renderer.getDepthFar() * 2,
                        0.5F);
                HitResult hitResult2 = raycastExpanded(
                        cameraEntity,
                        rayOrigin,
                        end,
                        box,
                        entity -> !entity.isSpectator() && entity.isPickable(),
                        renderer.getDepthFar() * 2,
                        0.5F);

                if (!(hitResult instanceof EntityHitResult result && result.getType().equals(HitResult.Type.ENTITY))) {
                    if (!(hitResult2 instanceof EntityHitResult result2 && result2.getType().equals(HitResult.Type.ENTITY))) {
                        hitResult = scanDown;
                        if (cameraEntity instanceof Player player && hitResult.getLocation().distanceTo(cameraEntity.getEyePosition()) > player.blockInteractionRange() && cameraEntity.level().getBlockEntity(BlockPos.containing(hitResult.getLocation())) == null
                                && !(cameraEntity.level().getBlockState(BlockPos.containing(hitResult.getLocation())).getBlock() instanceof FaceAttachedHorizontalDirectionalBlock)
                                && !(cameraEntity.level().getBlockState(BlockPos.containing(hitResult.getLocation())).getBlock() instanceof DoorBlock)
                                && !(cameraEntity.level().getBlockState(BlockPos.containing(hitResult.getLocation())).getBlock() instanceof TrapDoorBlock)) {
                            hitResult = new BlockHitResult(new Vec3(scanDown.getLocation().x(), cameraEntity.getEyeY(), scanDown.getLocation().z()), scanDown.getDirection(), BlockPos.containing(scanDown.getLocation().x(), cameraEntity.getEyeY(), scanDown.getLocation().z()), false);
                        }
                    } else {
                        hitResult = hitResult2;
                    }
                }

                if (ClientInit.verticalBinding.consumeClick()) {
                    Mod.verticalMode = !Mod.verticalMode;
                    Mod.horizontalTarget = Mod.verticalMode ? scanDown : null;
                    if (Mod.verticalMode) {
                        client.player.displayClientMessage(Component.translatable("Vertical look mode activated (Default Keybind: RIGHT ALT)"), true);
                    }
                }
                Mod.prevCrosshairTarget = Mod.crosshairTarget != null ? Mod.crosshairTarget : Mod.horizontalTarget != null ? Mod.horizontalTarget : hitResult;
                Mod.crosshairTarget = Mod.horizontalTarget != null ? Mod.horizontalTarget : hitResult;
                Mod.mouseTarget = Mod.horizontalTarget != null ? Mod.horizontalTarget : hitResult;
            }
        }
    }

    private static <T, C> T raycast(Vec3 start, Vec3 end, C context, BiFunction<C, BlockPos, T> blockHitFactory, Function<C, T> missFactory) {
        if (start.equals(end)) {
            return missFactory.apply(context);
        } else {
            double d = Mth.lerp(-1.0E-7, end.x, start.x);
            double e = Mth.lerp(-1.0E-7, end.y, start.y);
            double f = Mth.lerp(-1.0E-7, end.z, start.z);
            double g = Mth.lerp(-1.0E-7, start.x, end.x);
            double h = Mth.lerp(-1.0E-7, start.y, end.y);
            double i = Mth.lerp(-1.0E-7, start.z, end.z);
            int j = Mth.floor(g);
            int k = Mth.floor(h);
            int l = Mth.floor(i);
            BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos(j, k, l);
            T object = blockHitFactory.apply(context, mutable);
            if (object != null) {
                return object;
            } else {
                double m = d - g;
                double n = e - h;
                double o = f - i;
                int p = Mth.sign(m);
                int q = Mth.sign(n);
                int r = Mth.sign(o);
                double s = p == 0 ? Double.MAX_VALUE : (double)p / m;
                double t = q == 0 ? Double.MAX_VALUE : (double)q / n;
                double u = r == 0 ? Double.MAX_VALUE : (double)r / o;
                double v = s * (p > 0 ? 1.0 - Mth.frac(g) : Mth.frac(g));
                double w = t * (q > 0 ? 1.0 - Mth.frac(h) : Mth.frac(h));
                double x = u * (r > 0 ? 1.0 - Mth.frac(i) : Mth.frac(i));

                Object object2;
                do {
                    if (!(v <= 1.0) && !(w <= 1.0) && !(x <= 1.0)) {
                        return missFactory.apply(context);
                    }

                    if (v < w) {
                        if (v < x) {
                            j += p;
                            v += s;
                        } else {
                            l += r;
                            x += u;
                        }
                    } else if (w < x) {
                        k += q;
                        w += t;
                    } else {
                        l += r;
                        x += u;
                    }

                    object2 = blockHitFactory.apply(context, mutable.set(j, k, l));
                } while(object2 == null);

                return (T) object2;
            }
        }
    }
    @Nullable
     private static EntityHitResult raycastExpanded(Entity entity, Vec3 min, Vec3 max, AABB box, Predicate<Entity> predicate, double maxDistance, float margin) {
        Level world = entity.level();
        double d = maxDistance;
        Entity entity2 = null;
        Vec3 vec3d = null;
        Iterator var12 = world.getEntities(entity, box, predicate).iterator();

        while(true) {
            while(var12.hasNext()) {
                Entity entity3 = (Entity)var12.next();
                AABB box2 = entity3.getBoundingBox().inflate((double)entity3.getPickRadius()+margin);
                Optional<Vec3> optional = box2.clip(min, max);
                if (box2.contains(min)) {
                    if (d >= 0.0) {
                        entity2 = entity3;
                        vec3d = (Vec3)optional.orElse(min);
                        d = 0.0;
                    }
                } else if (optional.isPresent()) {
                    Vec3 vec3d2 = (Vec3)optional.get();
                    double e = min.distanceToSqr(vec3d2);
                    if (e < d || d == 0.0) {
                        if (entity3.getRootVehicle() == entity.getRootVehicle()) {
                            if (d == 0.0) {
                                entity2 = entity3;
                                vec3d = vec3d2;
                            }
                        } else {
                            entity2 = entity3;
                            vec3d = vec3d2;
                            d = e;
                        }
                    }
                }
            }

            if (entity2 == null || (entity instanceof LivingEntity living && !living.hasLineOfSight(entity2)) || entity2.isInvisible() || (entity instanceof Player player && entity2.isInvisibleTo(player))) {
                return null;
            }
            if(Mod.crosshairTarget instanceof EntityHitResult result && entity2.equals(result.getEntity())){
                lockontime++;
            }
            else{
                lockontime = 0;
            }
            lockontime = Math.min(160,lockontime);
            return new EntityHitResult(entity2, ClientInit.lockOn.isDown() ? vec3d : entity2.getEyePosition());
        }
    }
    @Inject(
            method = {"updateMouse"}, at = {
            @At(
                    value = "INVOKE",
                    target = "net/minecraft/client/network/ClientPlayerEntity.changeLookDirection(DD)V"
            )
    }, cancellable = true
    )
    private void updateMouseBXIV(CallbackInfo info) {
        if (Mod.enabled) {
            info.cancel();
        }
    }



    @Redirect(method = "onMouseScroll", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerInventory;scrollInHotbar(D)V"))


    private void scrollInHotbarXIV(Inventory instance, double scrollAmount) {

            if (Mod.enabled && Config.GSON.instance().scrollWheelZoom ) {


                Mod.zoom = (Math.clamp(Mod.zoom - (float) scrollAmount * 0.2f,2F/Math.clamp(Config.GSON.instance().zoomFactor,1F,1.5F),10.0F));

            } else {

                instance.swapPaint(scrollAmount);


            }


        }

    @Override
    public void setRightClick(boolean bool) {
        rightButtonClicked = bool;
    }
}
