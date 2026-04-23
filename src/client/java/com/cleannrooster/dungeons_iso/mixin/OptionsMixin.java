package com.cleannrooster.dungeons_iso.mixin;

import com.cleannrooster.dungeons_iso.config.Config;
import com.cleannrooster.dungeons_iso.mod.Mod;
import com.mojang.serialization.Codec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.SimpleOption;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static net.minecraft.client.Options.genericValueLabel;

@Mixin(Options.class)
public class OptionsMixin {
    private static SimpleOption<Integer> fov30;
    private static SimpleOption<Double> fovscale;
    private static SimpleOption<Boolean> bobbing;

    private static SimpleOption<Boolean> autoJumpXIV;


    private static Component getPercentValueTextCleann(Component prefix, double value) {
        return Component.translatable("options.percent_value", new Object[]{prefix, (int)(value * 100.0)});
    }
    private static Component getPercentValueOrOffTextCleann(Component prefix, double value) {
        return value == 0.0 ? genericValueLabel(prefix, CommonComponents.OPTION_OFF) : getPercentValueTextCleann(prefix, value);
    }


    static{
        autoJumpXIV = SimpleOption.ofBoolean("options.autoJump", true);

        bobbing = SimpleOption.ofBoolean("options.viewBobbing", true);

        fov30 = new SimpleOption<Integer>("options.fov", SimpleOption.emptyTooltip(), (optionText, value) -> {
            Component var10000;
            switch (value) {
                case 70:
                    var10000 = Component.translatable("options.generic_value", new Object[]{"fov", value});
                    break;
                case 110:
                    var10000 = Component.translatable("options.generic_value", new Object[]{"fov", value});
                    break;
                default:
                    var10000 = Component.translatable("options.generic_value", new Object[]{"fov", value});
            }
            return var10000;

        }, new SimpleOption.ValidatingIntSliderCallbacks(45, 90), Codec.DOUBLE.xmap((value) -> {
            return (int)(value * 40.0 + 70.0);
        }, (value) -> {
            return ((double)value - 70.0) / 40.0;
        }), (Integer)(int)Config.GSON.instance().fov, (value) -> {
            Minecraft.getInstance().worldRenderer.scheduleTerrainUpdate();
        });

        fov30.setValue(Math.clamp((int) Config.GSON.instance().fov,(int)45,90));
        fovscale = new SimpleOption("options.fovEffectScale", SimpleOption.constantTooltip(Component.translatable("options.fovEffectScale.tooltip")), ((optionText, value) -> getPercentValueOrOffTextCleann(optionText,(double)value)), SimpleOption.DoubleSliderCallbacks.INSTANCE.withModifier(Mth::square, Math::sqrt), Codec.doubleRange(0.0, 1.0), 1.0, (value) -> {
        });
        fovscale.setValue(0D);
        bobbing.setValue(false);
        autoJumpXIV.setValue(true);
    }
    @Inject(
            method = "getFov",
            at = @At(value = "RETURN"),
            cancellable = true
    )
    public void getFovCleann(CallbackInfoReturnable<SimpleOption<Integer>> option) {
        if(Mod.enabled){
            int fov = fov30.getValue();
            fov30.setValue(Math.clamp((int) (fov30.getValue()),  45,90));
            option.setReturnValue(fov30);
            fov30.setValue(fov);
        }
    }
    @Inject(
            method = "getFovEffectScale",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    public void getFovEffectScaleCleann(CallbackInfoReturnable<SimpleOption<Double>> option)
    {
        if(Mod.enabled) {

            option.setReturnValue(fovscale);
        }
    }
    @Inject(
            method = "getBobView",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    public void bobViewCleann(CallbackInfoReturnable<SimpleOption<Boolean>> option) {
        if(Mod.enabled) {

            option.setReturnValue(bobbing);
        }
    }
    @Inject(
            method = "getAutoJump",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    public void getAutoJumpCleann(CallbackInfoReturnable<SimpleOption<Boolean>> option) {
        if(Mod.enabled && Config.GSON.instance().clickToMove && Config.GSON.instance().forceAutoJump) {

            option.setReturnValue(autoJumpXIV);
        }
    }

}
