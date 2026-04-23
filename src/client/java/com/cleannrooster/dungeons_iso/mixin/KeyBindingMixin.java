package com.cleannrooster.dungeons_iso.mixin;

import com.cleannrooster.dungeons_iso.ClientInit;
import com.cleannrooster.dungeons_iso.api.MouseAccessor;
import com.cleannrooster.dungeons_iso.config.Config;
import com.cleannrooster.dungeons_iso.mod.Mod;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ProjectileWeaponItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(KeyMapping.class)
    public class KeyBindingMixin {
        @Inject(method = "isPressed", at = @At("RETURN"), cancellable = true)
    
        public void isPressedMouse(CallbackInfoReturnable<Boolean> cir) {
            if (((KeyMapping) (Object) this).equals(ClientInit.clickToMove) && Mod.enabled && Config.GSON.instance().clickToMove) {
                if(Minecraft.getInstance().currentScreen == null && Minecraft.getInstance().mouse.wasRightButtonClicked()){
                    cir.setReturnValue(true);
    
                }
                else{
                    ((MouseAccessor)Minecraft.getInstance().mouse).setRightClick(false);
                }
            }
        }
    
    
    
            @Inject(method = "isPressed", at = @At("RETURN"), cancellable = true)
    
        public void isPressedXIV(CallbackInfoReturnable<Boolean> cir) {
    
            if (((KeyMapping) (Object) this).equals(Minecraft.getInstance().options.useKey) && Mod.enabled && Config.GSON.instance().clickToMove) {
    
                if (ClientInit.interact.isPressed()) {
    
                    cir.setReturnValue(true);
                } else if (Minecraft.getInstance().player != null && Minecraft.getInstance().options.hotbarKeys[Minecraft.getInstance().player.getInventory().selectedSlot].isPressed()) {
                    if (Mod.cooldownWas > 4) {
    
                        cir.setReturnValue(true);
                    } else {
                        cir.setReturnValue(false);
    
                    }
                } else if (((KeyMapping) (Object) this).equals(Minecraft.getInstance().options.useKey) && Mod.enabled && Config.GSON.instance().clickToMove && Minecraft.getInstance().player != null && Minecraft.getInstance().player.getMainHandItem().getItem() instanceof ProjectileWeaponItem) {
                    if (Minecraft.getInstance().options.attackKey.isPressed()) {
                        cir.setReturnValue(true);
                    } else {
                        cir.setReturnValue(Mod.using);
    
    
                    }
                } else {
                    cir.setReturnValue(Mod.using);
    
                }
            }
    
        }
    
    
    
        @Inject(method = "wasPressed", at = @At("RETURN"), cancellable = true)
    
        public void wasPressedXIV(CallbackInfoReturnable<Boolean> cir) {
    
             if (((KeyMapping) (Object) this).equals(Minecraft.getInstance().options.useKey) && Mod.enabled && Config.GSON.instance().clickToMove) {
    
                if (ClientInit.interact.wasPressed() ){
                    cir.setReturnValue(true);
                    return;
    
                } else if (Minecraft.getInstance().player != null && Minecraft.getInstance().options.hotbarKeys[Minecraft.getInstance().player.getInventory().selectedSlot].wasPressed()) {
                    if (Mod.cooldownWas > 4) {
                        cir.setReturnValue(true);
                    }
                    else{
                        cir.setReturnValue(false);
    
                    }
                }
                else
                if (((KeyMapping) (Object) this).equals(Minecraft.getInstance().options.useKey) && Mod.enabled && Config.GSON.instance().clickToMove && Minecraft.getInstance().player != null && Minecraft.getInstance().player.getMainHandItem().getItem() instanceof ProjectileWeaponItem) {
                    if (Minecraft.getInstance().options.attackKey.wasPressed()) {
                        cir.setReturnValue(true);
                    } else {
                            cir.setReturnValue(false);
                    }
                }
                else{
                    cir.setReturnValue(false);
    
                }
            }
        }
    }