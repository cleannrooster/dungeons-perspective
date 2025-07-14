package com.cleannrooster.dungeons_iso.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.api.controller.FloatFieldControllerBuilder;
import dev.isxander.yacl3.gui.YACLScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
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
                                            .name(Text.translatable("dungeons_iso.config.force.name"))
                                            .description(OptionDescription.of(Text.translatable(
                                                    "dungeons_iso.config.force.description")))
                                            .binding(
                                                    defaults.force,
                                                    () -> config.force,
                                                    (value) -> config.force = value
                                            )
                                            .controller(BooleanControllerBuilder::create)
                                            .build())
                                    .option(Option
                                            .<Boolean>createBuilder()
                                            .name(Text.translatable("dungeons_iso.config.forceAutoJump.name"))
                                            .description(OptionDescription.of(Text.translatable(
                                                    "dungeons_iso.config.forceAutoJump.description")))
                                            .binding(
                                                    defaults.forceAutoJump,
                                                    () -> config.forceAutoJump,
                                                    (value) -> config.forceAutoJump = value
                                            )
                                            .controller(BooleanControllerBuilder::create)
                                            .build())
                                    .option(Option
                                            .<Boolean>createBuilder()
                                            .name(Text.translatable("dungeons_iso.config.rollTowardsCursor.name"))
                                            .description(OptionDescription.of(Text.translatable(
                                                    "dungeons_iso.config.rollTowardsCursor.description")))
                                            .binding(
                                                    defaults.rollTowardsCursor,
                                                    () -> config.rollTowardsCursor,
                                                    (value) -> config.rollTowardsCursor = value
                                            )
                                            .controller(BooleanControllerBuilder::create)
                                            .build())
                                    .option(Option
                                            .<Boolean>createBuilder()
                                            .name(Text.translatable("dungeons_iso.config.onStartup.name"))
                                            .description(OptionDescription.of(Text.translatable(
                                                    "dungeons_iso.config.onStartup.description")))
                                            .binding(
                                                    defaults.onStartup,
                                                    () -> config.onStartup,
                                                    (value) -> config.onStartup = value
                                            )
                                            .controller(BooleanControllerBuilder::create)
                                            .build())
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
                                    .option(Option
                                            .<Boolean>createBuilder()
                                            .name(Text.translatable("dungeons_iso.config.click_to_move.name"))
                                            .description(OptionDescription.of(Text.translatable(
                                                    "dungeons_iso.config.click_to_move.description")))
                                            .binding(
                                                    defaults.clickToMove,
                                                    () -> config.clickToMove,
                                                    (value) -> config.clickToMove = value
                                            )
                                            .controller(BooleanControllerBuilder::create)
                                            .build())
                                    .option(Option
                                            .<Boolean>createBuilder()
                                            .name(Text.translatable("dungeons_iso.config.dynamic_camera.name"))
                                            .description(OptionDescription.of(Text.translatable(
                                                    "dungeons_iso.config.dynamic_camera.description")))
                                            .binding(
                                                    defaults.dynamicCamera,
                                                    () -> config.dynamicCamera,
                                                    (value) -> config.dynamicCamera = value
                                            )
                                            .controller(BooleanControllerBuilder::create)
                                            .build())

                                    .option(Option
                                            .<Float>createBuilder()
                                            .name(Text.translatable("dungeons_iso.config.movefactor.name"))
                                            .description(OptionDescription.of(Text.translatable(
                                                    "dungeons_iso.config.movefactor.description")))
                                            .binding(
                                                    defaults.moveFactor_v3,
                                                    () -> config.moveFactor_v3,
                                                    (value) -> config.moveFactor_v3 = value
                                            )
                                            .controller(FloatFieldControllerBuilder::create)
                                            .build())
                                    .option(Option
                                            .<Boolean>createBuilder()
                                            .name(Text.translatable("dungeons_iso.config.force_no_defer.name"))
                                            .description(OptionDescription.of(Text.translatable(
                                                    "dungeons_iso.config.force_no_defer.description")))
                                            .binding(
                                                    defaults.forceNoDefer,
                                                    () -> config.forceNoDefer,
                                                    (value) -> config.forceNoDefer = value
                                            )
                                            .controller(BooleanControllerBuilder::create)
                                            .build())
                                    .option(Option
                                            .<Boolean>createBuilder()
                                            .name(Text.translatable("dungeons_iso.config.cameraRelative.name"))
                                            .description(OptionDescription.of(Text.translatable(
                                                    "dungeons_iso.config.cameraRelative.description")))
                                            .binding(
                                                    defaults.cameraRelative,
                                                    () -> config.cameraRelative,
                                                    (value) -> config.cameraRelative = value
                                            )
                                            .controller(BooleanControllerBuilder::create)
                                            .build())
                                    .option(Option
                                            .<Boolean>createBuilder()
                                            .name(Text.translatable("dungeons_iso.config.turn_to_mouse.name"))
                                            .description(OptionDescription.of(Text.translatable(
                                                    "dungeons_iso.config.turn_to_mouse.description")))
                                            .binding(
                                                    defaults.turnToMouse,
                                                    () -> config.turnToMouse,
                                                    (value) -> config.turnToMouse = value
                                            )
                                            .controller(BooleanControllerBuilder::create)
                                            .build())
                                    .option(Option
                                            .<Boolean>createBuilder()
                                            .name(Text.translatable("dungeons_iso.config.clipToSpace.name"))
                                            .description(OptionDescription.of(Text.translatable(
                                                    "dungeons_iso.config.clipToSpace.description")))
                                            .binding(
                                                    defaults.clipToSpace,
                                                    () -> config.clipToSpace,
                                                    (value) -> config.clipToSpace = value
                                            )
                                            .controller(BooleanControllerBuilder::create)
                                            .build())
                                    .option(Option
                                            .<Boolean>createBuilder()
                                            .name(Text.translatable("dungeons_iso.config.additionalMeleeAssistance.name"))
                                            .description(OptionDescription.of(Text.translatable(
                                                    "dungeons_iso.config.additionalMeleeAssistance.description")))
                                            .binding(
                                                    defaults.additionalMeleeAssistance,
                                                    () -> config.additionalMeleeAssistance,
                                                    (value) -> config.additionalMeleeAssistance = value
                                            )
                                            .controller(BooleanControllerBuilder::create)
                                            .build())
                                    .option(Option
                                            .<Float>createBuilder()
                                            .name(Text.translatable("dungeons_iso.config.zoomFactor.name"))
                                            .description(OptionDescription.of(Text.translatable(
                                                    "dungeons_iso.config.zoomFactor.description")))
                                            .binding(
                                                    defaults.zoomFactor,
                                                    () -> config.zoomFactor,
                                                    (value) -> config.zoomFactor = value
                                            )
                                            .controller(FloatFieldControllerBuilder::create)
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
