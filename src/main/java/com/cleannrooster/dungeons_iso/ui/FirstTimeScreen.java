package com.cleannrooster.dungeons_iso.ui;

import com.cleannrooster.dungeons_iso.config.Config;
import com.cleannrooster.dungeons_iso.config.FirstTimeState;
import com.cleannrooster.dungeons_iso.mod.Mod;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;

public class FirstTimeScreen extends BaseOwoScreen<FlowLayout> {
    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, Containers::verticalFlow);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    protected void build(FlowLayout rootComponent) {
        rootComponent
                .surface(Surface.VANILLA_TRANSLUCENT)
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .verticalAlignment(VerticalAlignment.CENTER);

        var components = new ArrayList<Component>();

        components.add(
                Containers.horizontalFlow(Sizing.content(), Sizing.content())
                        .child(Components.label(Text.translatable("dungeons_iso.firsttime.title")))
                        .alignment(HorizontalAlignment.CENTER, VerticalAlignment.CENTER)
                        .padding(Insets.of(5))
        );

        components.add(
                Containers.horizontalFlow(Sizing.content(), Sizing.content())
                        .child(Components.label(Text.translatable("dungeons_iso.firsttime.description")))
                        .alignment(HorizontalAlignment.CENTER, VerticalAlignment.CENTER)
                        .padding(Insets.of(5))
        );

        var buttonList = new ArrayList<Component>();

        buttonList.add(
                Containers.horizontalFlow(Sizing.content(), Sizing.content())
                        .child(Components.button(Text.translatable("dungeons_iso.firsttime.enable"), button -> {
                            Config.GSON.instance().onStartup = true;
                            Config.GSON.save();
                            FirstTimeState.get().choiceMade = true;
                            FirstTimeState.save();
                            enablePerspective();
                            close();
                        }))
                        .padding(Insets.of(3))
        );

        buttonList.add(
                Containers.horizontalFlow(Sizing.content(), Sizing.content())
                        .child(Components.button(Text.translatable("dungeons_iso.firsttime.disable"), button -> {
                            Config.GSON.instance().onStartup = false;
                            Config.GSON.save();
                            FirstTimeState.get().choiceMade = true;
                            FirstTimeState.save();
                            close();
                        }))
                        .padding(Insets.of(3))
        );

        var buttonRow = Containers.horizontalFlow(Sizing.content(), Sizing.content())
                .alignment(HorizontalAlignment.CENTER, VerticalAlignment.CENTER);
        ((FlowLayout) buttonRow).children(buttonList);
        components.add(buttonRow);

        var panel = Containers.verticalFlow(Sizing.content(), Sizing.content())
                .alignment(HorizontalAlignment.CENTER, VerticalAlignment.CENTER)
                .surface(Surface.DARK_PANEL)
                .padding(Insets.of(15));
        ((FlowLayout) panel).children(components);
        rootComponent.child(panel);
    }

    private void enablePerspective() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!Mod.enabled && client.world != null && client.player != null) {
            client.options.setPerspective(Perspective.FIRST_PERSON);
            Mod.enabled = true;
            Mod.lastPerspective = client.options.getPerspective();
            client.options.setPerspective(Perspective.THIRD_PERSON_BACK);
            Mod.yaw = client.player.getYaw();
            Mod.pitch = client.player.getPitch();
            client.mouse.lockCursor();
            InputUtil.setCursorParameters(client.getWindow().getHandle(), GLFW.GLFW_CURSOR_NORMAL, client.mouse.getX(), client.mouse.getY());
            client.worldRenderer.reload();
        }
    }
}
