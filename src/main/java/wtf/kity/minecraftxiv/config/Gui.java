package wtf.kity.minecraftxiv.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import net.minecraft.text.Text;
import net.minecraft.util.Pair;
import wtf.kity.minecraftxiv.ClientInit;
import wtf.kity.minecraftxiv.network.Capabilities;

public class Gui implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return (parent) -> YetAnotherConfigLib.create(Config.GSON, (defaults, config, builder) -> builder
                        .title(Text.translatable("minecraftxiv.config.title"))
                        .category(ConfigCategory.createBuilder()
                                .name(Text.translatable("minecraftxiv.config.category.options"))
                                .option(
                                        Option.<Boolean>createBuilder()
                                                .name(Text.translatable("minecraftxiv.config.scrollWheelZoom.name"))
                                                .description(OptionDescription.of(Text.translatable("minecraftxiv.config.scrollWheelZoom.description")))
                                                .binding(
                                                        defaults.scrollWheelZoom,
                                                        () -> config.scrollWheelZoom,
                                                        (value) -> config.scrollWheelZoom = value
                                                )
                                                .controller(BooleanControllerBuilder::create)
                                                .build())
                                .build())
                        .category(ConfigCategory.createBuilder()
                                .name(Text.translatable("minecraftxiv.config.category.capabilities"))
                                .option(LabelOption.create(Text.translatable("minecraftxiv.config.capabilities")))
                                .option(new ToggleableOption.ToggleableOptionBuilder<Boolean>()
                                        .innerControl(BooleanControllerBuilder::create)
                                        .name(Text.translatable("minecraftxiv.config.targetFromCamera.name"))
                                        .description(OptionDescription.of(Text.translatable("minecraftxiv.config.targetFromCamera.description")))
                                        .binding(
                                                new Pair<>(ClientInit.getCapabilities().targetFromCamera(), defaults.targetFromCamera),
                                                () -> new Pair<>(ClientInit.getCapabilities().targetFromCamera(), config.targetFromCamera),
                                                (value) -> {
                                                    ClientInit.setCapabilities(new Capabilities(value.getLeft()));
                                                    config.targetFromCamera = value.getRight();
                                                }
                                        )
                                        .customController(opt -> new ToggleableController<>((ToggleableOption<Boolean>) opt))
                                        .available(ClientInit.canChangeCapabilities())
                                        .build())
                                .build()))
                .generateScreen(parent);
    }
}
