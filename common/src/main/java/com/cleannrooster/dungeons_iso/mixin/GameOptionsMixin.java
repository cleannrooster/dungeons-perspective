package com.cleannrooster.dungeons_iso.mixin;

import com.cleannrooster.dungeons_iso.config.Config;
import com.cleannrooster.dungeons_iso.mod.Mod;
import com.mojang.serialization.Codec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static net.minecraft.client.Options.genericValueLabel;

@Mixin(Options.class)
public class GameOptionsMixin {
    private static OptionInstance<Integer> fov30;
    private static OptionInstance<Double> fovscale;
    private static OptionInstance<Boolean> bobbing;

    private static OptionInstance<Boolean> autoJumpXIV;


    private static Component getPercentValueTextCleann(Component prefix, double value) {
        return Component.translatable("options.percent_value", new Object[]{prefix, (int)(value * 100.0)});
    }
    private static Component getPercentValueOrOffTextCleann(Component prefix, double value) {
        return value == 0.0 ? genericValueLabel(prefix, CommonComponents.OPTION_OFF) : getPercentValueTextCleann(prefix, value);
    }


    static{
        autoJumpXIV = OptionInstance.createBoolean("options.autoJump", true);

        bobbing = OptionInstance.createBoolean("options.viewBobbing", true);

        fov30 = new OptionInstance<Integer>("options.fov", OptionInstance.noTooltip(), (optionText, value) -> {
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

        }, new OptionInstance.IntRange(45, 90), Codec.DOUBLE.xmap((value) -> {
            return (int)(value * 40.0 + 70.0);
        }, (value) -> {
            return ((double)value - 70.0) / 40.0;
        }), (Integer)(int)Config.GSON.instance().fov, (value) -> {
            com.cleannrooster.dungeons_iso.util.Util.refreshLevelRenderer();
        });

        fov30.set(Math.clamp((int) Config.GSON.instance().fov,(int)45,90));
        fovscale = new OptionInstance("options.fovEffectScale", OptionInstance.cachedConstantTooltip(Component.translatable("options.fovEffectScale.tooltip")), ((optionText, value) -> getPercentValueOrOffTextCleann(optionText,(double)value)), OptionInstance.UnitDouble.INSTANCE.xmap(Mth::square, Math::sqrt), Codec.doubleRange(0.0, 1.0), 1.0, (value) -> {
        });
        fovscale.set(0D);
        bobbing.set(false);
        autoJumpXIV.set(true);
    }
    @Inject(
            method = "fov",
            at = @At(value = "RETURN"),
            cancellable = true
    )
    public void getFovCleann(CallbackInfoReturnable<OptionInstance<Integer>> option) {
        if(Mod.enabled){
            int fov = fov30.get();
            fov30.set(Math.clamp((int) (fov30.get()),  45,90));
            option.setReturnValue(fov30);
            fov30.set(fov);
        }
    }
    @Inject(
            method = "fovEffectScale",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    public void getFovEffectScaleCleann(CallbackInfoReturnable<OptionInstance<Double>> option)
    {
        if(Mod.enabled) {

            option.setReturnValue(fovscale);
        }
    }
    @Inject(
            method = "bobView",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    public void bobViewCleann(CallbackInfoReturnable<OptionInstance<Boolean>> option) {
        if(Mod.enabled) {

            option.setReturnValue(bobbing);
        }
    }
    @Inject(
            method = "autoJump",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    public void getAutoJumpCleann(CallbackInfoReturnable<OptionInstance<Boolean>> option) {
        if(Mod.enabled && Config.GSON.instance().isClickToMove() && Config.GSON.instance().forceAutoJump) {

            option.setReturnValue(autoJumpXIV);
        }
    }

}
