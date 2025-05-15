package com.cleannrooster.dungeons_iso.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.Window;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
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
import com.cleannrooster.dungeons_iso.api.MinecraftClientAccessor;

import java.util.Iterator;
import java.util.Optional;
import java.util.function.Predicate;

import static net.minecraft.entity.projectile.ProjectileUtil.raycast;

@Mixin(Mouse.class)
public class MouseMixin {
    @Shadow
    private boolean cursorLocked;
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


    @Unique
    @Nullable
    private Double lastX;
    @Unique
    @Nullable
    private Double lastY;

    /**
     * It doesn't make sense to "lock" the cursor of an absolute pointing device.
     *
     * @author quaternary
     */
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
                if (client.options.pickItemKey.isPressed()||ClientInit.moveCameraBinding.isPressed()) {
                    if (lastX == null || lastY == null) {
                        InputUtil.setCursorParameters(client.getWindow().getHandle(), InputUtil.GLFW_CURSOR_DISABLED,
                                x, y
                        );
                        lastX = x;
                        lastY = y;
                    }
                    float yaw1 = (float) (Mod.yaw + i / 8.0D);
                    float pitch1 = (float) (Mod.pitch + j * k / 8.0D);
                    Mod.yaw = yaw1;
                    Mod.pitch = 45;


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

                    Vector2d res = new Vector2d(window.getFramebufferWidth(), window.getFramebufferHeight());
                    double aspect = res.x / res.y;
                    Vector2d coords = new Vector2d(mouse.getX(), mouse.getY()).div(res).mul(2.0).sub(new Vector2d(1.0));
                    double fov2 =
                            Math.toRadians(((GameRendererAccessor) renderer).callGetFov(camera, tickDelta, true)) / 2.0;
                    coords.x *= aspect;
                    coords.y = -coords.y;
                    Vector2d offsets = coords.mul(Math.tan(fov2));
                    Vector3d forward = camera.getRotation().transform(new Vector3d(0.0, 0.0, -1.0));
                    Vector3d right = camera.getRotation().transform(new Vector3d(1.0, 0.0, 0.0));
                    Vector3d up = camera.getRotation().transform(new Vector3d(0.0, 1.0, 0.0));
                    Vector3d dir = forward.add(right.mul(offsets.x).add(up.mul(offsets.y))).normalize();
                    Vec3d rayDir = new Vec3d(dir.x, dir.y, dir.z);
                    Box box = cameraEntity
                            .getBoundingBox()
                            .stretch(rayDir.multiply(renderer.getFarPlaneDistance()))
                            .expand(1.0, 1.0, 1.0);
                    Vec3d start = camera.getPos().add(rayDir.multiply(camera.getPos().distanceTo(client.player.getEyePos())-0));

                    HitResult hitResult0 = cameraEntity.getWorld().raycast(new RaycastContext(
                            start,
                            camera.getPos(),
                            RaycastContext.ShapeType.OUTLINE,
                            RaycastContext.FluidHandling.NONE,
                            cameraEntity
                    ));
                    if(hitResult0.getType().equals(HitResult.Type.BLOCK) && hitResult0.getPos().getY() > client.player.getPos().getY()+0.5){
                        start = start.add(rayDir.multiply(-1).multiply(0.9*hitResult0.getPos().distanceTo(start)));
                    }
                    else{
                        start = camera.getPos();
                    }
                    Vec3d start2 = client.player.getEyePos();

                    Vec3d end = start.add(rayDir.multiply(renderer.getFarPlaneDistance()));


                    HitResult hitResult = raycastExpanded(
                            cameraEntity,
                            start,
                            end,
                            box,
                            entity -> !entity.isSpectator() && entity.canHit(),
                            renderer.getFarPlaneDistance()
                    ,0.5F);
                    if (hitResult == null) {
                        hitResult = cameraEntity.getWorld().raycast(new RaycastContext(
                                start,
                                end,
                                RaycastContext.ShapeType.OUTLINE,
                                RaycastContext.FluidHandling.NONE,
                                cameraEntity
                        ));
                    }
                    Mod.crosshairTarget = hitResult;


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

            if (entity2 == null) {
                return null;
            }

            return new EntityHitResult(entity2, vec3d);
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


                Mod.zoom = (Math.clamp(Mod.zoom - (float) scrollAmount * 0.2f,0,5.0F));


            } else {

                instance.scrollInHotbar(scrollAmount);


            }


        }
}
