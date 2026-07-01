package com.cleannrooster.dungeons_iso.ui;

import com.cleannrooster.dungeons_iso.api.MinecraftClientAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.EntityHitResult;

import java.util.ArrayList;
import java.util.List;

public class LootUI extends Screen {

    private static final int BTN_W = 140;
    private static final int BTN_H = 18;
    private static final int COL_SIZE = 8;

    public LootUI() {
        super(Component.translatable("dungeons_iso.loot.title"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        if (Minecraft.getInstance().player == null) return;

        List<ItemEntity> itemEntities = Minecraft.getInstance().player.level().getEntities(
                EntityTypeTest.forClass(ItemEntity.class),
                Minecraft.getInstance().player.getBoundingBox().inflate(16),
                entity -> Minecraft.getInstance().player.hasLineOfSight(entity)
        );

        // Cap to first 64 items and lay out in columns of COL_SIZE rows
        List<ItemEntity> items = itemEntities.size() > 64 ? itemEntities.subList(0, 64) : itemEntities;

        int cols = (items.size() + COL_SIZE - 1) / COL_SIZE;
        int panelW = Math.max(BTN_W + 20, cols * (BTN_W + 4) + 20);
        int panelH = Math.min(COL_SIZE, items.size()) * (BTN_H + 4) + 30;
        int panelX = (this.width - panelW) / 2;
        int panelY = (this.height - panelH) / 2;

        for (int i = 0; i < items.size(); i++) {
            int col = i / COL_SIZE;
            int row = i % COL_SIZE;
            ItemEntity entity = items.get(i);
            int bx = panelX + 10 + col * (BTN_W + 4);
            int by = panelY + 26 + row * (BTN_H + 4);

            this.addRenderableWidget(Button.builder(entity.getName(), btn -> {
                MinecraftClientAccessor mc = (MinecraftClientAccessor) Minecraft.getInstance();
                mc.setLocation(new EntityHitResult(entity, entity.position()));
                mc.setOriginalLocation(Minecraft.getInstance().player.position());
            }).bounds(bx, by, BTN_W, BTN_H).build());
        }
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

        // Recalculate panel dims for rendering (mirrors init)
        int items = 0;
        if (Minecraft.getInstance().player != null) {
            items = Math.min(64, (int) Minecraft.getInstance().player.level()
                    .getEntities(EntityTypeTest.forClass(ItemEntity.class),
                            Minecraft.getInstance().player.getBoundingBox().inflate(16),
                            e -> Minecraft.getInstance().player.hasLineOfSight(e))
                    .stream().count());
        }
        int cols = (items + COL_SIZE - 1) / COL_SIZE;
        int panelW = Math.max(BTN_W + 20, cols * (BTN_W + 4) + 20);
        int panelH = Math.min(COL_SIZE, items) * (BTN_H + 4) + 30;
        int panelX = (this.width - panelW) / 2;
        int panelY = (this.height - panelH) / 2;

        graphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xC0101010);
        graphics.outline(panelX, panelY, panelW, panelH, 0xFF555555);
        graphics.centeredText(this.font, Component.translatable("dungeons_iso.loot.nearby"),
                this.width / 2, panelY + 8, 0xFFFFFFFF);

        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }
}
