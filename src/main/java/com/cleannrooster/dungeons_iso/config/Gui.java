package com.cleannrooster.dungeons_iso.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.gui.YACLScreen;
import dev.isxander.yacl3.gui.controllers.LabelController;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import com.cleannrooster.dungeons_iso.ClientInit;
import com.cleannrooster.dungeons_iso.network.Capabilities;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class Gui implements ModMenuApi {
    private static Text capabilityTooltip() {
        MutableText text = Text.translatable("dungeons_iso.config.capabilities.toggle.tooltip");
        if (!ClientInit.isConnectedToServer()) {
            text.append(Text
                    .translatable("dungeons_iso.config.capabilities.toggle.tooltip.notconnected")
                    .formatted(Formatting.YELLOW));
        } else if (!ClientInit.serverSupportsCapabilities()) {
            text.append(Text
                    .translatable("dungeons_iso.config.capabilities.toggle.tooltip.notsupported")
                    .formatted(Formatting.RED));
        } else if (!ClientInit.canChangeCapabilities()) {
            text.append(Text
                    .translatable("dungeons_iso.config.capabilities.toggle.tooltip.nopermissions")
                    .formatted(Formatting.RED));
        }
        return text;
    }

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return (parent) -> {
            AtomicReference<Option<Boolean>> targetFromCameraOption = new AtomicReference<>();
            AtomicReference<Option<Boolean>> unlimitedReachOption = new AtomicReference<>();

            YACLScreen screen = (YACLScreen) YetAnotherConfigLib.create(
                    Config.GSON,
                    (defaults, config, builder) -> builder
                            .title(Text.translatable("dungeons_iso.config.title"))
                            .category(ConfigCategory
                                    .createBuilder()
                                    .name(Text.translatable("dungeons_iso.config.category.options"))
                                    .option(Option
                                            .<Boolean>createBuilder()
                                            .name(Text.translatable("dungeons_iso.config.scrollWheelZoom.name"))
                                            .description(OptionDescription.of(Text.translatable(
                                                    "dungeons_iso.config.scrollWheelZoom.description")))
                                            .binding(
                                                    defaults.scrollWheelZoom,
                                                    () -> config.scrollWheelZoom,
                                                    (value) -> config.scrollWheelZoom = value
                                            )
                                            .controller(BooleanControllerBuilder::create)
                                            .build())

                                    .build())
            ).generateScreen(parent);

            ClientInit.listenCapabilities(new Consumer<>() {
                @Override
                public void accept(Capabilities capabilities) {
                    if (MinecraftClient.getInstance().currentScreen == screen) {
                        ((ToggleableController<Boolean>) targetFromCameraOption.get().controller())
                                .inner
                                .option()
                                .setAvailable(capabilities.targetFromCamera());
                        ((ToggleableController<Boolean>) unlimitedReachOption.get().controller())
                                .inner
                                .option()
                                .setAvailable(capabilities.unlimitedReach());
                    } else {
                        ClientInit.unlistenCapabilities(this);
                    }
                }
            });

            return screen;
        };
    }
}
