package com.cleannrooster.dungeons_iso.mixin;

import com.cleannrooster.dungeons_iso.config.Config;
import com.cleannrooster.dungeons_iso.mod.Mod;
import com.mojang.serialization.Codec;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static net.minecraft.client.option.GameOptions.getGenericValueText;

@Mixin(GameOptions.class)
public class GameOptionsMixin {
    private static SimpleOption<Integer> fov30;
    private static SimpleOption<Double> fovscale;
    private static SimpleOption<Boolean> bobbing;

    private static SimpleOption<Boolean> autoJumpXIV;


    private static Text getPercentValueTextCleann(Text prefix, double value) {
        return Text.translatable("options.percent_value", new Object[]{prefix, (int)(value * 100.0)});
    }
    private static Text getPercentValueOrOffTextCleann(Text prefix, double value) {
        return value == 0.0 ? getGenericValueText(prefix, ScreenTexts.OFF) : getPercentValueTextCleann(prefix, value);
    }


    static{
        autoJumpXIV = SimpleOption.ofBoolean("options.autoJump", true);

        bobbing = SimpleOption.ofBoolean("options.viewBobbing", true);

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

        }, new SimpleOption.ValidatingIntSliderCallbacks(0, 120), Codec.DOUBLE.xmap((value) -> {
            return (int)(value * 40.0 + 70.0);
        }, (value) -> {
            return ((double)value - 70.0) / 40.0;
        }), 70, (value) -> {
            MinecraftClient.getInstance().worldRenderer.scheduleTerrainUpdate();
        });

        fov30.setValue(Math.clamp((int) 70,0,120));
        fovscale = new SimpleOption("options.fovEffectScale", SimpleOption.constantTooltip(Text.translatable("options.fovEffectScale.tooltip")), ((optionText, value) -> getPercentValueOrOffTextCleann(optionText,(double)value)), SimpleOption.DoubleSliderCallbacks.INSTANCE.withModifier(MathHelper::square, Math::sqrt), Codec.doubleRange(0.0, 1.0), 1.0, (value) -> {
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
            fov30.setValue(Config.GSON.instance().ortho ? 50 :Math.clamp((int) (fov30.getValue()  + 90F*(1F-Math.log10(4F*Mod.getZoom()+1)))+4,Config.GSON.instance().ortho ? 45 :  45,120));
            option.setReturnValue(fov30);
            fov30.setValue(Config.GSON.instance().ortho ? fov30.getValue() :fov);
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
