package com.cleannrooster.dungeons_iso.mixin;

import com.cleannrooster.dungeons_iso.ModCompat;
import com.cleannrooster.dungeons_iso.api.*;
import com.cleannrooster.dungeons_iso.api.cullers.room.CullDebug;
import com.cleannrooster.dungeons_iso.compat.DragonCompat;
import com.cleannrooster.dungeons_iso.compat.MidnightControlsCompat;
import com.cleannrooster.dungeons_iso.compat.SodiumCompat;
import com.cleannrooster.dungeons_iso.config.Config;
import com.cleannrooster.dungeons_iso.ui.OwoScreens;
import com.cleannrooster.dungeons_iso.util.InteractionTargeting;
import com.google.common.collect.Lists;
import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.Window;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.control.LookControl;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.*;
import net.minecraft.server.command.DebugCommand;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.entity.EntityLookup;
import net.minecraft.world.event.BlockPositionSource;
import net.minecraft.world.gen.chunk.DebugChunkGenerator;
import org.apache.logging.log4j.core.appender.rolling.action.IfAll;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.cleannrooster.dungeons_iso.ClientInit;
import com.cleannrooster.dungeons_iso.compat.SpellEngineCompat;
import com.cleannrooster.dungeons_iso.mod.Mod;
import com.cleannrooster.dungeons_iso.util.ContextualInteractionTargeting;
import com.cleannrooster.dungeons_iso.util.ContextualTargeting;
import com.cleannrooster.dungeons_iso.util.Util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.cleannrooster.dungeons_iso.mod.Mod.*;


@Mixin(value = MinecraftClient.class,priority = 0 )
public abstract class MinecraftClientMixin implements MinecraftClientAccessor {
    private boolean canUseItem;

    // Per-tick exponential smoothing factor for the player's cosmetic pitch (see lookAt()).
    // Runs at the fixed 20 TPS tick; ~0.35 reaches the target in a few ticks while easing out
    // snap transitions. Lower = smoother/laggier, higher = snappier.
    private static final float PITCH_SMOOTHING = 0.35f;

    // Movement-targeting: ticks of continuous non-combat input after which the soft target is
    // cleared (bridges brief gaps between attacks/casts so the target isn't dropped mid-combat).
    private static final int MOVEMENT_TARGET_IDLE_CLEAR_TICKS = 40;
    private int idleTargetTicks;

    @Shadow
    private int itemUseCooldown;
    private Vec3d originalLocation;
    private boolean hasClicked;
    private Vec3d moveDir;

    @Override
    public boolean shouldRebuild() {
        return Mod.shouldReload && Mod.endTime < 10;
    }

    @Shadow
    @Nullable
    public ClientPlayerEntity player;
    double lookingTime;

    @Override
    public HitResult getLocation() {
        return location;
    }

    @Override
    public void setLocation(HitResult location) {
        this.location = location;
    }
    @Override
    public Vec3d getOriginalLocation() {
        return originalLocation;
    }
    @Override
    public void setOriginalLocation(Vec3d location) {
        this.originalLocation = location;
    }
    @Shadow
    abstract void doItemUse();


    @Shadow
    @Final
    public GameOptions options;
    public boolean   isIndoors;
    public HitResult location;

    private static Vec3d movementInputToVelocity(Vec3d movementInput, float speed, float yaw) {
        double d = movementInput.lengthSquared();
        if (d < 1.0E-7) {
            return Vec3d.ZERO;
        } else {
            Vec3d vec3d = (d > 1.0 ? movementInput.normalize() : movementInput).multiply((double)speed);
            float f = MathHelper.sin(yaw * 0.017453292F);
            float g = MathHelper.cos(yaw * 0.017453292F);
            return new Vec3d(vec3d.x * (double)g - vec3d.z * (double)f, vec3d.y, vec3d.z * (double)g + vec3d.x * (double)f);
        }
    }



    @Inject(method = "tick", at = @At("HEAD"))
    public void tickXIVHEAD(CallbackInfo ci) {
        MinecraftClient client = (MinecraftClient) (Object) this;
        boolean spell = false;
        if (ModCompat.isModLoaded("spell_engine")) {

            spell = SpellEngineCompat.isCasting();
        }
        boolean isController = false;
        Mod.zoom = Math.clamp(Mod.zoom,1F,10F);

        if (ModCompat.isModLoaded("midnightcontrols")) {
            isController = MidnightControlsCompat.isEnabled();
        }
        if(MinecraftClient.getInstance().player != null) {
            for(int i = 0; i < 9; ++i) {
                if (this.options.hotbarKeys[i].isPressed() && MinecraftClient.getInstance().player.getInventory().selectedSlot != i) {
                    Mod.cooldownWas = 0;

                }
            }
        }
        // Diagnostics live outside the Mod.enabled gate on purpose: "the mod is off" is one of the
        // states worth being able to see, and a report you cannot reach when things are broken is
        // no use.
        if (client.world != null) {
            CullDebug.tickLog();
            if (ClientInit.cullDebugBinding.wasPressed()) {
                CullDebug.report();
            }
        }

        if (Mod.enabled && client.cameraEntity != null && client.player != null ) {

            double x = ((Mod.crosshairTarget != null ? Mod.crosshairTarget.getPos().subtract(client.cameraEntity.getPos()).getX():0));
            double y = ((Mod.crosshairTarget != null ? Mod.crosshairTarget.getPos().subtract(client.cameraEntity.getPos()).getZ():0));
            if((Mod.crosshairTarget != null && Mod.crosshairTarget.getPos().distanceTo(client.cameraEntity.getPos()) > client.player.getBlockInteractionRange()) ||
                    (Mod.crosshairTarget != null && Mod.crosshairTarget.getPos().distanceTo(client.cameraEntity.getPos()) > client.player.getEntityInteractionRange())) {

                Mod.x += MinecraftClient.getInstance().gameRenderer.getCamera().getLastTickDelta() * 0.10 * Mod.zoom * 1.5 * new Vec3d(x, 0, y).normalize().x;
                Mod.z += MinecraftClient.getInstance().gameRenderer.getCamera().getLastTickDelta() * 0.10 * Mod.zoom * 1.5 * new Vec3d(x, 0, y).normalize().z;
            }
            if(Mod.crosshairTarget != null) {
                Mod.x = Math.clamp(Mod.x, -Math.abs(new Vec3d(Mod.x, 0, Mod.z).normalize().getX()) * Mod.crosshairTarget.getPos().subtract(client.cameraEntity.getPos()).horizontalLength(), Math.abs(new Vec3d(Mod.x, 0, Mod.z).normalize().getX()) * Mod.crosshairTarget.getPos().subtract(client.cameraEntity.getPos()).horizontalLength());
                Mod.z = Math.clamp(Mod.z, -Math.abs(new Vec3d(Mod.x, 0, Mod.z).normalize().getZ()) * Mod.crosshairTarget.getPos().subtract(client.cameraEntity.getPos()).horizontalLength(), Math.abs(new Vec3d(Mod.x, 0, Mod.z).normalize().getZ()) * Mod.crosshairTarget.getPos().subtract(client.cameraEntity.getPos()).horizontalLength());

            }
            Mod.x = Math.clamp(Mod.x,-Math.abs(new Vec3d(Mod.x,0,Mod.z).normalize().getX())*Mod.zoom*1.5,Math.abs(new Vec3d(Mod.x,0,Mod.z).normalize().getX())*Mod.zoom*1.5);
            Mod.z = Math.clamp(Mod.z,-Math.abs(new Vec3d(Mod.x,0,Mod.z).normalize().getZ())*Mod.zoom*1.5,Math.abs(new Vec3d(Mod.x,0,Mod.z).normalize().getZ())*Mod.zoom*1.5);

            SodiumCompat.run();

            if(ClientInit.contextToggleBinding.wasPressed()){
                contextToggle = !contextToggle;
            }
            if(ClientInit.openLootMenu.wasPressed()){
                Screen lootScreen = OwoScreens.loot();
                if (lootScreen != null) {
                    MinecraftClient.getInstance().setScreen(lootScreen);
                }
            }
            if(ClientInit.rotateToggle.wasPressed()){
                rotateToggle = !rotateToggle;
            }

            // Contextual interactable-block targeting. Independent of combat targets and of the
            // mouse hit result: it only chooses a nearby usable block to highlight and lets the
            // Interact key act on it. Runs before click-to-move so the key is always processed.
            if (Config.GSON.instance().isContextualInteract()) {
                if (client.currentScreen == null) {
                    ContextualTargeting.TargetingInputMode interactInputMode = Config.GSON.instance().isMovementTargeting()
                            ? ContextualTargeting.TargetingInputMode.MOVEMENT
                            : ContextualTargeting.TargetingInputMode.POINTER;
                    Vec3d interactDir = interactInputMode == ContextualTargeting.TargetingInputMode.MOVEMENT
                            ? getMovementTargetingDirection(client)
                            : getPointerTargetingDirection(client.player, Mod.crosshairTarget);
                    Mod.targetedInteractable = ContextualInteractionTargeting.updateTarget(
                            client.player, interactDir, Mod.targetedInteractable, interactInputMode);
                } else {
                    Mod.targetedInteractable = null;
                }
                while (ClientInit.interact.wasPressed()) {
                    tryUseTargetedInteractable(client);
                }
            }

            if(  ClientInit.clickToMove.isPressed() && Config.GSON.instance().isClickToMove()) {

                if (  (Mod.crosshairTarget instanceof BlockHitResult hit &&  isInteractable(hit))){
                    Hand[] var1 = Hand.values();
                    for (Hand hand : var1) {
                        var interact = client.interactionManager.interactBlock(player, hand, hit);
                        if (interact.isAccepted()) {

                            ((MinecraftClientAccessor) client).setLocation(null);
                            ((MinecraftClientAccessor) client).setOriginalLocation(null);
                            if (interact.shouldSwingHand()) {
                                itemUseCooldown = 4;

                                this.player.swingHand(hand);
                                return;

                            }
                        }
                    }

                }
                else if (   (Mod.crosshairTarget instanceof EntityHitResult hit && hit.getPos().distanceTo(player.getEyePos()) <= player.getEntityInteractionRange()/2) ){
                    Hand[] var1 = Hand.values();
                    for (Hand hand : var1) {
                        var interact = client.interactionManager.interactEntity(player, hit.getEntity(), hand);

                        if (interact.isAccepted()) {

                            ((MinecraftClientAccessor) client).setLocation(null);
                            ((MinecraftClientAccessor) client).setOriginalLocation(null);
                            if (interact.shouldSwingHand()) {
                                itemUseCooldown = 4;

                                this.player.swingHand(hand);
                                return;

                            }
                        }
                    }
                    Mod.using = true;

                }
                else
                if(  Mod.crosshairTarget != null && !(Mod.crosshairTarget instanceof BlockHitResult hit &&  isInteractable(hit))) {

                    ((MinecraftClientAccessor) client).setLocation(Mod.crosshairTarget);
                    ((MinecraftClientAccessor) client).setOriginalLocation(client.player.getPos());
                }

                return;
            }
            Mod.using = false;


            boolean bool = false;
            if(!player.isFallFlying() && this.location != null && Config.GSON.instance().isClickToMove()){
                Vec3d vec3d2 = new Vec3d((double)this.player.sidewaysSpeed, (double)this.player.upwardSpeed, (double)this.player.forwardSpeed);


           /*     while(this.options.pickItemKey.wasPressed() || this.options.useKey.wasPressed() || this.options.attackKey.wasPressed()){
                    if(Mod.crosshairTarget != null) {
                        this.player.lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, Mod.crosshairTarget.getPos());
                    }
                    return;
                }
                if (this.options.pickItemKey.isPressed() || this.options.useKey.isPressed() || this.options.attackKey.isPressed()){
                    if(Mod.crosshairTarget != null) {

                        this.player.lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, Mod.crosshairTarget.getPos());
                    }
                    return;
                }*/

            }
           /* if (this.options.attackKey.wasPressed()) {
                if(crosshairTarget instanceof EntityHitResult entityHitResult){
                    targeted = entityHitResult.getEntity();
                    pickCooldown = 20;
                }
            }*/
            if(this.options.attackKey.isPressed() ){
                mouseCooldown =  40+(int)(0.2F*20F/client.player.getAttributeValue(EntityAttributes.GENERIC_ATTACK_SPEED));
            }
            if (client.player.getMainHandStack().getItem() instanceof RangedWeaponItem ||
                    client.player.getMainHandStack().getItem() instanceof ProjectileItem ||
                    client.player.getMainHandStack().getItem() instanceof BowItem ||
                    client.player.getMainHandStack().getItem() instanceof CrossbowItem ||
                    client.player.isUsingItem()  ||
                    client.options.useKey.isPressed()
                    || spell
            ){
                bool = true;
                mouseCooldown = 40;
            }
            boolean bool2 = false;
            if (client.player.getMainHandStack().getItem() instanceof RangedWeaponItem ||
                    client.player.getMainHandStack().getItem() instanceof ProjectileItem ||
                    client.player.getMainHandStack().getItem() instanceof BowItem ||
                    client.player.getMainHandStack().getItem() instanceof CrossbowItem
            ){
                bool2 = true;
            }

            // Contextual combat target acquisition. Decides Mod.targeted (the soft combat-facing
            // target); the facing logic below then aims the character at it. Off by default and
            // fully skipped when disabled, so legacy targeting is untouched. The acquisition
            // direction comes from either the top-down cursor (POINTER) or movement input
            // (MOVEMENT) — the latter for controller / one-handed / keyboard-only play.
            if (Config.GSON.instance().isContextualTargeting()) {
                boolean rangedMode =
                        client.player.getMainHandStack().getItem() instanceof RangedWeaponItem ||
                        client.player.getMainHandStack().getItem() instanceof ProjectileItem ||
                        client.player.getMainHandStack().getItem() instanceof BowItem ||
                        client.player.getMainHandStack().getItem() instanceof CrossbowItem ||
                        client.player.isUsingItem() ||
                        spell;
                boolean meleeMode = (client.options.attackKey.isPressed() || mouseCooldown > 0 ) && !rangedMode;
                ContextualTargeting.TargetingMode combatMode = rangedMode
                        ? ContextualTargeting.TargetingMode.RANGED
                        : (meleeMode ? ContextualTargeting.TargetingMode.MELEE
                                     : ContextualTargeting.TargetingMode.NONE);

                ContextualTargeting.TargetingInputMode inputMode = Config.GSON.instance().isMovementTargeting()
                        ? ContextualTargeting.TargetingInputMode.MOVEMENT
                        : ContextualTargeting.TargetingInputMode.POINTER;

                Vec3d acquisitionDirection = inputMode == ContextualTargeting.TargetingInputMode.MOVEMENT
                        ? getMovementTargetingDirection(client)
                        : getPointerTargetingDirection(client.player, Mod.crosshairTarget);

                if (ContextualTargeting.isValidLockOn(client.player, Mod.lockOnTarget)) {
                    // Explicit hard lock-on overrides contextual soft targeting.
                    Mod.targeted = Mod.lockOnTarget;
                    idleTargetTicks = 0;
                } else {
                    Mod.lockOnTarget = null;
                    if (combatMode != ContextualTargeting.TargetingMode.NONE) {
                        idleTargetTicks = 0;
                        Mod.targeted = ContextualTargeting.updateTarget(
                                client.player, acquisitionDirection, Mod.targeted, combatMode, inputMode);
                    } else if (Config.GSON.instance().isContextualInteract()) {
                        // Out of combat: target nearby interactable entities (villagers, traders) so
                        // the Interact key can act on them (and the character faces/highlights them).
                        idleTargetTicks = 0;
                        Mod.targeted = ContextualTargeting.updateTarget(
                                client.player, acquisitionDirection, Mod.targeted,
                                ContextualTargeting.TargetingMode.INTERACT, inputMode);
                    } else {
                        handleTargetIdleState(inputMode);
                    }
                }
                // Keep the TAIL tick's pickCooldown reaper from clearing a live soft target.
                if (Mod.targeted != null) {
                    pickCooldown = 20;
                }
            }

            // Cursor-aim exception: even with turn-to-mouse off, projectile weapons and spell casts
            // still need to aim somewhere. Contextual combat targeting normally supplies that aim by
            // facing an acquired entity; when it can't (either targeting mode is off), a held ranged
            // weapon / spell cast falls back to aiming at the cursor. This is suppressed in full
            // movement/controller targeting (contextual + movement both on), which aims via the
            // movement stick and has no meaningful cursor. Combat-target facing still takes priority.
            boolean rangedHeld =
                    client.player.getMainHandStack().getItem() instanceof RangedWeaponItem // bows, crossbows
                    || client.player.getMainHandStack().getItem() instanceof ProjectileItem  // snowballs, eggs, pearls, potions
                    || client.player.getMainHandStack().getItem() instanceof TridentItem;
            boolean cursorAimException = (rangedHeld || spell)
                    && (!Config.GSON.instance().isContextualTargeting()
                        || !Config.GSON.instance().isMovementTargeting());

            // "Move-facing regime": turn-to-mouse is disabled, the player is not elytra-flying, and
            // the cursor-aim exception does not apply. In this regime the character NEVER looks at
            // the cursor — facing is driven only by the combat target or movement. While moving it
            // faces the movement direction; while standing still it keeps its current facing (looks
            // forward). This holds even while attacking or using an item. Fall-flying is excluded so
            // elytra steering (which uses look direction) still works.
            boolean moveFacingRegime = !Config.GSON.instance().isTurnToMouse() && !player.isFallFlying()
                    && !cursorAimException;

            // Anchor the renderer's rotation-interpolation state to the CURRENT rotation. The TAIL
            // tick copies these living* values into player.prev{Pitch,HeadYaw,BodyYaw} every tick;
            // if the facing logic below decides not to rotate (idle in no-turn-to-mouse mode, e.g.
            // right after switching into the perspective), leaving them stale makes the renderer
            // lerp stale→current each tick → the model jitters until you move. lookAt() overwrites
            // these with the same current values when it runs, so this is a no-op in that case.
            Mod.livingYaw = client.player.getYaw();
            Mod.livingPitch = client.player.getPitch();
            Mod.livingHeadYaw = client.player.getHeadYaw();
            Mod.livingBodyYaw = client.player.bodyYaw;

            if(Mod.targeted != null && !targeted.isInvisibleTo(client.player) && client.player.canSee(targeted)) {
                EntityHitResult result = new EntityHitResult(targeted,targeted.getEyePos());
                lookAt(client.player, EntityAnchorArgumentType.EntityAnchor.EYES, result.getPos(),true);

            }else
                if (moveFacingRegime && client.player.input.getMovementInput().length() > 0.1) {
                    if(Mod.targeted != null){
                        EntityHitResult result = new EntityHitResult(targeted,targeted.getEyePos());
                        lookAt(client.player, EntityAnchorArgumentType.EntityAnchor.EYES, result.getPos(),true);

                    }else {
                        if (client.player.getVehicle() != null) {
                            Vec3d vec3d = movementInputToVelocity(new Vec3d(client.player.input.movementSideways, 0, client.player.input.movementForward), 1.0F, client.player.getVehicle().getYaw());
                            if (vec3d.lengthSquared() > 1e-6) {
                                lookAt(client.player, EntityAnchorArgumentType.EntityAnchor.EYES, client.player.getEyePos().add(vec3d.normalize()), true);
                            }
                        } else {
                            // Use current raw WASD keys + current Mod.yaw instead of player.getMovement()
                            // (physics velocity). getMovement() lags by one tick and rotates with the camera
                            // each tick during middle-click drag, causing the player yaw to jitter as it
                            // tries to track a direction that changes every frame.
                            float rawFwd  = (client.options.forwardKey.isPressed() ? 1f : 0f)
                                          - (client.options.backKey.isPressed()    ? 1f : 0f);
                            float rawSide = (client.options.leftKey.isPressed()    ? 1f : 0f)
                                          - (client.options.rightKey.isPressed()   ? 1f : 0f);
                            // No movement key held but the left stick is deflected: face the stick
                            // direction (keys take priority). Mod.joystickRaw* is in the same
                            // key-input convention as rawFwd/rawSide.
                            if (rawFwd == 0 && rawSide == 0
                                    && (Mod.joystickRawFwd != 0 || Mod.joystickRawSide != 0)) {
                                rawFwd  = Mod.joystickRawFwd;
                                rawSide = Mod.joystickRawSide;
                            }
                            if (rawFwd != 0 || rawSide != 0) {
                                 moveDir = movementInputToVelocity(new Vec3d(rawSide, 0, rawFwd), 1.0F, Mod.yaw);
                                lookAt(client.player, EntityAnchorArgumentType.EntityAnchor.EYES,
                                       client.player.getEyePos().add(moveDir), true);
                            }
                            else{
                                if(moveDir != null){
                                    lookAt(client.player, EntityAnchorArgumentType.EntityAnchor.EYES,
                                            client.player.getEyePos().add(moveDir), true);
                                }
                            }
                        }
                    }
                    Mod.prevCrosshairTarget = client.crosshairTarget;
                    lookingTime = client.world.getTime();

                    //client.player.getVehicle().lookAt(EntityAnchorArgumentType.EntityAnchor.EYES,client.player.getVehicle().getEyePos().add(vec3d.normalize()));
                } else {
                    if (player.isFallFlying()) {
                        Mod.prevCrosshairTarget = Mod.crosshairTarget;
                    }
                    GameRenderer renderer = client.gameRenderer;
                    Camera camera = renderer.getCamera();
                    float tickDelta = camera.getLastTickDelta();

                    if (Mod.crosshairTarget != null) {

/*
                    if( mouseCooldown >= 40 || mouseCooldown <= 0){
*/

                        if(Mod.targeted != null){
                            EntityHitResult result = new EntityHitResult(targeted,targeted.getEyePos());
                            lookAt(client.player, EntityAnchorArgumentType.EntityAnchor.EYES, result.getPos(),true);

                        }else if (Config.GSON.instance().isTurnToMouse() || player.isFallFlying() || cursorAimException) {
                            // Face the cursor when turn-to-mouse is enabled, while elytra-flying (so
                            // steering works), or under the cursor-aim exception (aiming a ranged
                            // weapon / spell with no movement-based aim assist). Otherwise, with
                            // turn-to-mouse off, the character never looks at the cursor — movement/
                            // target facing above governs it, and standing still keeps the facing.
                            //
                            // Cosmetic look target: flatten to eye level so the character doesn't
                            // crane up at overhead blocks (forest canopy) in normal mouse-look.
                            // Vertical look is kept for entities, while aiming/casting, and for
                            // blocks worth acting on (right tool to harvest, or interactable).
                            // Interaction targeting is decoupled (see GameRendererMixin), so this
                            // never affects where actions land.
                            Vec3d lookTarget = crosshairTarget.getPos();
                            boolean allowVertical = spell || client.player.isUsingItem()
                                    || crosshairTarget instanceof EntityHitResult;
                            if (!allowVertical && crosshairTarget instanceof BlockHitResult blockHit && client.world != null) {
                                BlockState state = client.world.getBlockState(blockHit.getBlockPos());
                                if (!state.isAir() && (client.player.getMainHandStack().getMiningSpeedMultiplier(state) > 1.5f
                                        || Mod.isInteractable(blockHit))) {
                                    allowVertical = true;
                                }
                            }
                            if (!allowVertical) {
                                lookTarget = new Vec3d(lookTarget.x, client.player.getEyePos().y, lookTarget.z);
                            }
                            lookAt(client.player, EntityAnchorArgumentType.EntityAnchor.EYES, lookTarget,true);
                        }

                        /*
                    }
*/


                    }

                }



            // if(player.getVehicle() != null){
            //     player.setHeadYaw(MathHelper.clampAngle(player.headYaw, player.getVehicle().getYaw() , (float) ( 135)));

            //     player.setYaw( MathHelper.clampAngle(player.headYaw, player.getVehicle().getYaw() , (float) ( 135)));
            //     player.bodyYaw = player.getYaw();



            // }
                /*

                double d = Mod.crosshairTarget.getPos().x - vec3d.x;
                double e = Mod.crosshairTarget.getPos().y - vec3d.y;
                double f = Mod.crosshairTarget.getPos().z - vec3d.z;
                double g = Math.sqrt(d * d + f * f);
                double d2 = Mod.prevCrosshairTarget.getPos().x - vec3d.x;
                double e2 = Mod.prevCrosshairTarget.getPos().y - vec3d.y;
                double f2 = Mod.prevCrosshairTarget.getPos().z - vec3d.z;
                double g2 = Math.sqrt(d2 * d2 + f2 * f2);
                client.player.setPitch(MathHelper.lerp(tickDelta,MathHelper.wrapDegrees((float)(-(MathHelper.atan2(e2, g2) * 57.2957763671875)))  ,MathHelper.wrapDegrees((float)(-(MathHelper.atan2(e, g) * 57.2957763671875)))  ));
                client.player.setYaw(MathHelper.lerp(tickDelta,MathHelper.wrapDegrees((float)(MathHelper.atan2(f2, d2) * 57.2957763671875) - 90.0F)  ,MathHelper.wrapDegrees((float)(MathHelper.atan2(f, d) * 57.2957763671875) - 90.0F)  ));
                client.player.setHeadYaw(MathHelper.lerp(tickDelta,MathHelper.wrapDegrees((float)(MathHelper.atan2(f2, d2) * 57.2957763671875) - 90.0F) ,MathHelper.wrapDegrees((float)(MathHelper.atan2(f, d) * 57.2957763671875) - 90.0F) ));
*/


        }
        else{

            Mod.crosshairTarget = null;
            Mod.prevCrosshairTarget = null;
            Mod.targetedInteractable = null;
        }

        if(MinecraftClient.getInstance().world != null && MinecraftClient.getInstance().world.getTime()-Mod.dirtyTime > 40) {
            if(Mod.dirty) {
                endTime = 0;
                zoomOutTime = 0;
            }
            Mod.dirty = false;

        }
        if(isBlocked){
            Mod.frustrumZoom++;

        }
        else{
            Mod.frustrumZoom--;

        }
        if(shouldReload){

        }
        else if(Mod.endTime < 10) {
            Mod.endTime++;
        }
        if(shouldReload ){


            Mod.blockedTime++;

        }

        else if(zoomOutTime < 10) {
            Mod.frustrumZoom--;
            zoomOutTime++;
            blockedTime = 0;
        }
        else{
            blockedTime = 0;

        }
       if(zoomTimeNoDelay < 10) {
            Mod.zoomOutTimeNoDelay++;
        }

    }
    /**
     * Pointer-mode acquisition direction: horizontal world direction from the eyes to the cursor's
     * world position. Returns null when there is no usable cursor direction.
     */
    private Vec3d getPointerTargetingDirection(ClientPlayerEntity player, HitResult mouseTarget) {
        if (mouseTarget == null) {
            return null;
        }
        Vec3d flat = mouseTarget.getPos().subtract(player.getEyePos()).multiply(1.0, 0.0, 1.0);
        return flat.lengthSquared() < 1.0e-6 ? null : flat.normalize();
    }

    /**
     * Movement-mode acquisition direction: camera-relative world direction from the player's raw
     * movement input (keyboard or controller analog stick), reusing the same conversion the
     * movement-facing logic uses. Deliberately NOT physical velocity, which lags, slides, and is
     * perturbed by knockback. Returns {@link Vec3d#ZERO} inside the neutral deadzone.
     */
    private Vec3d getMovementTargetingDirection(MinecraftClient client) {
        // input.movementSideways/Forward is the active input implementation's normalized movement
        // vector, so controllers are supported without special-casing.
        Vec3d raw = new Vec3d(client.player.input.movementSideways, 0, client.player.input.movementForward);
        if (raw.lengthSquared() < 0.04) {
            return Vec3d.ZERO;
        }
        return movementInputToVelocity(raw, 1.0F, Mod.yaw).normalize();
    }

    /**
     * Handle a tick with no combat action. Pointer mode clears the soft target immediately (mouse
     * aiming re-acquires instantly). Movement mode retains it across brief gaps between attacks/
     * casts and only clears after a sustained idle window; a dead/removed target is dropped at once.
     * Explicit lock-on is never cleared here.
     */
    private void handleTargetIdleState(ContextualTargeting.TargetingInputMode inputMode) {
        if (inputMode == ContextualTargeting.TargetingInputMode.MOVEMENT) {
            if (Mod.targeted instanceof LivingEntity l && (!l.isAlive() || l.isRemoved())) {
                Mod.targeted = null;
                ContextualTargeting.clearSoftTarget();
                idleTargetTicks = 0;
                return;
            }
            idleTargetTicks++;
            if (idleTargetTicks >= MOVEMENT_TARGET_IDLE_CLEAR_TICKS) {
                Mod.targeted = null;
                ContextualTargeting.clearSoftTarget();
            }
        } else {
            Mod.targeted = null;
        }
    }

    /**
     * Perform a normal vanilla interaction on the contextual interactable target when the Interact
     * key is pressed. Priority: the retained {@link Mod#targetedInteractable}; else the mouse
     * hit result if it is an interactable block in range; else one immediate contextual scan. Does
     * nothing (no walking, no packets) when no in-range target is found. Uses
     * {@link net.minecraft.client.network.ClientPlayerInteractionManager#interactBlock} — never
     * {@code doItemUse()} (which would use the crosshair target) and never custom packets.
     */
    private void tryUseTargetedInteractable(MinecraftClient client) {
        if (client.player == null || client.world == null
                || client.interactionManager == null || client.currentScreen != null) {
            return;
        }
        // An interactable entity target (villager/trader acquired out of combat) takes priority when
        // it is in reach — vanilla entity interaction, no walking, no custom packets.
        if (Mod.targeted instanceof LivingEntity entity
                && ContextualTargeting.isInteractableEntity(entity)
                && client.player.canInteractWithEntity(entity, 0.0)) {
            for (Hand hand : Hand.values()) {
                var result = client.interactionManager.interactEntity(client.player, entity, hand);
                if (result.isAccepted()) {
                    if (result.shouldSwingHand()) {
                        client.player.swingHand(hand);
                    }
                    itemUseCooldown = 4;
                    return;
                }
            }
        }
        BlockHitResult hitResult = null;
        if (Mod.targetedInteractable != null && InteractionTargeting.isInteractable(client.player.clientWorld, Mod.targetedInteractable)) {
            hitResult = ContextualInteractionTargeting.resolveHit(client.player, Mod.targetedInteractable);
        } else if (Mod.crosshairTarget instanceof BlockHitResult crossHit
                && crossHit.getType() == HitResult.Type.BLOCK && InteractionTargeting.isInteractable(client.player.clientWorld,crossHit.getBlockPos())) {
            hitResult = crossHit;
        } else {
            ContextualTargeting.TargetingInputMode inputMode = Config.GSON.instance().isMovementTargeting()
                    ? ContextualTargeting.TargetingInputMode.MOVEMENT
                    : ContextualTargeting.TargetingInputMode.POINTER;
            Vec3d dir = inputMode == ContextualTargeting.TargetingInputMode.MOVEMENT
                    ? getMovementTargetingDirection(client)
                    : getPointerTargetingDirection(client.player, Mod.crosshairTarget);
            BlockPos scanned = ContextualInteractionTargeting.updateTarget(client.player, dir, null, inputMode);
            if (scanned != null) {
                Mod.targetedInteractable = scanned;
                hitResult = ContextualInteractionTargeting.resolveHit(client.player, scanned);
            }
        }
        if (hitResult == null) {
            return;
        }
        for (Hand hand : Hand.values()) {
            var result = client.interactionManager.interactBlock(client.player, hand, hitResult);
            if (result.isAccepted()) {
                if (result.shouldSwingHand()) {
                    client.player.swingHand(hand);
                }
                itemUseCooldown = 4;
                return;
            }
        }
    }

    private  void lookAt(LivingEntity living, EntityAnchorArgumentType.EntityAnchor anchorPoint, Vec3d target) {
        lookAt(living,anchorPoint,target,false);
    }


    private  void lookAt(LivingEntity living, EntityAnchorArgumentType.EntityAnchor anchorPoint, Vec3d target, boolean bool) {
        Vec3d vec3d = anchorPoint.positionAt(living);
        double d = target.x - vec3d.x;
        double e = target.y - vec3d.y;
        double f = target.z - vec3d.z;
        double g = Math.sqrt(d * d + f * f);

        Mod.livingPitch = living.getPitch();
        Mod.livingBodyYaw = living.bodyYaw;
        Mod.livingYaw = living.getYaw();
        Mod.livingHeadYaw = living.getHeadYaw();
        double headyaw = MathHelper.wrapDegrees((float)(MathHelper.atan2(f, d) * 57.2957763671875) - 90.0F);

        // Normalize prevHeadYaw to avoid 360° wrap in rendering interpolation

        while(headyaw - living.prevHeadYaw < -180.0F) {
            living.prevHeadYaw -= 360.0F;
        }
        while(headyaw - living.prevHeadYaw >= 180.0F) {
            living.prevHeadYaw += 360.0F;
        }

        // Normalize prevBodyYaw against the same angle for the same reason.
        // Previously this used a velocity-based bodyyaw which lagged by one tick —
        // when moveCameraBinding is held (or during camera rotation), velocity angle
        // and headYaw diverge, causing prevBodyYaw to oscillate each tick and the
        // renderer's lerp(prevBodyYaw, bodyYaw) to jitter visually.
        while(headyaw - living.prevBodyYaw < -180.0F) {
            living.prevBodyYaw -= 360.0F;
        }
        while(headyaw - living.prevBodyYaw >= 180.0F) {
            living.prevBodyYaw += 360.0F;
        }

        living.setHeadYaw((float) headyaw);

        // Ease pitch toward its target over a few ticks instead of snapping. Even with the
        // eye-level look target, pitch can still step when the player starts/stops aiming or a
        // genuine vertical target is picked; exponential smoothing at the fixed 20 TPS tick makes
        // those glide. The renderer's prevPitch->pitch interpolation covers per-frame smoothness.
        float targetPitch = MathHelper.wrapDegrees((float)(-(MathHelper.atan2(e, g) * 57.2957763671875)));
        living.setPitch(MathHelper.lerpAngleDegrees(PITCH_SMOOTHING, living.getPitch(), targetPitch));

        boolean spell = false;
        if (ModCompat.isModLoaded("spell_engine")) {
            spell = SpellEngineCompat.isCasting();
        }

        living.setYaw((float) headyaw);
        // Body faces the same direction as head — no velocity-based clamping.
        // clampAngle(velocityAngle, headYaw, 35°) was the old approach; velocity
        // lags by one tick so its angle is stale, producing a different bodyYaw
        // every tick that oscillates around headYaw → visible body-rotation jitter.
        living.bodyYaw = living.getHeadYaw();
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
    public boolean first = false;
    public boolean firstTimeGuiShown = false;
    public boolean isUse = false;
    public boolean disableTargeting = false;


    @Inject(method = "tick", at = @At("TAIL"))
    public void tickXIV(CallbackInfo ci) {
       MinecraftClient client =  (MinecraftClient)  (Object) this;
        if (this.player == null) {
            firstTimeGuiShown = false;
            return;
        }
        if (client.currentScreen == null && !firstTimeGuiShown && Config.GSON.instance().showFirstTimeGui && !com.cleannrooster.dungeons_iso.config.FirstTimeState.get().choiceMade) {
            firstTimeGuiShown = true;
            // Null without owo-lib. The choice is deliberately not recorded as made in that case,
            // so installing owo later still gets the player the prompt; the default (on) applies
            // meanwhile, which is what answering the prompt with "enable" would have done anyway.
            Screen firstTime = OwoScreens.firstTime();
            if (firstTime != null) {
                client.setScreen(firstTime);
            }
        }
        Mod.zoom = Math.clamp(Mod.zoom,1F,10F);

        boolean isController = false;

        if (ModCompat.isModLoaded("midnightcontrols")) {
            isController = MidnightControlsCompat.isEnabled();
        }
        if (client.currentScreen == null && ( Config.GSON.instance().force || (Config.GSON.instance().onStartup && !first) ||ClientInit.toggleBinding.wasPressed() || (
                this.options.togglePerspectiveKey.isPressed() && Mod.enabled
        ))) {
            if (!Config.GSON.instance().force && Mod.enabled) {
                Mod.enabled = false;

                options.setPerspective(Mod.lastPerspective);
                Util.debug("Disabled Minecraft XIV");
                if(client.currentScreen == null) {
                    InputUtil.setCursorParameters(client.getWindow().getHandle(), GLFW.GLFW_CURSOR_DISABLED,client.mouse.getX(), client.mouse.getY());

                }
                client.mouse.lockCursor();

            } else
            if(!Mod.enabled && client.world != null && client.player != null) {
                MinecraftClient.getInstance().options.setPerspective(Perspective.FIRST_PERSON);
                if (Config.GSON.instance().onStartup) {
                    first = true;
                }
                Mod.enabled = true;

                Mod.lastPerspective = this.options.getPerspective();
                this.options.setPerspective(Perspective.THIRD_PERSON_BACK);
                if (Mod.lastPerspective == Perspective.THIRD_PERSON_FRONT) {
                    Mod.yaw = ((180 + this.player.getYaw() + 180) % 360) - 180;
                    Mod.pitch = -this.player.getPitch();
                } else {
                    Mod.yaw = this.player.getYaw();
                    Mod.pitch = this.player.getPitch();
                }
                Util.debug("Enabled Minecraft XIV");
                client.mouse.lockCursor();

                InputUtil.setCursorParameters(client.getWindow().getHandle(), GLFW.GLFW_CURSOR_NORMAL, client.mouse.getX(), client.mouse.getY());
            }
            MinecraftClient.getInstance().worldRenderer.reload();
        }

        if (ClientInit.zoomInBinding.wasPressed()) {
            if (Mod.enabled) {
                Mod.zoom = Math.clamp(Mod.zoom - 0.2f, 2F/Math.clamp(Config.GSON.instance().zoomFactor,1F,1.5F),10.0F);
            }
        }

        if (ClientInit.zoomOutBinding.wasPressed()) {
            if (Mod.enabled) {
                Mod.zoom = Math.clamp(Mod.zoom + 0.2f,2F/Math.clamp(Config.GSON.instance().zoomFactor,1F,1.5F), 10.0F);
            }
        }

        if (Mod.lockOnTarget != null && !Mod.lockOnTarget.isAlive()) {
            Mod.lockOnTarget = null;
        }
        boolean bool = false;
        boolean spell = false;
        if (ModCompat.isModLoaded("spell_engine")) {

            spell = SpellEngineCompat.isCasting();
        }

        if(client.player != null && Mod.enabled) {

            DragonCompat.getDragonDistanceMultiplier();


            if (this.options.attackKey.isPressed()) {
                mouseCooldown = 40 + (int) (0.2*20F / client.player.getAttributeValue(EntityAttributes.GENERIC_ATTACK_SPEED));
            }
            if (client.player.isUsingItem() || (this.options.useKey.isPressed()) || spell) {
                isUse = true;
                bool = true;
            }
            boolean bool2 = false;
            if (client.player.getMainHandStack().getItem() instanceof RangedWeaponItem ||
                    client.player.getMainHandStack().getItem() instanceof ProjectileItem ||
                    client.player.getMainHandStack().getItem() instanceof BowItem ||
                    client.player.getMainHandStack().getItem() instanceof CrossbowItem
            ){
                bool2 = true;
            }
            if (client.player.getMainHandStack().getItem() instanceof RangedWeaponItem ||
                    client.player.getMainHandStack().getItem() instanceof ProjectileItem ||
                    client.player.getMainHandStack().getItem() instanceof BowItem ||
                    client.player.getMainHandStack().getItem() instanceof CrossbowItem ||
                    client.player.isUsingItem()  ||
                    client.options.useKey.isPressed()
                    || spell
            ){
                bool = true;
            }
            if((originalLocation != null && location != null  && !(this.location instanceof EntityHitResult entityHitResult) && this.player.squaredDistanceTo(originalLocation) >= this.originalLocation.squaredDistanceTo(location.getPos())-0.5) || this.player.input.pressingBack ||
                    this.player.input.pressingRight ||
                    this.player.input.pressingLeft||
                    this.player.input.pressingForward){

                this.location = null;
                this.originalLocation = null;

            }
            else{
                if(originalLocation != null && location != null && this.location instanceof EntityHitResult entityHitResult) {
                    this.location = new EntityHitResult(entityHitResult.getEntity(),entityHitResult.getEntity().getPos());
                    this.originalLocation = this.player.getPos();

                }
            }
         /*   if((FabricLoader.getInstance().isModLoaded("bettercombat") && mouseCooldown > 0 && !bool2  && Config.GSON.instance().additionalMeleeAssistance )){
                Entity entity = null;
                var additionMod =  player.getEntityInteractionRange() * 1.25;
                List<LivingEntity> living = player.getWorld().getEntitiesByClass(LivingEntity.class,player.getBoundingBox().expand(additionMod),
                        (target) ->{
                            return target != player && player.canSee(target) &&  target.distanceTo(player) < additionMod
                                    && target.getPos().subtract(player.getPos().subtract(player.getRotationVec(1.0F).multiply(additionMod))).normalize().dotProduct(player.getRotationVec(1.0F).normalize()) > 0.5F
                                    && target.getPos().subtract(player.getPos()).normalize().dotProduct(player.getRotationVec(1.0F).normalize()) > 0;
                        });
                if(!living.isEmpty()) {
                    var vec3d =  player.getPos();
                    living.sort(Comparator.comparing((a) -> a.getPos().distanceTo(vec3d)));
                    if (crosshairTarget instanceof EntityHitResult result) {
                        pickedTarget = result.getEntity();
                    }
                    else if(!disableTargeting) {
                        entity = living.get(0);
                    }
                    else{
                        pickedTarget = null;
                    }
                    if(ClientInit.cycleTargetBinding.wasPressed()){
                        if(disableTargeting) {
                            if (!living.isEmpty()) {
                                pickedTarget = living.get(0);
                                disableTargeting = false;
                            }
                        }
                        else if(pickedTarget == null ) {
                            if (!living.isEmpty()) {
                                pickedTarget = living.get(0);
                            }
                        }
                        else{
                            if(living.contains(pickedTarget)) {

                                if (living.size() > living.indexOf(pickedTarget) + 1) {
                                    pickedTarget = living.get(living.indexOf(pickedTarget) + 1);
                                    disableTargeting = false;

                                } else {
                                    disableTargeting = true;
                                    pickedTarget = null;

                                    player.sendMessage(Text.translatable("Disabled Targeting"));

                                }
                            }
                            else{
                                disableTargeting = true;
                                pickedTarget = null;

                                player.sendMessage(Text.translatable("Disabled Targeting"));

                            }
                        }

                    }
                    if(Mod.pickedTarget != null && Mod.pickedTarget instanceof LivingEntity livingPicekdTarget  && living.contains(livingPicekdTarget) && livingPicekdTarget.isAlive()) {

                        Mod.crosshairTarget = new EntityHitResult(Mod.pickedTarget,Mod.pickedTarget.getEyePos());
                        Mod.prevCrosshairTarget = new EntityHitResult(Mod.pickedTarget, Mod.pickedTarget.getEyePos());
                        Mod.targeted = Mod.pickedTarget;

                    }
                    else {
                        Mod.crosshairTarget = entity != null ? new EntityHitResult(entity, living.getFirst().getEyePos()) : crosshairTarget;
                        Mod.prevCrosshairTarget = entity != null ?new EntityHitResult(entity, living.getFirst().getEyePos()) : prevCrosshairTarget;
                        Mod.targeted = entity;


                    }

                }
                else{
                    Mod.targeted = null;
                    pickedTarget = null;

                }

            }
            else{
                Mod.targeted = null;
                pickedTarget = null;


            }*/
/*
            if (  (  (!Config.GSON.instance().turnToMouse &&     !player.isFallFlying()) && (
                       (!(bool ) && !using && mouseCooldown <= 0  && !(player.getMainHandStack().getItem() instanceof CrossbowItem item ) &&  client.player.input.getMovementInput().length() > 0.1)))) {
                if(Mod.targeted != null){
                    EntityHitResult result = new EntityHitResult(targeted,targeted.getEyePos());
                    lookAt(client.player, EntityAnchorArgumentType.EntityAnchor.EYES, result.getPos(),true);

                }else {
                    if (client.player.getVehicle() != null) {
                        Vec3d vec3d = movementInputToVelocity(new Vec3d(client.player.input.movementSideways, 0, client.player.input.movementForward), 1.0F, client.player.getVehicle().getYaw());
                        lookAt(client.player, EntityAnchorArgumentType.EntityAnchor.EYES, client.player.getEyePos().add(vec3d.normalize()), true);
                    } else {
                        lookAt(client.player, EntityAnchorArgumentType.EntityAnchor.EYES, client.player.getEyePos().add(client.player.getMovement().subtract(
                                0, client.player.getMovement().getY(), 0).normalize()), true);

                    }
                }
                lookingTime = client.world.getTime();


                //client.player.getVehicle().lookAt(EntityAnchorArgumentType.EntityAnchor.EYES,client.player.getVehicle().getEyePos().add(vec3d.normalize()));
            } else {

                GameRenderer renderer = client.gameRenderer;
                Camera camera = renderer.getCamera();
                float tickDelta = camera.getLastTickDelta();

                if (targeted != null) {


                    if (!player.isFallFlying()) {

                        lookAt(client.player,EntityAnchorArgumentType.EntityAnchor.EYES, new Vec3d(
                                Mod.targeted.getPos().getX(),
                                Mod.targeted.getEyeY(),
                                Mod.targeted.getPos().getZ()),true);
                    }


                }
                else
                if (Mod.crosshairTarget != null) {



                    if (!player.isFallFlying()) {

                        // Cosmetic look target: flatten to eye level so the character doesn't crane
                        // up at overhead blocks (forest canopy) in normal mouse-look. Vertical look
                        // is kept for entities, while aiming/casting, and for blocks worth acting on
                        // (the held item is the right tool to harvest it, or it is interactable).
                        // Interaction targeting is decoupled (see GameRendererMixin), so this never
                        // affects where actions actually land.
                        Vec3d lookTarget = crosshairTarget.getPos();
                        boolean allowVertical = spell || client.player.isUsingItem()
                                || crosshairTarget instanceof EntityHitResult;
                        if (!allowVertical && crosshairTarget instanceof BlockHitResult blockHit && client.world != null) {
                            BlockState state = client.world.getBlockState(blockHit.getBlockPos());
                            if (!state.isAir() && (client.player.getMainHandStack().getMiningSpeedMultiplier(state) > 1.5f
                                    || Mod.isInteractable(blockHit))) {
                                allowVertical = true;
                            }
                        }
                        if (!allowVertical) {
                            lookTarget = new Vec3d(lookTarget.x, client.player.getEyePos().y, lookTarget.z);
                        }
                        lookAt(client.player,EntityAnchorArgumentType.EntityAnchor.EYES, lookTarget,true);
                    }


                }

            }
            if(Mod.targeted != null){
                EntityHitResult result = new EntityHitResult(targeted,targeted.getEyePos());
                lookAt(client.player, EntityAnchorArgumentType.EntityAnchor.EYES, result.getPos(),true);

            }

            if(player.getVehicle() != null){
                player.setHeadYaw(MathHelper.clampAngle(player.headYaw, player.getVehicle().getYaw() , (float) ( 135)));

                player.setYaw( MathHelper.clampAngle(player.headYaw, player.getVehicle().getYaw() , (float) ( 135)));
                player.bodyYaw = player.getYaw();


            }
            */
            if(Objects.nonNull(hit)) {
                notmoving = false;
                if (hit.getType().equals(HitResult.Type.BLOCK)) {
                    double toadd = -1.0F;
                    Mod.forward = false;

                    if (cooldown <= 0) {
                        toadd = -1.0F;

                        Mod.forward = false;
                        blockedTime = 0;

                    }
                    clipMetric = (float) clipMetric + (float) toadd;

                    if (clipMetric < 8) {
                        clipMetric = 8;
                    }


                } else {
                    if (!shouldReload) {
                        Mod.clipMetric += 0.4F;
                        Mod.forward = true;
                        cooldown = 20;
                    } else {

                        Mod.notmoving = true;
                    }
                    if (Mod.clipMetric > 32) {
                        clipMetric = 32;
                    }
                }
                //clipMetric = (float) Math.clamp(clipMetric,Math.min(16F,player.getHeight()),Math.max(Math.min(16F,player.getHeight()),16));



            }
            // client.player.prevYaw = (livingYaw);
            client.player.prevPitch = (livingPitch);
            client.player.prevHeadYaw = (livingHeadYaw);
            client.player.prevBodyYaw = (livingBodyYaw);
        }
        if(pickCooldown <= 0){
            targeted = null;
        }
        Mod.cooldown--;
        Mod.cooldownWas++;
        mouseCooldown--;
        pickCooldown--;
    }

    @Inject(method = "hasOutline", at = @At("HEAD"), cancellable = true)
    public void hasOutlineXIV(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if(Mod.enabled &&Mod.crosshairTarget instanceof EntityHitResult hitResult){
            if(entity.equals(hitResult.getEntity()) || entity.equals(targeted)){
                if(!ClientInit.lockOn.isPressed()) {
                    cir.setReturnValue(true);
                }
            }
        }
        if(Mod.enabled && entity.equals(targeted)){
            cir.setReturnValue(true);

        }
        if(Mod.enabled && player != null && entity == player ){
/*
            if (M) {
                if(crosshairTarget instanceof EntityHitResult entityHitResult){
                    targeted = entityHitResult.getEntity();
                    pickCooldown = 20;
                }
            }*/
            if(  player.getWorld().raycast(new RaycastContext(
                    MinecraftClient.getInstance().gameRenderer.getCamera().getPos(),
                    entity.getEyePos(),
                    RaycastContext.ShapeType.VISUAL,
                    RaycastContext.FluidHandling.NONE,
                    player
            )).getType() == HitResult.Type.BLOCK) {

            }
            else{

            }

        }

    }
    private int mouseCooldown = 40;

    private int pickCooldown = 20;


    public int getMouseCooldown() {
        return mouseCooldown;
    }

    public int setMouseCooldown(int cooldown) {
        this.mouseCooldown = cooldown;
        return this.mouseCooldown;
    }
    @Inject(method = "doAttack", at = @At("HEAD"), cancellable = true)

    private void doAttackXIV(CallbackInfoReturnable<Boolean> ci) {
            if (Config.GSON.instance().additionalMeleeAssistance && crosshairTarget instanceof EntityHitResult entityHitResult) {
                targeted = entityHitResult.getEntity();
                pickCooldown = 20;
            }
    }
}