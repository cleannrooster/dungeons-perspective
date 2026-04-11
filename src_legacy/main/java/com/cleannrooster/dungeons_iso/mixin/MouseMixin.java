package com.cleannrooster.dungeons_iso.mixin;

import com.cleannrooster.dungeons_iso.api.*;
import com.cleannrooster.dungeons_iso.compat.MidnightControlsCompat;
import com.llamalad7.mixinextras.sugar.Local;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.block.WallMountedBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.sound.ElytraSoundInstance;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.Window;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.fluid.FluidState;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.*;
import net.minecraft.world.event.BlockPositionSource;
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

import java.util.Iterator;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

@Mixin(Mouse.class)
public class MouseMixin implements MouseAccessor {
    @Shadow
    private boolean cursorLocked;
    @Shadow
    private boolean rightButtonClicked;

    @Shadow
    @Final
    private MinecraftClient client;
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

        if (client.isWindowFocused()) {
            if (!cursorLocked) {
                if (!MinecraftClient.IS_SYSTEM_MAC) {
                    KeyBinding.updatePressedStates();
                }

                cursorLocked = true;

                if (Mod.enabled) {
                    // Merely hide the cursor instead of "disabling" it
                    InputUtil.setCursorParameters(client.getWindow().getHandle(), GLFW.GLFW_CURSOR_NORMAL, x, y);
                } else {
                    InputUtil.setCursorParameters(client.getWindow().getHandle(), GLFW.GLFW_CURSOR_DISABLED, x, y);

                    x = client.getWindow().getWidth() / 2.0;
                    y = client.getWindow().getHeight() / 2.0;
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
                x = client.getWindow().getWidth() / 2.0;
                y = client.getWindow().getHeight() / 2.0;
            }
            InputUtil.setCursorParameters(client.getWindow().getHandle(), GLFW.GLFW_CURSOR_NORMAL, x, y);
        }
    }
    int lasti;
    int timeGone = 0;

    /**
     * Computes the ray origin for the current mouse position.
     * In orthographic mode, the origin is offset from the camera along the view plane.
     * In perspective mode, the origin is the camera position.
     */
    private Vec3d getRayOrigin(Camera camera, double coordsX, double coordsY) {
        if (Config.GSON.instance().ortho) {
            // Must match Ortho.createOrthoMatrix: halfWidth = getZoom()*2*aspect, halfHeight = getZoom()*2
            // coordsX already has aspect baked in (ndcX * aspect), coordsY is raw ndcY
            double orthoScale = Mod.getZoom() * 2;
            return camera.getPos()
                    .add(new Vec3d(camera.getDiagonalPlane()).multiply(-orthoScale * coordsX))
                    .add(new Vec3d(camera.getVerticalPlane()).multiply(orthoScale * coordsY));
        }
        return camera.getPos();
    }

    /**
     * Block raycast handler — shared between elytra and normal raycasts.
     */
    private static final BiFunction<RaycastContextCull, BlockPos, BlockHitResult> BLOCK_HIT_FACTORY = (innerContext, pos) -> {
        MinecraftClient client = MinecraftClient.getInstance();
        BlockState blockState = client.player.getWorld().getBlockState(pos);
        FluidState fluidState = client.player.getWorld().getFluidState(pos);
        Vec3d vec3d = innerContext.getStart();
        Vec3d vec3d2 = innerContext.getEnd();
        VoxelShape voxelShape = innerContext.getBlockShape(blockState, client.player.getWorld(), pos);
        BlockHitResult firstResult = client.world.raycastBlock(vec3d, vec3d2, pos, voxelShape, blockState);

        VoxelShape voxelShape2 = innerContext.getFluidShape(fluidState, client.player.getWorld(), pos);
        BlockHitResult blockHitResult2 = voxelShape2.raycast(vec3d, vec3d2, pos);
        double d = firstResult == null ? Double.MAX_VALUE : innerContext.getStart().squaredDistanceTo(firstResult.getPos());
        double e = blockHitResult2 == null ? Double.MAX_VALUE : innerContext.getStart().squaredDistanceTo(blockHitResult2.getPos());
        return d <= e ? firstResult : blockHitResult2;
    };

    private static final BiFunction<RaycastContextCull, BlockPos, BlockHitResult> ELYTRA_HIT_FACTORY = (innerContext, pos) -> {
        MinecraftClient client = MinecraftClient.getInstance();
        BlockState blockState = client.player.getWorld().getBlockState(pos);
        FluidState fluidState = client.player.getWorld().getFluidState(pos);
        Vec3d vec3d = innerContext.getStart();
        Vec3d vec3d2 = innerContext.getEnd();
        VoxelShape voxelShape = innerContext.getBlockShape(blockState, client.player.getWorld(), pos);
        BlockHitResult firstResult = voxelShape.raycast(vec3d, vec3d2, pos);

        VoxelShape voxelShape2 = innerContext.getFluidShape(fluidState, client.player.getWorld(), pos);
        BlockHitResult blockHitResult2 = voxelShape2.raycast(vec3d, vec3d2, pos);
        double d = firstResult == null ? Double.MAX_VALUE : innerContext.getStart().squaredDistanceTo(firstResult.getPos());
        double e = blockHitResult2 == null ? Double.MAX_VALUE : innerContext.getStart().squaredDistanceTo(blockHitResult2.getPos());
        return d <= e ? firstResult : blockHitResult2;
    };

    private static final Function<RaycastContextCull, BlockHitResult> MISS_FACTORY = (innerContext) -> {
        Vec3d vec3d = innerContext.getStart().subtract(innerContext.getEnd());
        return BlockHitResult.createMissed(innerContext.getEnd(), Direction.getFacing(vec3d.x, vec3d.y, vec3d.z), BlockPos.ofFloored(innerContext.getEnd()));
    };


    @Inject(
            method = "updateMouse",
            at = @At(value = "INVOKE", target = "net/minecraft/client/tutorial/TutorialManager.onUpdateMouse(DD)V")
    )
    private void updateMouseAXIV(
            double timeDelta, CallbackInfo ci, @Local(ordinal = 1) double i, @Local(ordinal = 2) double j, @Local int k
    ) {
        GameRenderer renderer = client.gameRenderer;
        Window window = client.getWindow();
        Mouse mouse = client.mouse;

        Camera camera = renderer.getCamera();
        float tickDelta = camera.getLastTickDelta();
        Entity cameraEntity = client.cameraEntity;
        boolean spell = false;

        if (Mod.enabled && cameraEntity != null && client.player != null) {

            boolean isController = false;
            if (FabricLoader.getInstance().isModLoaded("midnightcontrols")) {
                isController = MidnightControlsCompat.isEnabled();
            }
            if(isController) {
                if (timeGone > 40) {
                    InputUtil.setCursorParameters(client.getWindow().getHandle(), InputUtil.GLFW_CURSOR_DISABLED,
                            x, y
                    );
                    Mod.noMouse = true;
                }
                if (i == lasti) {
                    timeGone++;
                } else {
                    if (timeGone > 40) {
                        timeGone = 0;
                        InputUtil.setCursorParameters(client.getWindow().getHandle(), InputUtil.GLFW_CURSOR_NORMAL,
                                x, y
                        );
                    }
                }
            }
                if (client.options.pickItemKey.isPressed()||ClientInit.moveCameraBinding.isPressed() || Mod.rotateToggle) {
                    if (lastX == null || lastY == null) {
                        InputUtil.setCursorParameters(client.getWindow().getHandle(), InputUtil.GLFW_CURSOR_DISABLED,
                                x, y
                        );
                        lastX = x;
                        lastY = y;
                    }
                    float yaw1 = (float) (Mod.yaw + i / 8.0D);
                    float zoom = (float) ( j * k / 8.0D)/45F;
                    Mod.yaw = yaw1;
                    if(!Config.GSON.instance().XIV) {
                        Mod.pitch = 45;
                        Mod.zoom += zoom;
                        Mod.zoom = Math.clamp(Mod.zoom, 1F, 10F);
                    }
                    else{
                        Mod.pitch = Mod.pitch + zoom*45F;
                        Mod.pitch = MathHelper.clamp(Mod.pitch, 15F, 90F);

                    }

                    Mod.crosshairTarget = null;
                } else {
                    if (lastX != null && lastY != null) {
                        InputUtil.setCursorParameters(
                                client.getWindow().getHandle(),
                                GLFW.GLFW_CURSOR_NORMAL,
                                lastX != null ? lastX : x,
                                lastY != null ? lastY : y
                        );

                            x = lastX;
                        y = lastY;
                        lastX = null;
                        lastY = null;
                    }
                    if(Mod.horizontalTarget != null){
                        Mod.horizontalTarget = new BlockHitResult(Mod.horizontalTarget.getPos().add(0, ((-j*k)/40),0),Mod.horizontalTarget.getSide(),BlockPos.ofFloored(Mod.horizontalTarget.getPos().add(0, ((-j*k)/40),0)),true);

                    }

                    // --- Screen-to-world ray computation ---
                    int fbWidth = window.getFramebufferWidth();
                    int fbHeight = window.getFramebufferHeight();
                    double aspect = (double) fbWidth / fbHeight;
                    double fov2 = Math.toRadians(((GameRendererAccessor) renderer).callGetFov(camera, tickDelta, true)) / 2.0;
                    double tanFov2 = Math.tan(fov2);

                    // Normalize mouse position to [-1, 1] NDC
                    double ndcX = (mouse.getX() / fbWidth) * 2.0 - 1.0;
                    double ndcY = -((mouse.getY() / fbHeight) * 2.0 - 1.0);

                    // Apply aspect ratio and FOV to get view-space offsets
                    double offsetX = ndcX * aspect * tanFov2;
                    double offsetY = ndcY * tanFov2;

                    // Build camera-space basis vectors
                    Vector3d forward = camera.getRotation().transform(new Vector3d(0.0, 0.0, -1.0));
                    Vector3d right = camera.getRotation().transform(new Vector3d(1.0, 0.0, 0.0));
                    Vector3d up = camera.getRotation().transform(new Vector3d(0.0, 1.0, 0.0));

                    // Construct ray direction
                    Vec3d rayDir;
                    if (Config.GSON.instance().ortho) {
                        rayDir = new Vec3d(forward.x, forward.y, forward.z);
                    } else {
                        Vector3d dir = new Vector3d(forward).add(new Vector3d(right).mul(offsetX)).add(new Vector3d(up).mul(offsetY)).normalize();
                        rayDir = new Vec3d(dir.x, dir.y, dir.z);
                    }

                    // Compute ray origin (offset in ortho mode, camera pos in perspective)
                    Vec3d rayOrigin = getRayOrigin(camera, ndcX * aspect, ndcY);

                    // End point of the ray
                    Vec3d end = rayOrigin.add(rayDir.multiply(renderer.getFarPlaneDistance()));

                    // Bounding box for entity search along the ray
                    Box box = Box.of(rayOrigin, 2, 2, 2)
                            .stretch(rayDir.multiply(renderer.getFarPlaneDistance() * 2))
                            .expand(1.0, 1.0, 1.0);

                    // Elytra flight: adjust endpoint to hit ground level
                    if (cameraEntity instanceof PlayerEntity player && player.isFallFlying()) {
                        if (((CameraAccessor) camera).getPosBeforeModulation() != null) {
                            HitResult hitResult0 = raycast(rayOrigin, end, new RaycastContextCull(
                                    rayOrigin, end,
                                    CustomShapeTypes.AIR_AT_LEVEL,
                                    RaycastContext.ShapeType.OUTLINE,
                                    RaycastContext.FluidHandling.NONE,
                                    cameraEntity
                            ), ELYTRA_HIT_FACTORY, MISS_FACTORY);
                            if (hitResult0 instanceof BlockHitResult result) {
                                Mod.flyingYAddition = -2 * Math.log(result.getPos().distanceTo(player.getEyePos()) / 8F);
                            }
                            end = hitResult0.getPos().add(0, Mod.flyingYAddition, 0);
                        }
                    }

                    // Main block raycast
                    HitResult hitResult0 = raycast(rayOrigin, end, new RaycastContextCull(
                            rayOrigin, end,
                            CustomShapeTypes.CULLED,
                            RaycastContext.ShapeType.OUTLINE,
                            RaycastContext.FluidHandling.NONE,
                            cameraEntity
                    ), BLOCK_HIT_FACTORY, MISS_FACTORY);

                    // Scan back toward camera, then forward again to find the nearest visible surface
                    BlockHitResult scanUp = client.player.getWorld().raycast(
                            new RaycastContext(
                                    hitResult0.getPos(), rayOrigin, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, client.player)
                    );
                    BlockHitResult scanDown = client.player.getWorld().raycast(
                            new RaycastContext(
                                    scanUp.getPos(), scanUp.getPos().add(hitResult0.getPos().subtract(scanUp.getPos()).normalize().multiply(renderer.getFarPlaneDistance() * 2)), RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, client.player)
                    );

                    if (Mod.crosshairTarget == null) {
                        Mod.crosshairTarget = client.crosshairTarget;
                    }

                    // Entity targeting: first try scanDown->camera ray, then camera->end ray
                    HitResult hitResult = raycastExpanded(
                            cameraEntity,
                            scanDown.getPos(),
                            rayOrigin,
                            box,
                            entity -> !entity.isSpectator() && entity.canHit(),
                            renderer.getFarPlaneDistance() * 2,
                            0.5F);
                    HitResult hitResult2 = raycastExpanded(
                            cameraEntity,
                            rayOrigin,
                            end,
                            box,
                            entity -> !entity.isSpectator() && entity.canHit(),
                            renderer.getFarPlaneDistance() * 2,
                            0.5F);

                    if (!(hitResult instanceof EntityHitResult result && result.getType().equals(HitResult.Type.ENTITY))) {
                        if (!(hitResult2 instanceof EntityHitResult result2 && result2.getType().equals(HitResult.Type.ENTITY))) {
                            hitResult = scanDown;
                            if (cameraEntity instanceof PlayerEntity player && hitResult.getPos().distanceTo(cameraEntity.getEyePos()) > player.getBlockInteractionRange() && cameraEntity.getWorld().getBlockEntity(BlockPos.ofFloored(hitResult.getPos())) == null
                                    && !(cameraEntity.getWorld().getBlockState(BlockPos.ofFloored(hitResult.getPos())).getBlock() instanceof WallMountedBlock)
                                    && !(cameraEntity.getWorld().getBlockState(BlockPos.ofFloored(hitResult.getPos())).getBlock() instanceof DoorBlock)
                                    && !(cameraEntity.getWorld().getBlockState(BlockPos.ofFloored(hitResult.getPos())).getBlock() instanceof TrapdoorBlock)) {
                                hitResult = new BlockHitResult(new Vec3d(scanDown.getPos().getX(), cameraEntity.getEyeY(), scanDown.getPos().getZ()), scanDown.getSide(), BlockPos.ofFloored(scanDown.getPos().getX(), cameraEntity.getEyeY(), scanDown.getPos().getZ()), false);
                            }
                        } else {
                            hitResult = hitResult2;
                        }
                    }

                    if (ClientInit.verticalBinding.wasPressed()) {
                        Mod.verticalMode = !Mod.verticalMode;
                        Mod.horizontalTarget = Mod.verticalMode ? scanDown : null;
                        if (Mod.verticalMode) {
                            client.player.sendMessage(Text.translatable("Vertical look mode activated (Default Keybind: RIGHT ALT)"), true);
                        }
                    }
                    Mod.prevCrosshairTarget = Mod.crosshairTarget != null ? Mod.crosshairTarget : Mod.horizontalTarget != null ? Mod.horizontalTarget : hitResult;
                    Mod.crosshairTarget = Mod.horizontalTarget != null ? Mod.horizontalTarget : hitResult;
                    Mod.mouseTarget = Mod.horizontalTarget != null ? Mod.horizontalTarget : hitResult;
                }



        }
    }

    private static <T, C> T raycast(Vec3d start, Vec3d end, C context, BiFunction<C, BlockPos, T> blockHitFactory, Function<C, T> missFactory) {
        if (start.equals(end)) {
            return missFactory.apply(context);
        } else {
            double d = MathHelper.lerp(-1.0E-7, end.x, start.x);
            double e = MathHelper.lerp(-1.0E-7, end.y, start.y);
            double f = MathHelper.lerp(-1.0E-7, end.z, start.z);
            double g = MathHelper.lerp(-1.0E-7, start.x, end.x);
            double h = MathHelper.lerp(-1.0E-7, start.y, end.y);
            double i = MathHelper.lerp(-1.0E-7, start.z, end.z);
            int j = MathHelper.floor(g);
            int k = MathHelper.floor(h);
            int l = MathHelper.floor(i);
            BlockPos.Mutable mutable = new BlockPos.Mutable(j, k, l);
            T object = blockHitFactory.apply(context, mutable);
            if (object != null) {
                return object;
            } else {
                double m = d - g;
                double n = e - h;
                double o = f - i;
                int p = MathHelper.sign(m);
                int q = MathHelper.sign(n);
                int r = MathHelper.sign(o);
                double s = p == 0 ? Double.MAX_VALUE : (double)p / m;
                double t = q == 0 ? Double.MAX_VALUE : (double)q / n;
                double u = r == 0 ? Double.MAX_VALUE : (double)r / o;
                double v = s * (p > 0 ? 1.0 - MathHelper.fractionalPart(g) : MathHelper.fractionalPart(g));
                double w = t * (q > 0 ? 1.0 - MathHelper.fractionalPart(h) : MathHelper.fractionalPart(h));
                double x = u * (r > 0 ? 1.0 - MathHelper.fractionalPart(i) : MathHelper.fractionalPart(i));

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
     private static EntityHitResult raycastExpanded(Entity entity, Vec3d min, Vec3d max, Box box, Predicate<Entity> predicate, double maxDistance, float margin) {
        World world = entity.getWorld();
        double d = maxDistance;
        Entity entity2 = null;
        Vec3d vec3d = null;
        Iterator var12 = world.getOtherEntities(entity, box, predicate).iterator();

        while(true) {
            while(var12.hasNext()) {
                Entity entity3 = (Entity)var12.next();
                Box box2 = entity3.getBoundingBox().expand((double)entity3.getTargetingMargin()+margin);
                Optional<Vec3d> optional = box2.raycast(min, max);
                if (box2.contains(min)) {
                    if (d >= 0.0) {
                        entity2 = entity3;
                        vec3d = (Vec3d)optional.orElse(min);
                        d = 0.0;
                    }
                } else if (optional.isPresent()) {
                    Vec3d vec3d2 = (Vec3d)optional.get();
                    double e = min.squaredDistanceTo(vec3d2);
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

            if (entity2 == null || (entity instanceof LivingEntity living && !living.canSee(entity2)) || entity2.isInvisible() || (entity instanceof PlayerEntity player && entity2.isInvisibleTo(player))) {
                return null;
            }
            if(Mod.crosshairTarget instanceof EntityHitResult result && entity2.equals(result.getEntity())){
                lockontime++;
            }
            else{
                lockontime = 0;
            }
            lockontime = Math.min(160,lockontime);
            return new EntityHitResult(entity2, ClientInit.lockOn.isPressed() ? vec3d : entity2.getEyePos());
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


    private void scrollInHotbarXIV(PlayerInventory instance, double scrollAmount) {

            if (Mod.enabled && Config.GSON.instance().scrollWheelZoom ) {


                Mod.zoom = (Math.clamp(Mod.zoom - (float) scrollAmount * 0.2f,2F/Math.clamp(Config.GSON.instance().zoomFactor,1F,1.5F),10.0F));

            } else {

                instance.scrollInHotbar(scrollAmount);


            }


        }

    @Override
    public void setRightClick(boolean bool) {
        rightButtonClicked = bool;
    }
}
