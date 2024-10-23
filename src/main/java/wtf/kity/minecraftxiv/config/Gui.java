package wtf.kity.minecraftxiv.config;

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
import wtf.kity.minecraftxiv.ClientInit;
import wtf.kity.minecraftxiv.network.Capabilities;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class Gui implements ModMenuApi {
    private static Text capabilityTooltip() {
        MutableText text = Text.translatable("minecraftxiv.config.capabilities.toggle.tooltip");
        if (!ClientInit.isConnectedToServer()) {
            text.append(Text
                    .translatable("minecraftxiv.config.capabilities.toggle.tooltip.notconnected")
                    .formatted(Formatting.YELLOW));
        } else if (!ClientInit.serverSupportsCapabilities()) {
            text.append(Text
                    .translatable("minecraftxiv.config.capabilities.toggle.tooltip.notsupported")
                    .formatted(Formatting.RED));
        } else if (!ClientInit.canChangeCapabilities()) {
            text.append(Text
                    .translatable("minecraftxiv.config.capabilities.toggle.tooltip.nopermissions")
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
                            .title(Text.translatable("minecraftxiv.config.title"))
                            .category(ConfigCategory
                                    .createBuilder()
                                    .name(Text.translatable("minecraftxiv.config.category.options"))
                                    .option(Option
                                            .<Boolean>createBuilder()
                                            .name(Text.translatable("minecraftxiv.config.scrollWheelZoom.name"))
                                            .description(OptionDescription.of(Text.translatable(
                                                    "minecraftxiv.config.scrollWheelZoom.description")))
                                            .binding(
                                                    defaults.scrollWheelZoom,
                                                    () -> config.scrollWheelZoom,
                                                    (value) -> config.scrollWheelZoom = value
                                            )
                                            .controller(BooleanControllerBuilder::create)
                                            .build())
                                    .option(Option
                                            .<Boolean>createBuilder()
                                            .name(Text.translatable("minecraftxiv.config.movementCameraRelative.name"))
                                            .description(OptionDescription.of(Text.translatable(
                                                    "minecraftxiv.config.movementCameraRelative.description")))
                                            .binding(
                                                    defaults.movementCameraRelative,
                                                    () -> config.movementCameraRelative,
                                                    (value) -> config.movementCameraRelative = value
                                            )
                                            .controller(BooleanControllerBuilder::create)
                                            .build())
                                    .build())
                            .category(ConfigCategory
                                    .createBuilder()
                                    .name(Text.translatable("minecraftxiv.config.category.capabilities"))
                                    .option(Option
                                            .<Text>createBuilder()
                                            .name(Text.empty())
                                            .binding(Binding.immutable(Text.translatable(
                                                    "minecraftxiv.config.capabilities")))
                                            .customController(LabelController::new)
                                            .build())
                                    .option(Util.make(() -> {
                                        targetFromCameraOption.set(Option
                                                .<Boolean>createBuilder()
                                                .name(Text.translatable("minecraftxiv.config.targetFromCamera.name"))
                                                .description(OptionDescription.of(Text.translatable(
                                                        "minecraftxiv.config.targetFromCamera.description")))
                                                .binding(
                                                        defaults.targetFromCamera,
                                                        () -> config.targetFromCamera,
                                                        (value) -> config.targetFromCamera = value
                                                )
                                                .customController(opt -> new ToggleableController<>(
                                                        opt,
                                                        BooleanControllerBuilder::create,
                                                        Binding.generic(
                                                                ClientInit.getCapabilities().targetFromCamera(),
                                                                () -> ClientInit.getCapabilities().targetFromCamera(),
                                                                (val) -> ClientInit.submitCapabilities(ClientInit
                                                                        .getCapabilities()
                                                                        .withTargetFromCamera(val))
                                                        ),
                                                        Gui::capabilityTooltip
                                                ))
                                                .available(ClientInit.canChangeCapabilities())
                                                .build());
                                        return targetFromCameraOption.get();
                                    }))
                                    .option(Util.make(() -> {
                                        unlimitedReachOption.set(Option
                                                .<Boolean>createBuilder()
                                                .name(Text.translatable("minecraftxiv.config.unlimitedReach.name"))
                                                .description(OptionDescription.of(Text.translatable(
                                                        "minecraftxiv.config.unlimitedReach.description")))
                                                .binding(
                                                        defaults.unlimitedReach,
                                                        () -> config.unlimitedReach,
                                                        (value) -> config.unlimitedReach = value
                                                )
                                                .customController(opt -> new ToggleableController<>(
                                                        opt,
                                                        BooleanControllerBuilder::create,
                                                        Binding.generic(
                                                                ClientInit.getCapabilities().unlimitedReach(),
                                                                () -> ClientInit.getCapabilities().unlimitedReach(),
                                                                (val) -> ClientInit.submitCapabilities(ClientInit
                                                                        .getCapabilities()
                                                                        .withUnlimitedReach(val))
                                                        ),
                                                        Gui::capabilityTooltip
                                                ))
                                                .available(ClientInit.canChangeCapabilities())
                                                .build());
                                        return unlimitedReachOption.get();
                                    }))
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
