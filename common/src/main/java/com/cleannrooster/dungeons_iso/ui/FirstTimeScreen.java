package com.cleannrooster.dungeons_iso.ui;

import com.cleannrooster.dungeons_iso.config.Config;
import com.cleannrooster.dungeons_iso.config.FirstTimeState;
import com.cleannrooster.dungeons_iso.mod.Mod;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class FirstTimeScreen extends Screen {

    private List<FormattedCharSequence> descLines;

    public FirstTimeScreen() {
        super(Component.translatable("dungeons_iso.firsttime.title"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        descLines = this.font.split(
                Component.translatable("dungeons_iso.firsttime.description"), 260);

        int panelW = 290;
        int panelH = 120;
        int panelX = (this.width - panelW) / 2;
        int panelY = (this.height - panelH) / 2;

        int btnW = 120;
        int btnH = 20;
        int btnY = panelY + panelH - btnH - 15;

        this.addRenderableWidget(Button.builder(
                Component.translatable("dungeons_iso.firsttime.enable"),
                btn -> {
                    Config.GSON.instance().onStartup = true;
                    Config.GSON.save();
                    FirstTimeState.get().choiceMade = true;
                    FirstTimeState.save();
                    enablePerspective();
                    onClose();
                })
                .bounds(panelX + 15, btnY, btnW, btnH)
                .build());

        this.addRenderableWidget(Button.builder(
                Component.translatable("dungeons_iso.firsttime.disable"),
                btn -> {
                    Config.GSON.instance().onStartup = false;
                    Config.GSON.save();
                    FirstTimeState.get().choiceMade = true;
                    FirstTimeState.save();
                    onClose();
                })
                .bounds(panelX + panelW - btnW - 15, btnY, btnW, btnH)
                .build());
    }
    @Override
    public void extractBackground(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
        if (this.isInGameUi()) {
            this.extractTransparentBackground(graphics);
        } else {
            if (this.minecraft.level == null) {
                this.extractPanorama(graphics, a);
            }

            //this.extractBlurredBackground(graphics);
            this.extractMenuBackground(graphics);
        }

        this.minecraft.gui.hud.extractDeferredSubtitles();
    }
    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        this.extractBackground(graphics, mouseX, mouseY, delta);

        int panelW = 290;
        int panelH = 120;
        int panelX = (this.width - panelW) / 2;
        int panelY = (this.height - panelH) / 2;

        graphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xC0101010);
        graphics.outline(panelX, panelY, panelW, panelH, 0xFF555555);

        graphics.centeredText(this.font, this.title, this.width / 2, panelY + 12, 0xFFFFFFFF);
        if (descLines != null) {
            int ty = panelY + 30;
            for (FormattedCharSequence line : descLines) {
                graphics.centeredText(this.font, line, this.width / 2, ty, 0xFFAAAAAA);
                ty += 10;
            }
        }

        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    private void enablePerspective() {
        Minecraft client = Minecraft.getInstance();
        if (!Mod.enabled && client.level != null && client.player != null) {
            client.options.setCameraType(CameraType.FIRST_PERSON);
            Mod.enabled = true;
            Mod.lastPerspective = client.options.getCameraType();
            client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            Mod.yaw = client.player.getYRot();
            Mod.pitch = client.player.getXRot();
            client.mouseHandler.grabMouse();
            InputConstants.grabOrReleaseMouse(client.getWindow(), GLFW.GLFW_CURSOR_NORMAL,
                    client.mouseHandler.xpos(), client.mouseHandler.ypos());
            com.cleannrooster.dungeons_iso.util.Util.refreshLevelRenderer();
        }
    }
}
