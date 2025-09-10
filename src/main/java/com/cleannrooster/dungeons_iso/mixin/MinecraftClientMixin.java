package com.cleannrooster.dungeons_iso.mixin;

import com.cleannrooster.dungeons_iso.api.*;
import com.cleannrooster.dungeons_iso.compat.DragonCompat;
import com.cleannrooster.dungeons_iso.compat.MidnightControlsCompat;
import com.cleannrooster.dungeons_iso.compat.SodiumCompat;
import com.cleannrooster.dungeons_iso.config.Config;
import com.cleannrooster.dungeons_iso.ui.LootUI;
import com.google.common.collect.Lists;
import net.fabricmc.loader.api.FabricLoader;
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
import com.cleannrooster.dungeons_iso.util.Util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.cleannrooster.dungeons_iso.mod.Mod.*;


@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin implements MinecraftClientAccessor {
    private boolean canUseItem;

    @Shadow
    private int itemUseCooldown;
    private Vec3d originalLocation;
    private boolean hasClicked;

    @Override
    public boolean shouldRebuild() {
        return Mod.shouldReload || Mod.endTime > 0;
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
        if (FabricLoader.getInstance().isModLoaded("spell_engine")) {

            spell = SpellEngineCompat.isCasting();
        }
        boolean isController = false;

        if (FabricLoader.getInstance().isModLoaded("midnightcontrols")) {
            isController = MidnightControlsCompat.isEnabled();
        }
        if(MinecraftClient.getInstance().player != null) {
            for(int i = 0; i < 9; ++i) {
                if (this.options.hotbarKeys[i].isPressed() && MinecraftClient.getInstance().player.getInventory().selectedSlot != i) {
                    Mod.cooldownWas = 0;

                }
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
                MinecraftClient.getInstance().setScreen(new LootUI());
            }
            if(ClientInit.rotateToggle.wasPressed()){
                rotateToggle = !rotateToggle;
            }
            if(  ClientInit.clickToMove.isPressed() && Config.GSON.instance().clickToMove) {

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
            if(!player.isFallFlying() && this.location != null && Config.GSON.instance().clickToMove){
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
            if(FabricLoader.getInstance().isModLoaded("bettercombat") && mouseCooldown > 0 &&  !bool2   && Config.GSON.instance().additionalMeleeAssistance ){
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
                    if(ClientInit.cycleTargetBinding.wasPressed()) {

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
                        else {
                            if (living.contains(pickedTarget)) {

                                if (living.size() > living.indexOf(pickedTarget) + 1) {
                                    pickedTarget = living.get(living.indexOf(pickedTarget) + 1);
                                    disableTargeting = false;

                                } else {
                                    disableTargeting = true;
                                    pickedTarget = null;

                                    player.sendMessage(Text.translatable("Disabled Targeting"));

                                }
                            } else {
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
                        Mod.crosshairTarget = entity != null ?  new EntityHitResult(entity, living.get(0).getEyePos()) : crosshairTarget;
                        Mod.prevCrosshairTarget = entity != null ? new EntityHitResult(entity, living.get(0).getEyePos()) : prevCrosshairTarget;
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


            }
                if (((!Config.GSON.instance().turnToMouse && !player.isFallFlying()) && (
                         !(bool ) && !using && mouseCooldown <= 0 && !(player.getMainHandStack().getItem() instanceof CrossbowItem item )&& ( client.player.input.getMovementInput().length() > 0.1)))) {
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
                        if (Mod.prevCrosshairTarget == null) {
                            Mod.prevCrosshairTarget = Mod.crosshairTarget;
                        }
                        Mod.prevCrosshairTarget = Mod.crosshairTarget;

                        if(Mod.targeted != null){
                            EntityHitResult result = new EntityHitResult(targeted,targeted.getEyePos());
                            lookAt(client.player, EntityAnchorArgumentType.EntityAnchor.EYES, result.getPos(),true);

                        }else {
                            lookAt(client.player, EntityAnchorArgumentType.EntityAnchor.EYES, new Vec3d(
                                    MathHelper.lerp((Math.min(10, client.world.getTime() - lookingTime + tickDelta)) / 10D, Mod.prevCrosshairTarget.getPos().getX(), Mod.crosshairTarget.getPos().getX()),
                                    MathHelper.lerp((Math.min(10, client.world.getTime() - lookingTime + tickDelta)) / 10D, Mod.prevCrosshairTarget.getPos().getY(), Mod.crosshairTarget.getPos().getY()),
                                    MathHelper.lerp((Math.min(10, client.world.getTime() - lookingTime + tickDelta)) / 10D, Mod.prevCrosshairTarget.getPos().getZ(), Mod.crosshairTarget.getPos().getZ())));
                        }

                        /*
                    }
*/


                    }

                }

            if(Mod.targeted != null){
                EntityHitResult result = new EntityHitResult(targeted,targeted.getEyePos());
                lookAt(client.player, EntityAnchorArgumentType.EntityAnchor.EYES, result.getPos(),true);

            }

            if(player.getVehicle() != null){
                player.setHeadYaw(MathHelper.clampAngle(player.headYaw, player.getVehicle().getYaw() , (float) ( 135)));
                player.prevHeadYaw = player.headYaw;

                player.setYaw( MathHelper.clampAngle(player.headYaw, player.getVehicle().getYaw() , (float) ( 135)));
                player.bodyYaw = player.getYaw();

                player.prevYaw = player.getYaw();
               player.prevBodyYaw = player.getYaw();

            }
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
    private  void lookAt(LivingEntity living, EntityAnchorArgumentType.EntityAnchor anchorPoint, Vec3d target) {
        lookAt(living,anchorPoint,target,false);
    }


    private  void lookAt(LivingEntity living, EntityAnchorArgumentType.EntityAnchor anchorPoint, Vec3d target, boolean bool) {
        Vec3d vec3d = anchorPoint.positionAt(living);
        double d = target.x - vec3d.x;
        double e = target.y - vec3d.y;
        double f = target.z - vec3d.z;
        double g = Math.sqrt(d * d + f * f);
        living.setPitch(MathHelper.wrapDegrees((float)(-(MathHelper.atan2(e, g) * 57.2957763671875))));
        living.setHeadYaw(MathHelper.wrapDegrees((float)(MathHelper.atan2(f, d) * 57.2957763671875) - 90.0F));
        living.prevHeadYaw = living.headYaw;
        living.setYaw( MathHelper.clampAngle(living.getYaw(), living.headYaw, (float) (this.isUse ? 0 : 5)));
        living.prevYaw = (living.getYaw());
        living.bodyYaw = (living.getYaw());
        living.prevBodyYaw = (living.bodyYaw);
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
    public boolean isUse = false;
    public boolean disableTargeting = false;


    @Inject(method = "tick", at = @At("TAIL"))
    public void tickXIV(CallbackInfo ci) {
        MinecraftClient client =  (MinecraftClient)  (Object) this;
        if (this.player == null) {
            return;
        }


        if(Mod.enabled && client.worldRenderer != null ){
            ((WorldRendererAccessor)client.worldRenderer).chunks().forEach(builtChunk -> {{
                builtChunk.scheduleRebuild(true);
            }});
        }
        boolean isController = false;

        if (FabricLoader.getInstance().isModLoaded("midnightcontrols")) {
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

        }

        if (ClientInit.zoomInBinding.wasPressed()) {
            if (Mod.enabled) {
                Mod.zoom = Math.clamp(Mod.zoom - 0.2f, 2F/Math.clamp(Config.GSON.instance().zoomFactor,1F,1.5F),5);
            }
        }

        if (ClientInit.zoomOutBinding.wasPressed()) {
            if (Mod.enabled) {
                Mod.zoom = Math.clamp(Mod.zoom + 0.2f,2F/Math.clamp(Config.GSON.instance().zoomFactor,1F,1.5F), 5.0F);
            }
        }

        if (Mod.lockOnTarget != null && !Mod.lockOnTarget.isAlive()) {
            Mod.lockOnTarget = null;
        }
        boolean bool = false;
        boolean spell = false;
        if (FabricLoader.getInstance().isModLoaded("spell_engine")) {

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
            if((FabricLoader.getInstance().isModLoaded("bettercombat") && mouseCooldown > 0 && !bool2  && Config.GSON.instance().additionalMeleeAssistance )){
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


            }

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
                Mod.prevCrosshairTarget = MinecraftClient.getInstance().crosshairTarget;


                //client.player.getVehicle().lookAt(EntityAnchorArgumentType.EntityAnchor.EYES,client.player.getVehicle().getEyePos().add(vec3d.normalize()));
            } else {

                GameRenderer renderer = client.gameRenderer;
                Camera camera = renderer.getCamera();
                float tickDelta = camera.getLastTickDelta();
                if (      player.isFallFlying()){
                    Mod.prevCrosshairTarget = Mod.crosshairTarget;
                }
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

                    if (Mod.prevCrosshairTarget == null) {
                        Mod.prevCrosshairTarget = Mod.crosshairTarget;
                    }
                    Mod.prevCrosshairTarget = Mod.crosshairTarget;


                    if (!player.isFallFlying()) {

                        lookAt(client.player,EntityAnchorArgumentType.EntityAnchor.EYES, new Vec3d(
                                MathHelper.lerp((Math.min(10, client.world.getTime() - lookingTime + tickDelta)) / 10D, Mod.prevCrosshairTarget.getPos().getX(), Mod.crosshairTarget.getPos().getX()),
                                MathHelper.lerp((Math.min(10, client.world.getTime() - lookingTime + tickDelta)) / 10D, Mod.prevCrosshairTarget.getPos().getY(), Mod.crosshairTarget.getPos().getY()),
                                MathHelper.lerp((Math.min(10, client.world.getTime() - lookingTime + tickDelta)) / 10D, Mod.prevCrosshairTarget.getPos().getZ(), Mod.crosshairTarget.getPos().getZ())),true);
                    }


                }

            }
            if(Mod.targeted != null){
                EntityHitResult result = new EntityHitResult(targeted,targeted.getEyePos());
                lookAt(client.player, EntityAnchorArgumentType.EntityAnchor.EYES, result.getPos(),true);

            }

            if(player.getVehicle() != null){
                player.setHeadYaw(MathHelper.clampAngle(player.headYaw, player.getVehicle().getYaw() , (float) ( 135)));
                player.prevHeadYaw = player.headYaw;

                player.setYaw( MathHelper.clampAngle(player.headYaw, player.getVehicle().getYaw() , (float) ( 135)));
                player.bodyYaw = player.getYaw();

                player.prevYaw = player.getYaw();
                player.prevBodyYaw = player.getYaw();

            }
            if(Objects.nonNull(hit)) {
                notmoving = false;
                if (hit.getType().equals(HitResult.Type.BLOCK)) {
                    double toadd = -1.0F;
                    Mod.forward = false;

                    if( cooldown <= 0){
                        toadd = -1.0F;

                        Mod.forward = false;
                        blockedTime = 0;

                    }
                    clipMetric =  (float) clipMetric+(float) toadd;

                    if(clipMetric < 8){
                        clipMetric = 8;
                    }


                } else {
                    if (!shouldReload) {
                        Mod.clipMetric += 0.4F;
                        Mod.forward = true;
                        cooldown = 20;
                    }
                    else{

                        Mod.notmoving = true;
                    }
                    if (Mod.clipMetric > 32) {
                        clipMetric = 32;
                    }
                }
                //clipMetric = (float) Math.clamp(clipMetric,Math.min(16F,player.getHeight()),Math.max(Math.min(16F,player.getHeight()),16));

            }
        }
        Mod.cooldown--;
        Mod.cooldownWas++;
        mouseCooldown--;

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



    public int getMouseCooldown() {
        return mouseCooldown;
    }

    public int setMouseCooldown(int cooldown) {
        this.mouseCooldown = cooldown;
        return this.mouseCooldown;
    }
    @Inject(method = "disconnect(Lnet/minecraft/client/gui/screen/Screen;)V", at = @At("HEAD"))
    public void disconnectPre(Screen screen, CallbackInfo ci) {
        ClientInit.capabilities = null;
    }
}