package com.cleannrooster.dungeons_iso.mixin;

import com.cleannrooster.dungeons_iso.ClientInit;
import com.cleannrooster.dungeons_iso.api.MouseAccessor;
import com.cleannrooster.dungeons_iso.config.Config;
import com.cleannrooster.dungeons_iso.mod.Mod;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.WallMountedBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.item.RangedWeaponItem;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(KeyBinding.class)
public class KeyBindingMixin {
    @Inject(method = "isPressed", at = @At("RETURN"), cancellable = true)

    public void isPressedMouse(CallbackInfoReturnable<Boolean> cir) {
        if (((KeyBinding) (Object) this).equals(ClientInit.clickToMove) && Mod.enabled && Config.GSON.instance().clickToMove) {
            if(MinecraftClient.getInstance().currentScreen == null && MinecraftClient.getInstance().mouse.wasRightButtonClicked()){
                cir.setReturnValue(true);

            }
            else{
                ((MouseAccessor)MinecraftClient.getInstance().mouse).setRightClick(false);
            }
        }
    }



        @Inject(method = "isPressed", at = @At("RETURN"), cancellable = true)

    public void isPressedXIV(CallbackInfoReturnable<Boolean> cir) {

        if (((KeyBinding) (Object) this).equals(MinecraftClient.getInstance().options.useKey) && Mod.enabled && Config.GSON.instance().clickToMove) {

            if (ClientInit.interact.isPressed()) {

                cir.setReturnValue(true);
            } else if (MinecraftClient.getInstance().player != null && MinecraftClient.getInstance().options.hotbarKeys[MinecraftClient.getInstance().player.getInventory().selectedSlot].isPressed()) {
                if (Mod.cooldownWas > 4) {

                    cir.setReturnValue(true);
                } else {
                    cir.setReturnValue(false);

                }
            } else if (((KeyBinding) (Object) this).equals(MinecraftClient.getInstance().options.useKey) && Mod.enabled && Config.GSON.instance().clickToMove && MinecraftClient.getInstance().player != null && MinecraftClient.getInstance().player.getMainHandStack().getItem() instanceof RangedWeaponItem) {
                if (MinecraftClient.getInstance().options.attackKey.isPressed()) {
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

         if (((KeyBinding) (Object) this).equals(MinecraftClient.getInstance().options.useKey) && Mod.enabled && Config.GSON.instance().clickToMove) {

            if (ClientInit.interact.wasPressed() ){
                cir.setReturnValue(true);
                return;

            } else if (MinecraftClient.getInstance().player != null && MinecraftClient.getInstance().options.hotbarKeys[MinecraftClient.getInstance().player.getInventory().selectedSlot].wasPressed()) {
                if (Mod.cooldownWas > 4) {
                    cir.setReturnValue(true);
                }
                else{
                    cir.setReturnValue(false);

                }
            }
            else
            if (((KeyBinding) (Object) this).equals(MinecraftClient.getInstance().options.useKey) && Mod.enabled && Config.GSON.instance().clickToMove && MinecraftClient.getInstance().player != null && MinecraftClient.getInstance().player.getMainHandStack().getItem() instanceof RangedWeaponItem) {
                if (MinecraftClient.getInstance().options.attackKey.wasPressed()) {
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
