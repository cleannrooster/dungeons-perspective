package com.cleannrooster.dungeons_iso.config;

import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.impl.controller.FloatSliderControllerBuilderImpl;
import dev.isxander.yacl3.impl.controller.IntegerSliderControllerBuilderImpl;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/**
 * Platform-agnostic config screen factory backed by YACL.
 *
 * Keeping this separate from {@link Gui} (which implements ModMenuApi) means
 * NeoForge can reference this class directly without hitting a
 * NoClassDefFoundError on the ModMenu interface.
 *
 * <p>YACL is optional, and this class is the part of the mod that genuinely cannot work without
 * it — loading it at all with YACL missing is what throws. So the availability test deliberately
 * does <em>not</em> live here: callers must check {@code Config.GSON.hasScreen()} first, which
 * keeps the only reference to this class inside a branch that never runs without YACL.
 */
public class ConfigScreen {

    /**
     * Build and return the YACL config screen, optionally with a parent screen.
     *
     * <p>Guard every call with {@code Config.GSON.hasScreen()}.
     */
    @SuppressWarnings("unchecked")
    public static Screen create(Screen parent) {
        return YetAnotherConfigLib.create(
                (dev.isxander.yacl3.config.v2.api.ConfigClassHandler<Config>) Config.GSON.handler(),
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
                                                .name(Text.translatable("dungeons_iso.config.controllerMode.name"))
                                                .description(OptionDescription.of(Text.translatable(
                                                        "dungeons_iso.config.controllerMode.description")))
                                                .binding(
                                                        defaults.controllerMode,
                                                        () -> config.controllerMode,
                                                        (value) -> config.controllerMode = value
                                                )
                                                .controller(BooleanControllerBuilder::create)
                                                .build())
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
                                                .name(Text.translatable("dungeons_iso.config.joystickMovement.name"))
                                                .description(OptionDescription.of(Text.translatable(
                                                        "dungeons_iso.config.joystickMovement.description")))
                                                .binding(
                                                        defaults.joystickMovement,
                                                        () -> config.joystickMovement,
                                                        (value) -> config.joystickMovement = value
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
                                        .option(Option
                                                .<Float>createBuilder()
                                                .name(Text.translatable("dungeons_iso.config.bias.name"))
                                                .description(OptionDescription.of(Text.translatable(
                                                        "dungeons_iso.config.bias.description")))
                                                .binding(
                                                        defaults.soundListenerBias,
                                                        () -> config.soundListenerBias,
                                                        (value) -> config.soundListenerBias = value
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
                                        .option(Option
                                                .<Boolean>createBuilder()
                                                .name(Text.translatable("dungeons_iso.config.contextualTargeting.name"))
                                                .description(OptionDescription.of(Text.translatable(
                                                        "dungeons_iso.config.contextualTargeting.description")))
                                                .binding(
                                                        defaults.contextualTargeting,
                                                        () -> config.contextualTargeting,
                                                        (value) -> config.contextualTargeting = value
                                                )
                                                .controller(BooleanControllerBuilder::create)
                                                .build())
                                        .option(Option
                                                .<Boolean>createBuilder()
                                                .name(Text.translatable("dungeons_iso.config.movementTargeting.name"))
                                                .description(OptionDescription.of(Text.translatable(
                                                        "dungeons_iso.config.movementTargeting.description")))
                                                .binding(
                                                        defaults.movementTargeting,
                                                        () -> config.movementTargeting,
                                                        (value) -> config.movementTargeting = value
                                                )
                                                .controller(BooleanControllerBuilder::create)
                                                .build())
                                        .option(Option
                                                .<Boolean>createBuilder()
                                                .name(Text.translatable("dungeons_iso.config.contextualInteract.name"))
                                                .description(OptionDescription.of(Text.translatable(
                                                        "dungeons_iso.config.contextualInteract.description")))
                                                .binding(
                                                        defaults.contextualInteract,
                                                        () -> config.contextualInteract,
                                                        (value) -> config.contextualInteract = value
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
                                                .name(Text.translatable("dungeons_iso.config.distanceFog.name"))
                                                .description(OptionDescription.of(Text.translatable(
                                                        "dungeons_iso.config.distanceFog.description")))
                                                .binding(
                                                        defaults.distanceFog,
                                                        () -> config.distanceFog,
                                                        (value) -> config.distanceFog = value
                                                )
                                                .controller(BooleanControllerBuilder::create)
                                                .build())
                                        .option(Option
                                                .<Boolean>createBuilder()
                                                .name(Text.translatable("dungeons_iso.config.renderDistanceCap.name"))
                                                .description(OptionDescription.of(Text.translatable(
                                                        "dungeons_iso.config.renderDistanceCap.description")))
                                                .binding(
                                                        defaults.renderDistanceCap,
                                                        () -> config.renderDistanceCap,
                                                        (value) -> config.renderDistanceCap = value
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

                                .group(OptionGroup.createBuilder()
                                        .name(Text.translatable("dungeons_iso.config.group.roomculling"))
                                        .option(Option
                                                .<Boolean>createBuilder()
                                                .name(Text.translatable("dungeons_iso.config.roomCulling.name"))
                                                .description(OptionDescription.of(Text.translatable(
                                                        "dungeons_iso.config.roomCulling.description")))
                                                .binding(
                                                        defaults.roomCulling,
                                                        () -> config.roomCulling,
                                                        (value) -> config.roomCulling = value
                                                )
                                                .controller(BooleanControllerBuilder::create)
                                                .build())
                                        .option(Option
                                                .<Boolean>createBuilder()
                                                .name(Text.translatable("dungeons_iso.config.disableOcclusionCulling.name"))
                                                .description(OptionDescription.of(Text.translatable(
                                                        "dungeons_iso.config.disableOcclusionCulling.description")))
                                                .binding(
                                                        defaults.disableOcclusionCulling,
                                                        () -> config.disableOcclusionCulling,
                                                        (value) -> config.disableOcclusionCulling = value
                                                )
                                                .controller(BooleanControllerBuilder::create)
                                                .build())
                                        .option(Option
                                                .<Boolean>createBuilder()
                                                .name(Text.translatable("dungeons_iso.config.cullDebugLog.name"))
                                                .description(OptionDescription.of(Text.translatable(
                                                        "dungeons_iso.config.cullDebugLog.description")))
                                                .binding(
                                                        defaults.cullDebugLog,
                                                        () -> config.cullDebugLog,
                                                        (value) -> config.cullDebugLog = value
                                                )
                                                .controller(BooleanControllerBuilder::create)
                                                .build())
                                        .option(Option
                                                .<Integer>createBuilder()
                                                .name(Text.translatable("dungeons_iso.config.roomRadius.name"))
                                                .description(OptionDescription.of(Text.translatable(
                                                        "dungeons_iso.config.roomRadius.description")))
                                                .binding(
                                                        defaults.roomRadius,
                                                        () -> config.roomRadius,
                                                        (value) -> config.roomRadius = value
                                                )
                                                .controller(o -> new IntegerSliderControllerBuilderImpl(o).range(16, 96).step(1))
                                                .build())
                                        .option(Option
                                                .<Integer>createBuilder()
                                                .name(Text.translatable("dungeons_iso.config.roomWallThickness.name"))
                                                .description(OptionDescription.of(Text.translatable(
                                                        "dungeons_iso.config.roomWallThickness.description")))
                                                .binding(
                                                        defaults.roomWallThickness,
                                                        () -> config.roomWallThickness,
                                                        (value) -> config.roomWallThickness = value
                                                )
                                                .controller(o -> new IntegerSliderControllerBuilderImpl(o).range(0, 8).step(1))
                                                .build())
                                        .option(Option
                                                .<Integer>createBuilder()
                                                .name(Text.translatable("dungeons_iso.config.roomCeilingTolerance.name"))
                                                .description(OptionDescription.of(Text.translatable(
                                                        "dungeons_iso.config.roomCeilingTolerance.description")))
                                                .binding(
                                                        defaults.roomCeilingTolerance,
                                                        () -> config.roomCeilingTolerance,
                                                        (value) -> config.roomCeilingTolerance = value
                                                )
                                                .controller(o -> new IntegerSliderControllerBuilderImpl(o).range(0, 16).step(1))
                                                .build())
                                        .option(Option
                                                .<Integer>createBuilder()
                                                .name(Text.translatable("dungeons_iso.config.roomCoverHeight.name"))
                                                .description(OptionDescription.of(Text.translatable(
                                                        "dungeons_iso.config.roomCoverHeight.description")))
                                                .binding(
                                                        defaults.roomCoverHeight,
                                                        () -> config.roomCoverHeight,
                                                        (value) -> config.roomCoverHeight = value
                                                )
                                                .controller(o -> new IntegerSliderControllerBuilderImpl(o).range(1, 128).step(1))
                                                .build())
                                        .option(Option
                                                .<Integer>createBuilder()
                                                .name(Text.translatable("dungeons_iso.config.roomMaxVolume.name"))
                                                .description(OptionDescription.of(Text.translatable(
                                                        "dungeons_iso.config.roomMaxVolume.description")))
                                                .binding(
                                                        defaults.roomMaxVolume,
                                                        () -> config.roomMaxVolume,
                                                        (value) -> config.roomMaxVolume = value
                                                )
                                                .controller(o -> new IntegerSliderControllerBuilderImpl(o).range(10000, 500000).step(10000))
                                                .build())
                                        .option(Option
                                                .<Integer>createBuilder()
                                                .name(Text.translatable("dungeons_iso.config.roomSectionsPerTick.name"))
                                                .description(OptionDescription.of(Text.translatable(
                                                        "dungeons_iso.config.roomSectionsPerTick.description")))
                                                .binding(
                                                        defaults.roomSectionsPerTick,
                                                        () -> config.roomSectionsPerTick,
                                                        (value) -> config.roomSectionsPerTick = value
                                                )
                                                .controller(o -> new IntegerSliderControllerBuilderImpl(o).range(1, 32).step(1))
                                                .build())
                                        .option(Option
                                                .<Integer>createBuilder()
                                                .name(Text.translatable("dungeons_iso.config.roomNodesPerSlice.name"))
                                                .description(OptionDescription.of(Text.translatable(
                                                        "dungeons_iso.config.roomNodesPerSlice.description")))
                                                .binding(
                                                        defaults.roomNodesPerSlice,
                                                        () -> config.roomNodesPerSlice,
                                                        (value) -> config.roomNodesPerSlice = value
                                                )
                                                .controller(o -> new IntegerSliderControllerBuilderImpl(o).range(256, 20000).step(256))
                                                .build())
                                        .option(Option
                                                .<Integer>createBuilder()
                                                .name(Text.translatable("dungeons_iso.config.roomRescanCooldown.name"))
                                                .description(OptionDescription.of(Text.translatable(
                                                        "dungeons_iso.config.roomRescanCooldown.description")))
                                                .binding(
                                                        defaults.roomRescanCooldown,
                                                        () -> config.roomRescanCooldown,
                                                        (value) -> config.roomRescanCooldown = value
                                                )
                                                .controller(o -> new IntegerSliderControllerBuilderImpl(o).range(1, 40).step(1))
                                                .build())
                                        .build())
                                .group(OptionGroup.createBuilder()
                                        .name(Text.translatable("dungeons_iso.config.group.shapeculling"))
                                        .option(Option
                                                .<Boolean>createBuilder()
                                                .name(Text.translatable("dungeons_iso.config.shapeCulling.name"))
                                                .description(OptionDescription.of(Text.translatable(
                                                        "dungeons_iso.config.shapeCulling.description")))
                                                .binding(
                                                        defaults.shapeCulling,
                                                        () -> config.shapeCulling,
                                                        (value) -> config.shapeCulling = value
                                                )
                                                .controller(BooleanControllerBuilder::create)
                                                .build())
                                        .option(Option
                                                .<Boolean>createBuilder()
                                                .name(Text.translatable("dungeons_iso.config.unifiedSilhouette.name"))
                                                .description(OptionDescription.of(Text.translatable(
                                                        "dungeons_iso.config.unifiedSilhouette.description")))
                                                .binding(
                                                        defaults.unifiedSilhouette,
                                                        () -> config.unifiedSilhouette,
                                                        (value) -> config.unifiedSilhouette = value
                                                )
                                                .controller(BooleanControllerBuilder::create)
                                                .build())
                                        .option(Option
                                                .<Boolean>createBuilder()
                                                .name(Text.translatable("dungeons_iso.config.terrainSilhouetteCulling.name"))
                                                .description(OptionDescription.of(Text.translatable(
                                                        "dungeons_iso.config.terrainSilhouetteCulling.description")))
                                                .binding(
                                                        defaults.terrainSilhouetteCulling,
                                                        () -> config.terrainSilhouetteCulling,
                                                        (value) -> config.terrainSilhouetteCulling = value
                                                )
                                                .controller(BooleanControllerBuilder::create)
                                                .build())
                                        .option(Option
                                                .<Integer>createBuilder()
                                                .name(Text.translatable("dungeons_iso.config.terrainSilhouetteDilation.name"))
                                                .description(OptionDescription.of(Text.translatable(
                                                        "dungeons_iso.config.terrainSilhouetteDilation.description")))
                                                .binding(
                                                        defaults.terrainSilhouetteDilation,
                                                        () -> config.terrainSilhouetteDilation,
                                                        (value) -> config.terrainSilhouetteDilation = value
                                                )
                                                .controller(o -> new IntegerSliderControllerBuilderImpl(o).range(0, 12).step(1))
                                                .build())
                                        .option(Option
                                                .<Boolean>createBuilder()
                                                .name(Text.translatable("dungeons_iso.config.ghostCulledBlocks.name"))
                                                .description(OptionDescription.of(Text.translatable(
                                                        "dungeons_iso.config.ghostCulledBlocks.description")))
                                                .binding(
                                                        defaults.ghostCulledBlocks,
                                                        () -> config.ghostCulledBlocks,
                                                        (value) -> config.ghostCulledBlocks = value
                                                )
                                                .controller(BooleanControllerBuilder::create)
                                                .build())
                                        .option(Option
                                                .<Float>createBuilder()
                                                .name(Text.translatable("dungeons_iso.config.ghostClearScreen.name"))
                                                .description(OptionDescription.of(Text.translatable(
                                                        "dungeons_iso.config.ghostClearScreen.description")))
                                                .binding(
                                                        defaults.ghostClearScreen,
                                                        () -> config.ghostClearScreen,
                                                        (value) -> config.ghostClearScreen = value
                                                )
                                                .controller(o -> new FloatSliderControllerBuilderImpl(o).range(0.0F, 2.0F).step(0.01F))
                                                .build())
                                        .option(Option
                                                .<Float>createBuilder()
                                                .name(Text.translatable("dungeons_iso.config.ghostOpaqueScreen.name"))
                                                .description(OptionDescription.of(Text.translatable(
                                                        "dungeons_iso.config.ghostOpaqueScreen.description")))
                                                .binding(
                                                        defaults.ghostOpaqueScreen,
                                                        () -> config.ghostOpaqueScreen,
                                                        (value) -> config.ghostOpaqueScreen = value
                                                )
                                                .controller(o -> new FloatSliderControllerBuilderImpl(o).range(0.0F, 2.0F).step(0.01F))
                                                .build())
                                        .option(Option
                                                .<Float>createBuilder()
                                                .name(Text.translatable("dungeons_iso.config.ghostMaxAlpha.name"))
                                                .description(OptionDescription.of(Text.translatable(
                                                        "dungeons_iso.config.ghostMaxAlpha.description")))
                                                .binding(
                                                        defaults.ghostMaxAlpha,
                                                        () -> config.ghostMaxAlpha,
                                                        (value) -> config.ghostMaxAlpha = value
                                                )
                                                .controller(o -> new FloatSliderControllerBuilderImpl(o).range(0.0F, 1.0F).step(0.05F))
                                                .build())
                                        .option(Option
                                                .<Float>createBuilder()
                                                .name(Text.translatable("dungeons_iso.config.terrainOccludeThreshold.name"))
                                                .description(OptionDescription.of(Text.translatable(
                                                        "dungeons_iso.config.terrainOccludeThreshold.description")))
                                                .binding(
                                                        defaults.terrainOccludeThreshold,
                                                        () -> config.terrainOccludeThreshold,
                                                        (value) -> config.terrainOccludeThreshold = value
                                                )
                                                .controller(o -> new FloatSliderControllerBuilderImpl(o).range(0.0F, 1.0F).step(0.01F))
                                                .build())
                                        .option(Option
                                                .<Float>createBuilder()
                                                .name(Text.translatable("dungeons_iso.config.sightlineSuppressThreshold.name"))
                                                .description(OptionDescription.of(Text.translatable(
                                                        "dungeons_iso.config.sightlineSuppressThreshold.description")))
                                                .binding(
                                                        defaults.sightlineSuppressThreshold,
                                                        () -> config.sightlineSuppressThreshold,
                                                        (value) -> config.sightlineSuppressThreshold = value
                                                )
                                                .controller(o -> new FloatSliderControllerBuilderImpl(o).range(0.5F, 1.0F).step(0.01F))
                                                .build())
                                        .option(Option
                                                .<Integer>createBuilder()
                                                .name(Text.translatable("dungeons_iso.config.terrainShapeCap.name"))
                                                .description(OptionDescription.of(Text.translatable(
                                                        "dungeons_iso.config.terrainShapeCap.description")))
                                                .binding(
                                                        defaults.terrainShapeCap,
                                                        () -> config.terrainShapeCap,
                                                        (value) -> config.terrainShapeCap = value
                                                )
                                                .controller(o -> new IntegerSliderControllerBuilderImpl(o).range(256, 16384).step(256))
                                                .build())
                                        .option(Option
                                                .<Integer>createBuilder()
                                                .name(Text.translatable("dungeons_iso.config.treeShapeCap.name"))
                                                .description(OptionDescription.of(Text.translatable(
                                                        "dungeons_iso.config.treeShapeCap.description")))
                                                .binding(
                                                        defaults.treeShapeCap,
                                                        () -> config.treeShapeCap,
                                                        (value) -> config.treeShapeCap = value
                                                )
                                                .controller(o -> new IntegerSliderControllerBuilderImpl(o).range(256, 16384).step(256))
                                                .build())
                                        .option(Option
                                                .<Integer>createBuilder()
                                                .name(Text.translatable("dungeons_iso.config.shapeMaxSpan.name"))
                                                .description(OptionDescription.of(Text.translatable(
                                                        "dungeons_iso.config.shapeMaxSpan.description")))
                                                .binding(
                                                        defaults.shapeMaxSpan,
                                                        () -> config.shapeMaxSpan,
                                                        (value) -> config.shapeMaxSpan = value
                                                )
                                                .controller(o -> new IntegerSliderControllerBuilderImpl(o).range(4, 96).step(1))
                                                .build())
                                        .option(Option
                                                .<Integer>createBuilder()
                                                .name(Text.translatable("dungeons_iso.config.shapeMaxHeight.name"))
                                                .description(OptionDescription.of(Text.translatable(
                                                        "dungeons_iso.config.shapeMaxHeight.description")))
                                                .binding(
                                                        defaults.shapeMaxHeight,
                                                        () -> config.shapeMaxHeight,
                                                        (value) -> config.shapeMaxHeight = value
                                                )
                                                .controller(o -> new IntegerSliderControllerBuilderImpl(o).range(4, 160).step(1))
                                                .build())
                                        .option(Option
                                                .<Integer>createBuilder()
                                                .name(Text.translatable("dungeons_iso.config.treeLeafSpread.name"))
                                                .description(OptionDescription.of(Text.translatable(
                                                        "dungeons_iso.config.treeLeafSpread.description")))
                                                .binding(
                                                        defaults.treeLeafSpread,
                                                        () -> config.treeLeafSpread,
                                                        (value) -> config.treeLeafSpread = value
                                                )
                                                .controller(o -> new IntegerSliderControllerBuilderImpl(o).range(1, 16).step(1))
                                                .build())
                                        .option(Option
                                                .<Integer>createBuilder()
                                                .name(Text.translatable("dungeons_iso.config.sightlineMaxShapes.name"))
                                                .description(OptionDescription.of(Text.translatable(
                                                        "dungeons_iso.config.sightlineMaxShapes.description")))
                                                .binding(
                                                        defaults.sightlineMaxShapes,
                                                        () -> config.sightlineMaxShapes,
                                                        (value) -> config.sightlineMaxShapes = value
                                                )
                                                .controller(o -> new IntegerSliderControllerBuilderImpl(o).range(1, 64).step(1))
                                                .build())
                                        .option(Option
                                                .<Integer>createBuilder()
                                                .name(Text.translatable("dungeons_iso.config.sightlineMaxCulledBlocks.name"))
                                                .description(OptionDescription.of(Text.translatable(
                                                        "dungeons_iso.config.sightlineMaxCulledBlocks.description")))
                                                .binding(
                                                        defaults.sightlineMaxCulledBlocks,
                                                        () -> config.sightlineMaxCulledBlocks,
                                                        (value) -> config.sightlineMaxCulledBlocks = value
                                                )
                                                .controller(o -> new IntegerSliderControllerBuilderImpl(o).range(1024, 65536).step(1024))
                                                .build())
                                        .option(Option
                                                .<Integer>createBuilder()
                                                .name(Text.translatable("dungeons_iso.config.sightlineRescanCooldown.name"))
                                                .description(OptionDescription.of(Text.translatable(
                                                        "dungeons_iso.config.sightlineRescanCooldown.description")))
                                                .binding(
                                                        defaults.sightlineRescanCooldown,
                                                        () -> config.sightlineRescanCooldown,
                                                        (value) -> config.sightlineRescanCooldown = value
                                                )
                                                .controller(o -> new IntegerSliderControllerBuilderImpl(o).range(1, 20).step(1))
                                                .build())
                                        .option(Option
                                                .<Float>createBuilder()
                                                .name(Text.translatable("dungeons_iso.config.sightlineCameraStep.name"))
                                                .description(OptionDescription.of(Text.translatable(
                                                        "dungeons_iso.config.sightlineCameraStep.description")))
                                                .binding(
                                                        defaults.sightlineCameraStep,
                                                        () -> config.sightlineCameraStep,
                                                        (value) -> config.sightlineCameraStep = value
                                                )
                                                .controller(o -> new FloatSliderControllerBuilderImpl(o).range(0.1F, 4.0F).step(0.05F))
                                                .build())
                                        .build())

                                .build())
        ).generateScreen(parent);
    }
}
