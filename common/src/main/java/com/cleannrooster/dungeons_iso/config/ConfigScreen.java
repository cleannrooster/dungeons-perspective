package com.cleannrooster.dungeons_iso.config;

import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.impl.controller.FloatSliderControllerBuilderImpl;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/**
 * Platform-agnostic config screen factory backed by YACL.
 *
 * Keeping this separate from {@link Gui} (which implements ModMenuApi) means
 * NeoForge can reference this class directly without hitting a
 * NoClassDefFoundError on the ModMenu interface.
 */
public class ConfigScreen {

    /** Build and return the YACL config screen, optionally with a parent screen. */
    public static Screen create(Screen parent) {
        return YetAnotherConfigLib.create(
                Config.GSON,
                (defaults, config, builder) -> builder
                        .title(Text.translatable("dungeons_iso.config.title"))
                        .category(ConfigCategory
                                .createBuilder()
                                .name(Text.translatable("dungeons_iso.config.category.options"))

                                // General
                                .group(OptionGroup.createBuilder()
                                        .name(Text.translatable("dungeons_iso.config.group.general"))
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
                                                .name(Text.translatable("dungeons_iso.config.showFirstTimeGui.name"))
                                                .description(OptionDescription.of(Text.translatable(
                                                        "dungeons_iso.config.showFirstTimeGui.description")))
                                                .binding(
                                                        defaults.showFirstTimeGui,
                                                        () -> config.showFirstTimeGui,
                                                        (value) -> {
                                                            config.showFirstTimeGui = value;
                                                            if (value) {
                                                                FirstTimeState.reset();
                                                            }
                                                        }
                                                )
                                                .controller(BooleanControllerBuilder::create)
                                                .build())
                                        .build())

                                // Camera
                                .group(OptionGroup.createBuilder()
                                        .name(Text.translatable("dungeons_iso.config.group.camera"))
                                        .option(Option
                                                .<Boolean>createBuilder()
                                                .name(Text.translatable("dungeons_iso.config.xiv.name"))
                                                .description(OptionDescription.of(Text.translatable(
                                                        "dungeons_iso.config.xiv.description")))
                                                .binding(
                                                        defaults.XIV,
                                                        () -> config.XIV,
                                                        (value) -> config.XIV = value
                                                )
                                                .controller(BooleanControllerBuilder::create)
                                                .build())
                                        .option(Option
                                                .<Boolean>createBuilder()
                                                .name(Text.translatable("dungeons_iso.config.ortho.name"))
                                                .description(OptionDescription.of(Text.translatable(
                                                        "dungeons_iso.config.ortho.description")))
                                                .binding(
                                                        defaults.ortho,
                                                        () -> config.ortho,
                                                        (value) -> config.ortho = value
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
                                                .controller(opt -> new FloatSliderControllerBuilderImpl(opt).range(0F, 4F).step(0.001F))
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
                                                .<Float>createBuilder()
                                                .name(Text.translatable("dungeons_iso.config.zoomFactor.name"))
                                                .description(OptionDescription.of(Text.translatable(
                                                        "dungeons_iso.config.zoomFactor.description")))
                                                .binding(
                                                        defaults.zoomFactor,
                                                        () -> config.zoomFactor,
                                                        (value) -> config.zoomFactor = value
                                                )
                                                .controller(opt -> new FloatSliderControllerBuilderImpl(opt).range(1F, 1.5F).step(0.001F))
                                                .build())
                                        .option(Option
                                                .<Float>createBuilder()
                                                .name(Text.translatable("dungeons_iso.config.fov.name"))
                                                .description(OptionDescription.of(Text.translatable(
                                                        "dungeons_iso.config.fov.description")))
                                                .binding(
                                                        defaults.fov,
                                                        () -> config.fov,
                                                        (value) -> config.fov = value
                                                )
                                                .controller(opt -> new FloatSliderControllerBuilderImpl(opt).range(45F, 90F).step(0.001F))
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
                                                .<Float>createBuilder()
                                                .name(Text.translatable("dungeons_iso.config.zNearFactor.name"))
                                                .description(OptionDescription.of(Text.translatable(
                                                        "dungeons_iso.config.zNearFactor.description")))
                                                .binding(
                                                        defaults.zNearFactor,
                                                        () -> config.zNearFactor,
                                                        (value) -> config.zNearFactor = value
                                                )
                                                .controller(opt -> new FloatSliderControllerBuilderImpl(opt).range(0F, 1F).step(0.001F))
                                                .build())
                                        .build())

                                // Controls
                                .group(OptionGroup.createBuilder()
                                        .name(Text.translatable("dungeons_iso.config.group.controls"))
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
                                        .build())

                                // Rendering
                                .group(OptionGroup.createBuilder()
                                        .name(Text.translatable("dungeons_iso.config.group.rendering"))
                                        .option(Option
                                                .<Boolean>createBuilder()
                                                .name(Text.translatable("dungeons_iso.config.fogOfWar.name"))
                                                .description(OptionDescription.of(Text.translatable(
                                                        "dungeons_iso.config.fogOfWar.description")))
                                                .binding(
                                                        defaults.fogOfWar,
                                                        () -> config.fogOfWar,
                                                        (value) -> config.fogOfWar = value
                                                )
                                                .controller(BooleanControllerBuilder::create)
                                                .build())
                                        .option(Option
                                                .<Boolean>createBuilder()
                                                .name(Text.translatable("dungeons_iso.config.frustumCulling.name"))
                                                .description(OptionDescription.of(Text.translatable(
                                                        "dungeons_iso.config.frustumCulling.description")))
                                                .binding(
                                                        defaults.frustumCulling,
                                                        () -> config.frustumCulling,
                                                        (value) -> config.frustumCulling = value
                                                )
                                                .controller(BooleanControllerBuilder::create)
                                                .build())
                                        .option(Option
                                                .<Boolean>createBuilder()
                                                .name(Text.translatable("dungeons_iso.config.backcull.name"))
                                                .description(OptionDescription.of(Text.translatable(
                                                        "dungeons_iso.config.backcull.description")))
                                                .binding(
                                                        defaults.backCull,
                                                        () -> config.backCull,
                                                        (value) -> config.backCull = value
                                                )
                                                .controller(BooleanControllerBuilder::create)
                                                .build())
                                        .option(Option
                                                .<Float>createBuilder()
                                                .name(Text.translatable("dungeons_iso.config.cullangle.name"))
                                                .description(OptionDescription.of(Text.translatable(
                                                        "dungeons_iso.config.cullangle.description")))
                                                .binding(
                                                        defaults.cullAngle,
                                                        () -> config.cullAngle,
                                                        (value) -> config.cullAngle = value
                                                )
                                                .controller(opt -> new FloatSliderControllerBuilderImpl(opt).range(1.5F, 12F).step(0.001F))
                                                .build())
                                        .option(Option
                                                .<Float>createBuilder()
                                                .name(Text.translatable("dungeons_iso.config.coneHalfAngle.name"))
                                                .description(OptionDescription.of(Text.translatable(
                                                        "dungeons_iso.config.coneHalfAngle.description")))
                                                .binding(
                                                        defaults.coneHalfAngle,
                                                        () -> config.coneHalfAngle,
                                                        (value) -> config.coneHalfAngle = value
                                                )
                                                .controller(opt -> new FloatSliderControllerBuilderImpl(opt).range(15F, 75F).step(1F))
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
                                        .build())

                                .build())
        ).generateScreen(parent);
    }
}
