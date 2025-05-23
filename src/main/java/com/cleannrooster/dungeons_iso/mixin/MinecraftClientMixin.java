package com.cleannrooster.dungeons_iso.mixin;

import com.cleannrooster.dungeons_iso.api.ChunkDataAccessor;
import com.cleannrooster.dungeons_iso.api.WorldRendererAccessor;
import com.cleannrooster.dungeons_iso.compat.SodiumCompat;
import com.cleannrooster.dungeons_iso.config.Config;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.Window;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ProjectileItem;
import net.minecraft.item.ThrowablePotionItem;
import net.minecraft.server.command.DebugCommand;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.gen.chunk.DebugChunkGenerator;
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
import com.cleannrooster.dungeons_iso.api.MinecraftClientAccessor;
import com.cleannrooster.dungeons_iso.util.Util;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin implements MinecraftClientAccessor {
    public boolean shouldRebuild;
    private boolean canUseItem;

@Shadow
    private int itemUseCooldown;
    private Vec3d originalLocation;

    @Override
    public boolean shouldRebuild() {
        return shouldRebuild;
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
            if(MinecraftClient.getInstance().player != null) {
                for(int i = 0; i < 9; ++i) {
                    if (this.options.hotbarKeys[i].isPressed() && MinecraftClient.getInstance().player.getInventory().selectedSlot != i) {
                        Mod.cooldownWas = 0;

                    }
                }
            }
        if (Mod.enabled && client.cameraEntity != null && client.player != null) {
            HitResult hitResultOrigin = client.cameraEntity.getWorld().raycast(new RaycastContext(
                    client.player.getEyePos(),
                    client.gameRenderer.getCamera().getPos(),
                    RaycastContext.ShapeType.VISUAL,
                    RaycastContext.FluidHandling.NONE,
                    client.cameraEntity
            ));
            if(client.mouse.wasRightButtonClicked() && Config.GSON.instance().clickToMove) {
                ((MinecraftClientAccessor) client).setLocation(Mod.crosshairTarget);
                ((MinecraftClientAccessor) client).setOriginalLocation(client.player.getPos());
            }

            this.shouldRebuild = hitResultOrigin.getType().equals(HitResult.Type.BLOCK);
            if(FabricLoader.getInstance().isModLoaded("sodium")){
                SodiumCompat.run();
            }
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
            if(this.options.attackKey.isPressed()){
                mouseCooldown =  40+(int)(0.4F*20F/client.player.getAttributeValue(EntityAttributes.GENERIC_ATTACK_SPEED));
            }
            if (client.player.getMainHandStack().getItem() instanceof ProjectileItem || client.player.getMainHandStack().getItem() instanceof BowItem || client.player.getMainHandStack().getItem() instanceof CrossbowItem || client.player.isUsingItem() || ( this.options.useKey.isPressed()) || spell ) {
                mouseCooldown = 40;
                bool = true;
            }
            if (      !player.isFallFlying() && (
                    !bool && (mouseCooldown <= 0 && client.player.input.getMovementInput().length() > 0.1))) {
                if (client.player.getVehicle() != null) {
                    Vec3d vec3d = movementInputToVelocity(new Vec3d(client.player.input.movementSideways, 0, client.player.input.movementForward), 1.0F, client.player.getVehicle().getYaw());
                    client.player.lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, client.player.getEyePos().add(vec3d.normalize()));
                } else {
                    client.player.lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, client.player.getEyePos().add(client.player.getMovement().subtract(
                            0, client.player.getMovement().getY(), 0).normalize()));

                }
                Mod.prevCrosshairTarget = client.crosshairTarget;
                lookingTime = client.world.getTime();

                //client.player.getVehicle().lookAt(EntityAnchorArgumentType.EntityAnchor.EYES,client.player.getVehicle().getEyePos().add(vec3d.normalize()));
            }
            else {
                if (      player.isFallFlying()){
                    Mod.prevCrosshairTarget = Mod.crosshairTarget;
                }
                GameRenderer renderer = client.gameRenderer;
                Camera camera = renderer.getCamera();
                float tickDelta = camera.getLastTickDelta();

                if (Mod.crosshairTarget != null) {

                    if( mouseCooldown >= 40 || mouseCooldown <= 0){
                        if (Mod.prevCrosshairTarget == null) {
                            Mod.prevCrosshairTarget = Mod.crosshairTarget;
                        }
                        if (!player.isFallFlying()) {

                            client.player.lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, new Vec3d(
                                    MathHelper.lerp((Math.min(10, client.world.getTime() - lookingTime + tickDelta)) / 10D, Mod.prevCrosshairTarget.getPos().getX(), Mod.crosshairTarget.getPos().getX()),
                                    MathHelper.lerp((Math.min(10, client.world.getTime() - lookingTime + tickDelta)) / 10D, Mod.prevCrosshairTarget.getPos().getY(), Mod.crosshairTarget.getPos().getY()),
                                    MathHelper.lerp((Math.min(10, client.world.getTime() - lookingTime + tickDelta)) / 10D, Mod.prevCrosshairTarget.getPos().getZ(), Mod.crosshairTarget.getPos().getZ())));
                        }
                    }



                }
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
    }


    @Inject(method = "tick", at = @At("TAIL"))
    public void tickXIV(CallbackInfo ci) {
        MinecraftClient client =  (MinecraftClient)  (Object) this;
        if (this.player == null) {
            return;
        }

        // For some reason, KeyBinding#wasPressed doesn't work here, so I'm using KeyBinding#isPressed, which doesn't
        // seem to break anything.
        //if (ClientInit.getInstance().getKeyBinding().wasPressed() || (this.options.togglePerspectiveKey.wasPressed
        // () && mod.isEnabled())) {
        if(Mod.enabled && client.worldRenderer != null ){
            ((WorldRendererAccessor)client.worldRenderer).chunks().forEach(builtChunk -> {{
                builtChunk.scheduleRebuild(true);
            }});
        }

        if (ClientInit.toggleBinding.wasPressed() || (
                this.options.togglePerspectiveKey.isPressed() && Mod.enabled
        )) {
            if (Mod.enabled) {
                Mod.enabled = false;

                options.setPerspective(Mod.lastPerspective);
                Util.debug("Disabled Minecraft XIV");
                if(client.currentScreen == null) {
                    InputUtil.setCursorParameters(client.getWindow().getHandle(), GLFW.GLFW_CURSOR_DISABLED,client.mouse.getX(), client.mouse.getY());

                }

            } else {
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

                InputUtil.setCursorParameters(client.getWindow().getHandle(), GLFW.GLFW_CURSOR_NORMAL, client.mouse.getX(), client.mouse.getY());
            }



            // Re-lock the cursor so it correctly changes state
            client.mouse.lockCursor();
        }

        if (ClientInit.zoomInBinding.wasPressed()) {
            if (Mod.enabled) {
                Mod.zoom = Math.max(Mod.zoom - 0.2f, 0.0f);
            }
        }

        if (ClientInit.zoomOutBinding.wasPressed()) {
            if (Mod.enabled) {
                Mod.zoom = Math.min(Mod.zoom + 0.2f, 5.0f);
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
            if (this.options.attackKey.isPressed()) {
                mouseCooldown = 40 + (int) (0.4*20F / client.player.getAttributeValue(EntityAttributes.GENERIC_ATTACK_SPEED));
            }
            if (client.player.isUsingItem() || (this.options.useKey.isPressed()) || spell) {
                mouseCooldown = 40;
                bool = true;
            }
            if((originalLocation != null && location != null && this.player.squaredDistanceTo(originalLocation) >= this.originalLocation.squaredDistanceTo(location.getPos())-0.5) || this.player.input.pressingBack ||
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
            if (      !player.isFallFlying() && (
                !bool && (mouseCooldown <= 0 && client.player.input.getMovementInput().length() > 0.1))) {
                if (client.player.getVehicle() != null) {
                    Vec3d vec3d = movementInputToVelocity(new Vec3d(client.player.input.movementSideways, 0, client.player.input.movementForward), 1.0F, client.player.getVehicle().getYaw());
                    client.player.lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, client.player.getEyePos().add(vec3d.normalize()));
                } else {
                    client.player.lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, client.player.getEyePos().add(client.player.getMovement().subtract(
                            0, client.player.getMovement().getY(), 0).normalize()));

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
                if (Mod.crosshairTarget != null) {

                    if( mouseCooldown >= 40 || mouseCooldown <= 0){
                        if (Mod.prevCrosshairTarget == null) {
                            Mod.prevCrosshairTarget = Mod.crosshairTarget;
                        }
                        if (!player.isFallFlying()) {

                            client.player.lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, new Vec3d(
                                    MathHelper.lerp((Math.min(10, client.world.getTime() - lookingTime + tickDelta)) / 10D, Mod.prevCrosshairTarget.getPos().getX(), Mod.crosshairTarget.getPos().getX()),
                                    MathHelper.lerp((Math.min(10, client.world.getTime() - lookingTime + tickDelta)) / 10D, Mod.prevCrosshairTarget.getPos().getY(), Mod.crosshairTarget.getPos().getY()),
                                    MathHelper.lerp((Math.min(10, client.world.getTime() - lookingTime + tickDelta)) / 10D, Mod.prevCrosshairTarget.getPos().getZ(), Mod.crosshairTarget.getPos().getZ())));
                        }
                    }


                }

            }

        }

        Mod.cooldownWas++;
        mouseCooldown--;

    }

    @Inject(method = "hasOutline", at = @At("HEAD"), cancellable = true)
    public void hasOutlineXIV(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if(Mod.enabled &&Mod.crosshairTarget instanceof EntityHitResult hitResult){
            if(entity.equals(hitResult.getEntity())){
                if(!ClientInit.lockOn.isPressed()) {
                    cir.setReturnValue(true);
                }
            }
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