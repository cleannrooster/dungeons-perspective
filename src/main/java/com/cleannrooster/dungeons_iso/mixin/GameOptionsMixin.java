package com.cleannrooster.dungeons_iso.mixin;

import com.cleannrooster.dungeons_iso.mod.Mod;
import com.mojang.serialization.Codec;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameOptions.class)
public class GameOptionsMixin {
    private static SimpleOption<Integer> fov30;

    static{
        fov30 = new SimpleOption<Integer>("options.fov", SimpleOption.emptyTooltip(), (optionText, value) -> {
            Text var10000;
            switch (value) {
                case 70:
                    var10000 = Text.translatable("options.generic_value", new Object[]{"fov", value});
                    break;
                case 110:
                    var10000 = Text.translatable("options.generic_value", new Object[]{"fov", value});
                    break;
                default:
                    var10000 = Text.translatable("options.generic_value", new Object[]{"fov", value});
            }
            return var10000;

        }, new SimpleOption.ValidatingIntSliderCallbacks(30, 110), Codec.DOUBLE.xmap((value) -> {
            return (int)(value * 40.0 + 70.0);
        }, (value) -> {
            return ((double)value - 70.0) / 40.0;
        }), 70, (value) -> {
            MinecraftClient.getInstance().worldRenderer.scheduleTerrainUpdate();
        });
        fov30.setValue(45);
    }
    @Inject(
            method = "getFov",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    public void getFovCleann(CallbackInfoReturnable<SimpleOption<Integer>> option) {
        if(Mod.enabled){
            option.setReturnValue(fov30);
        }
    }

}
