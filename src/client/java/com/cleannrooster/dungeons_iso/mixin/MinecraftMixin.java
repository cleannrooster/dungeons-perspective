package com.cleannrooster.dungeons_iso.mixin;

import com.cleannrooster.dungeons_iso.ClientInit;
import com.cleannrooster.dungeons_iso.ModCompat;
import com.cleannrooster.dungeons_iso.api.MinecraftAccessor;
import com.cleannrooster.dungeons_iso.compat.DragonCompat;
import com.cleannrooster.dungeons_iso.compat.MidnightControlsCompat;
import com.cleannrooster.dungeons_iso.compat.SodiumCompat;
import com.cleannrooster.dungeons_iso.compat.SpellEngineCompat;
import com.cleannrooster.dungeons_iso.config.Config;
import com.cleannrooster.dungeons_iso.config.FirstTimeState;
import com.cleannrooster.dungeons_iso.mod.Mod;
import com.cleannrooster.dungeons_iso.ui.FirstTimeScreen;
import com.cleannrooster.dungeons_iso.ui.LootUI;
import com.cleannrooster.dungeons_iso.util.Util;
import net.minecraft.client.*;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.*;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

import static com.cleannrooster.dungeons_iso.mod.Mod.*;


@Mixin(value = Minecraft.class,priority = 0 )
public abstract class MinecraftMixin implements MinecraftAccessor {
    private boolean canUseItem;

    @Shadow
    private int itemUseCooldown;
    private Vec3 originalLocation;
    private boolean hasClicked;

    @Override
    public boolean shouldRebuild() {
        return Mod.shouldReload && Mod.endTime < 10;
    }

    @Shadow
    @Nullable
    public LocalPlayer player;
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
    public Vec3 getOriginalLocation() {
        return originalLocation;
    }
    @Override
    public void setOriginalLocation(Vec3 location) {
        this.originalLocation = location;
    }
    @Shadow
    abstract void doItemUse();


    @Shadow
    @Final
    public Options options;
    public boolean   isIndoors;
    public HitResult location;

    private static Vec3 movementInputToVelocity(Vec3 movementInput, float speed, float yaw) {
        double d = movementInput.lengthSqr();
        if (d < 1.0E-7) {
            return Vec3.ZERO;
        } else {
            Vec3 vec3d = (d > 1.0 ? movementInput.normalize() : movementInput).multiply((double)speed);
            float f = Mth.sin(yaw * 0.017453292F);
            float g = Mth.cos(yaw * 0.017453292F);
            return new Vec3(vec3d.x * (double)g - vec3d.z * (double)f, vec3d.y, vec3d.z * (double)g + vec3d.x * (double)f);
        }
    }



    @Inject(method = "tick", at = @At("HEAD"))
    public void tickXIVHEAD(CallbackInfo ci) {
        Minecraft client = (Minecraft) (Object) this;
        boolean spell = false;
        if (ModCompat.isModLoaded("spell_engine")) {

            spell = SpellEngineCompat.isCasting();
        }
        boolean isController = false;
        Mod.zoom = Math.clamp(Mod.zoom,1F,10F);

        if (ModCompat.isModLoaded("midnightcontrols")) {
            isController = MidnightControlsCompat.isEnabled();
        }
        if(Minecraft.getInstance().player != null) {
            for(int i = 0; i < 9; ++i) {
                if (this.options.hotbarKeys[i].isPressed() && Minecraft.getInstance().player.getInventory().selectedSlot != i) {
                    Mod.cooldownWas = 0;

                }
            }
        }
        if (Mod.enabled && client.cameraEntity != null && client.player != null ) {

            double x = ((Mod.crosshairTarget != null ? Mod.crosshairTarget.getPos().subtract(client.cameraEntity.getPos()).getX():0));
            double y = ((Mod.crosshairTarget != null ? Mod.crosshairTarget.getPos().subtract(client.cameraEntity.getPos()).getZ():0));
            if((Mod.crosshairTarget != null && Mod.crosshairTarget.getPos().distanceTo(client.cameraEntity.getPos()) > client.player.getBlockInteractionRange()) ||
                    (Mod.crosshairTarget != null && Mod.crosshairTarget.getPos().distanceTo(client.cameraEntity.getPos()) > client.player.getEntityInteractionRange())) {

                Mod.x += Minecraft.getInstance().gameRenderer.getMainCamera().getLastTickDelta() * 0.10 * Mod.zoom * 1.5 * new Vec3(x, 0, y).normalize().x;
                Mod.z += Minecraft.getInstance().gameRenderer.getMainCamera().getLastTickDelta() * 0.10 * Mod.zoom * 1.5 * new Vec3(x, 0, y).normalize().z;
            }
            if(Mod.crosshairTarget != null) {
                Mod.x = Math.clamp(Mod.x, -Math.abs(new Vec3(Mod.x, 0, Mod.z).normalize().getX()) * Mod.crosshairTarget.getPos().subtract(client.cameraEntity.getPos()).horizontalLength(), Math.abs(new Vec3(Mod.x, 0, Mod.z).normalize().getX()) * Mod.crosshairTarget.getPos().subtract(client.cameraEntity.getPos()).horizontalLength());
                Mod.z = Math.clamp(Mod.z, -Math.abs(new Vec3(Mod.x, 0, Mod.z).normalize().getZ()) * Mod.crosshairTarget.getPos().subtract(client.cameraEntity.getPos()).horizontalLength(), Math.abs(new Vec3(Mod.x, 0, Mod.z).normalize().getZ()) * Mod.crosshairTarget.getPos().subtract(client.cameraEntity.getPos()).horizontalLength());

            }
            Mod.x = Math.clamp(Mod.x,-Math.abs(new Vec3(Mod.x,0,Mod.z).normalize().getX())*Mod.zoom*1.5,Math.abs(new Vec3(Mod.x,0,Mod.z).normalize().getX())*Mod.zoom*1.5);
            Mod.z = Math.clamp(Mod.z,-Math.abs(new Vec3(Mod.x,0,Mod.z).normalize().getZ())*Mod.zoom*1.5,Math.abs(new Vec3(Mod.x,0,Mod.z).normalize().getZ())*Mod.zoom*1.5);

            SodiumCompat.run();

            if(ClientInit.contextToggleBinding.wasPressed()){
                contextToggle = !contextToggle;
            }
            if(ClientInit.openLootMenu.wasPressed()){
                Minecraft.getInstance().setScreen(new LootUI());
            }
            if(ClientInit.rotateToggle.wasPressed()){
                rotateToggle = !rotateToggle;
            }
            if(  ClientInit.clickToMove.isPressed() && Config.GSON.instance().clickToMove) {

                if (  (Mod.crosshairTarget instanceof BlockHitResult hit &&  isInteractable(hit))){
                    InteractionHand[] var1 = InteractionHand.values();
                    for (InteractionHand hand : var1) {
                        var interact = client.interactionManager.interactBlock(player, hand, hit);
                        if (interact.isAccepted()) {

                            ((MinecraftAccessor) client).setLocation(null);
                            ((MinecraftAccessor) client).setOriginalLocation(null);
                            if (interact.shouldSwingHand()) {
                                itemUseCooldown = 4;

                                this.player.swing(hand);
                                return;

                            }
                        }
                    }

                }
                else if (   (Mod.crosshairTarget instanceof EntityHitResult hit && hit.getPos().distanceTo(player.getEyePos()) <= player.getEntityInteractionRange()/2) ){
                    InteractionHand[] var1 = InteractionHand.values();
                    for (InteractionHand hand : var1) {
                        var interact = client.interactionManager.interactEntity(player, hit.getEntity(), hand);

                        if (interact.isAccepted()) {

                            ((MinecraftAccessor) client).setLocation(null);
                            ((MinecraftAccessor) client).setOriginalLocation(null);
                            if (interact.shouldSwingHand()) {
                                itemUseCooldown = 4;

                                this.player.swing(hand);
                                return;

                            }
                        }
                    }
                    Mod.using = true;

                }
                else
                if(  Mod.crosshairTarget != null && !(Mod.crosshairTarget instanceof BlockHitResult hit &&  isInteractable(hit))) {

                    ((MinecraftAccessor) client).setLocation(Mod.crosshairTarget);
                    ((MinecraftAccessor) client).setOriginalLocation(client.player.getPos());
                }

                return;
            }
            Mod.using = false;


            boolean bool = false;
            if(!player.isFallFlying() && this.location != null && Config.GSON.instance().clickToMove){
                Vec3 vec3d2 = new Vec3((double)this.player.sidewaysSpeed, (double)this.player.upwardSpeed, (double)this.player.forwardSpeed);


           /*     while(this.options.pickItemKey.wasPressed() || this.options.useKey.wasPressed() || this.options.attackKey.wasPressed()){
                    if(Mod.crosshairTarget != null) {
                        this.player.lookAt(EntityAnchorArgument.EntityAnchor.EYES, Mod.crosshairTarget.getPos());
                    }
                    return;
                }
                if (this.options.pickItemKey.isPressed() || this.options.useKey.isPressed() || this.options.attackKey.isPressed()){
                    if(Mod.crosshairTarget != null) {

                        this.player.lookAt(EntityAnchorArgument.EntityAnchor.EYES, Mod.crosshairTarget.getPos());
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
            if(Mod.targeted != null && !targeted.isInvisibleTo(client.player) && client.player.canSee(targeted)) {
                EntityHitResult result = new EntityHitResult(targeted,targeted.getEyePos());
                lookAt(client.player, EntityAnchorArgument.EntityAnchor.EYES, result.getPos(),true);

            }else
                if (((!Config.GSON.instance().turnToMouse && !player.isFallFlying()) && (
                         !(bool ) && !using && mouseCooldown <= 0 && !(player.getMainHandStack().getItem() instanceof CrossbowItem item )&& ( client.player.input.getMovementInput().length() > 0.1)))) {
                    if(Mod.targeted != null){
                        EntityHitResult result = new EntityHitResult(targeted,targeted.getEyePos());
                        lookAt(client.player, EntityAnchorArgument.EntityAnchor.EYES, result.getPos(),true);

                    }else {
                        if (client.player.getVehicle() != null) {
                            Vec3 vec3d = movementInputToVelocity(new Vec3(client.player.input.movementSideways, 0, client.player.input.movementForward), 1.0F, client.player.getVehicle().getYRot());
                            if (vec3d.lengthSquared() > 1e-6) {
                                lookAt(client.player, EntityAnchorArgument.EntityAnchor.EYES, client.player.getEyePos().add(vec3d.normalize()), true);
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
                            if (rawFwd != 0 || rawSide != 0) {
                                Vec3 moveDir = movementInputToVelocity(new Vec3(rawSide, 0, rawFwd), 1.0F, Mod.yaw);
                                lookAt(client.player, EntityAnchorArgument.EntityAnchor.EYES,
                                       client.player.getEyePos().add(moveDir), true);
                            }
                        }
                    }
                    Mod.prevCrosshairTarget = client.crosshairTarget;
                    lookingTime = client.world.getTime();

                    //client.player.getVehicle().lookAt(EntityAnchorArgument.EntityAnchor.EYES,client.player.getVehicle().getEyePos().add(vec3d.normalize()));
                } else {
                    if (player.isFallFlying()) {
                        Mod.prevCrosshairTarget = Mod.crosshairTarget;
                    }
                    GameRenderer renderer = client.gameRenderer;
                    Camera camera = renderer.getMainCamera();
                    float tickDelta = camera.getLastTickDelta();

                    if (Mod.crosshairTarget != null) {

/*
                    if( mouseCooldown >= 40 || mouseCooldown <= 0){
*/

                        if(Mod.targeted != null){
                            EntityHitResult result = new EntityHitResult(targeted,targeted.getEyePos());
                            lookAt(client.player, EntityAnchorArgument.EntityAnchor.EYES, result.getPos(),true);

                        }else {
                            lookAt(client.player, EntityAnchorArgument.EntityAnchor.EYES, crosshairTarget.getPos(),true);
                        }

                        /*
                    }
*/


                    }

                }



            // if(player.getVehicle() != null){
            //     player.setHeadYaw(Mth.clampAngle(player.headYaw, player.getVehicle().getYRot() , (float) ( 135)));

            //     player.setYaw( Mth.clampAngle(player.headYaw, player.getVehicle().getYRot() , (float) ( 135)));
            //     player.bodyYaw = player.getYRot();



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
                client.player.setPitch(Mth.lerp(tickDelta,Mth.wrapDegrees((float)(-(Mth.atan2(e2, g2) * 57.2957763671875)))  ,Mth.wrapDegrees((float)(-(Mth.atan2(e, g) * 57.2957763671875)))  ));
                client.player.setYaw(Mth.lerp(tickDelta,Mth.wrapDegrees((float)(Mth.atan2(f2, d2) * 57.2957763671875) - 90.0F)  ,Mth.wrapDegrees((float)(Mth.atan2(f, d) * 57.2957763671875) - 90.0F)  ));
                client.player.setHeadYaw(Mth.lerp(tickDelta,Mth.wrapDegrees((float)(Mth.atan2(f2, d2) * 57.2957763671875) - 90.0F) ,Mth.wrapDegrees((float)(Mth.atan2(f, d) * 57.2957763671875) - 90.0F) ));
*/


        }
        else{

            Mod.crosshairTarget = null;
            Mod.prevCrosshairTarget = null;
        }

        if(Minecraft.getInstance().world != null && Minecraft.getInstance().world.getTime()-Mod.dirtyTime > 40) {
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
    private  void lookAt(LivingEntity living, EntityAnchorArgument.EntityAnchor anchorPoint, Vec3 target) {
        lookAt(living,anchorPoint,target,false);
    }


    private  void lookAt(LivingEntity living, EntityAnchorArgument.EntityAnchor anchorPoint, Vec3 target, boolean bool) {
        Vec3 vec3d = anchorPoint.positionAt(living);
        double d = target.x - vec3d.x;
        double e = target.y - vec3d.y;
        double f = target.z - vec3d.z;
        double g = Math.sqrt(d * d + f * f);

        Mod.livingPitch = living.getXRot();
        Mod.livingBodyYaw = living.bodyYaw;
        Mod.livingYaw = living.getYRot();
        Mod.livingHeadYaw = living.getHeadYaw();
        double headyaw = Mth.wrapDegrees((float)(Mth.atan2(f, d) * 57.2957763671875) - 90.0F);

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
        living.setPitch(Mth.wrapDegrees((float)(-(Mth.atan2(e, g) * 57.2957763671875))));

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
            BlockPos.Mutable mutable = new BlockPos.Mutable(j, k, l);
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
                double v = s * (p > 0 ? 1.0 - Mth.fractionalPart(g) : Mth.fractionalPart(g));
                double w = t * (q > 0 ? 1.0 - Mth.fractionalPart(h) : Mth.fractionalPart(h));
                double x = u * (r > 0 ? 1.0 - Mth.fractionalPart(i) : Mth.fractionalPart(i));

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
       Minecraft client =  (Minecraft)  (Object) this;
        if (this.player == null) {
            firstTimeGuiShown = false;
            return;
        }
        if (client.currentScreen == null && !firstTimeGuiShown && Config.GSON.instance().showFirstTimeGui && !FirstTimeState.get().choiceMade) {
            firstTimeGuiShown = true;
            client.setScreen(new FirstTimeScreen());
        }
        Mod.zoom = Math.clamp(Mod.zoom,1F,10F);

        /*if(Mod.enabled && client.worldRenderer != null ){
            ((WorldRendererAccessor)client.worldRenderer).chunks().forEach(builtChunk -> {{
                builtChunk.scheduleRebuild(true);
            }});
        }*/
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
                    InputConstants.setCursorParameters(client.getWindow().getHandle(), GLFW.GLFW_CURSOR_DISABLED,client.mouse.getX(), client.mouse.getY());

                }
                client.mouse.lockCursor();

            } else
            if(!Mod.enabled && client.world != null && client.player != null) {
                Minecraft.getInstance().options.setPerspective(Perspective.FIRST_PERSON);
                if (Config.GSON.instance().onStartup) {
                    first = true;
                }
                Mod.enabled = true;

                Mod.lastPerspective = this.options.getPerspective();
                this.options.setPerspective(Perspective.THIRD_PERSON_BACK);
                if (Mod.lastPerspective == Perspective.THIRD_PERSON_FRONT) {
                    Mod.yaw = ((180 + this.player.getYRot() + 180) % 360) - 180;
                    Mod.pitch = -this.player.getXRot();
                } else {
                    Mod.yaw = this.player.getYRot();
                    Mod.pitch = this.player.getXRot();
                }
                Util.debug("Enabled Minecraft XIV");
                client.mouse.lockCursor();

                InputConstants.setCursorParameters(client.getWindow().getHandle(), GLFW.GLFW_CURSOR_NORMAL, client.mouse.getX(), client.mouse.getY());
            }
            Minecraft.getInstance().worldRenderer.reload();
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
            if((originalLocation != null && location != null  && !(this.location instanceof EntityHitResult entityHitResult) && this.player.distanceToSqr(originalLocation) >= this.originalLocation.distanceToSqr(location.getPos())-0.5) || this.player.input.pressingBack ||
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
                    lookAt(client.player, EntityAnchorArgument.EntityAnchor.EYES, result.getPos(),true);

                }else {
                    if (client.player.getVehicle() != null) {
                        Vec3 vec3d = movementInputToVelocity(new Vec3(client.player.input.movementSideways, 0, client.player.input.movementForward), 1.0F, client.player.getVehicle().getYRot());
                        lookAt(client.player, EntityAnchorArgument.EntityAnchor.EYES, client.player.getEyePos().add(vec3d.normalize()), true);
                    } else {
                        lookAt(client.player, EntityAnchorArgument.EntityAnchor.EYES, client.player.getEyePos().add(client.player.getMovement().subtract(
                                0, client.player.getMovement().getY(), 0).normalize()), true);

                    }
                }
                lookingTime = client.world.getTime();


                //client.player.getVehicle().lookAt(EntityAnchorArgument.EntityAnchor.EYES,client.player.getVehicle().getEyePos().add(vec3d.normalize()));
            } else {

                GameRenderer renderer = client.gameRenderer;
                Camera camera = renderer.getMainCamera();
                float tickDelta = camera.getLastTickProgress();

                if (targeted != null) {


                    if (!player.isFallFlying()) {

                        lookAt(client.player,EntityAnchorArgument.EntityAnchor.EYES, new Vec3(
                                Mod.targeted.getPos().getX(),
                                Mod.targeted.getEyeY(),
                                Mod.targeted.getPos().getZ()),true);
                    }


                }
                else
                if (Mod.crosshairTarget != null) {



                    if (!player.isFallFlying()) {

                        lookAt(client.player,EntityAnchorArgument.EntityAnchor.EYES, crosshairTarget.getPos(),true);
                    }


                }

            }
            if(Mod.targeted != null){
                EntityHitResult result = new EntityHitResult(targeted,targeted.getEyePos());
                lookAt(client.player, EntityAnchorArgument.EntityAnchor.EYES, result.getPos(),true);

            }

            if(player.getVehicle() != null){
                player.setHeadYaw(Mth.clampAngle(player.headYaw, player.getVehicle().getYRot() , (float) ( 135)));

                player.setYaw( Mth.clampAngle(player.headYaw, player.getVehicle().getYRot() , (float) ( 135)));
                player.bodyYaw = player.getYRot();


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

                client.player.prevYaw = (livingYaw);
                client.player.prevPitch = (livingPitch);
                client.player.prevHeadYaw = (livingHeadYaw);
                client.player.prevBodyYaw = (livingBodyYaw);

            }
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
            if(  player.getWorld().raycast(new ClipContext(
                    Minecraft.getInstance().gameRenderer.getMainCamera().getPos(),
                    entity.getEyePos(),
                    ClipContext.ShapeType.VISUAL,
                    ClipContext.FluidHandling.NONE,
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