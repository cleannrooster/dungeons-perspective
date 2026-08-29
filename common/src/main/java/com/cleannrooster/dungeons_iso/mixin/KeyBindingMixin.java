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
        @Inject(method = "isDown", at = @At("RETURN"), cancellable = true)
    
        public void isPressedMouse(CallbackInfoReturnable<Boolean> cir) {
            if (((KeyMapping) (Object) this).same(ClientInit.clickToMove) && Mod.enabled && Config.GSON.instance().isClickToMove()) {
                if(((GuiScreenAccessor) Minecraft.getInstance().gui).getScreen() == null && Minecraft.getInstance().mouseHandler.isRightPressed()){
                    cir.setReturnValue(true);
    
                }
                else{
                    ((MouseAccessor)Minecraft.getInstance().mouseHandler).setRightClick(false);
                }
            }
        }
    
    
    
            @Inject(method = "isDown", at = @At("RETURN"), cancellable = true)
    
        public void isPressedXIV(CallbackInfoReturnable<Boolean> cir) {
    
            if (((KeyMapping) (Object) this).same(Minecraft.getInstance().options.keyUse) && Mod.enabled && Config.GSON.instance().isClickToMove()) {
    
                if (ClientInit.interact.isDown()) {
    
                    cir.setReturnValue(true);
                } else if (Minecraft.getInstance().player != null && Minecraft.getInstance().options.keyHotbarSlots[Minecraft.getInstance().player.getInventory().getSelectedSlot()].isDown()) {
                    if (Mod.cooldownWas > 4) {
    
                        cir.setReturnValue(true);
                    } else {
                        cir.setReturnValue(false);
    
                    }
                } else if (((KeyMapping) (Object) this).same(Minecraft.getInstance().options.keyUse) && Mod.enabled && Config.GSON.instance().isClickToMove() && Minecraft.getInstance().player != null && Minecraft.getInstance().player.getMainHandItem().getItem() instanceof ProjectileWeaponItem) {
                    if (Minecraft.getInstance().options.keyAttack.isDown()) {
                        cir.setReturnValue(true);
                    } else {
                        cir.setReturnValue(Mod.using);
    
    
                    }
                } else {
                    cir.setReturnValue(Mod.using);
    
                }
            }
    
        }
    
    
    
        @Inject(method = "consumeClick", at = @At("RETURN"), cancellable = true)
    
        public void wasPressedXIV(CallbackInfoReturnable<Boolean> cir) {
    
             if (((KeyMapping) (Object) this).same(Minecraft.getInstance().options.keyUse) && Mod.enabled && Config.GSON.instance().isClickToMove()) {
    
                if (ClientInit.interact.consumeClick() ){
                    cir.setReturnValue(true);
                    return;
    
                } else if (Minecraft.getInstance().player != null && Minecraft.getInstance().options.keyHotbarSlots[Minecraft.getInstance().player.getInventory().getSelectedSlot()].consumeClick()) {
                    if (Mod.cooldownWas > 4) {
                        cir.setReturnValue(true);
                    }
                    else{
                        cir.setReturnValue(false);
    
                    }
                }
                else
                if (((KeyMapping) (Object) this).same(Minecraft.getInstance().options.keyUse) && Mod.enabled && Config.GSON.instance().isClickToMove() && Minecraft.getInstance().player != null && Minecraft.getInstance().player.getMainHandItem().getItem() instanceof ProjectileWeaponItem) {
                    if (Minecraft.getInstance().options.keyAttack.consumeClick()) {
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
