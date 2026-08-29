package com.cleannrooster.dungeons_iso.api;

import com.cleannrooster.dungeons_iso.config.Config;
import com.cleannrooster.dungeons_iso.mod.Mod;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Gold outline for the independently selected contextual block target. */
public final class ContextualTargetOutline {
    private ContextualTargetOutline() {
    }

    public static void submit(CameraRenderState camera, SubmitNodeCollector collector) {
        Minecraft client = Minecraft.getInstance();
        if (!Mod.enabled || !Config.GSON.instance().isContextualInteract()
                || Mod.targetedInteractable == null || client.level == null || camera == null) {
            return;
        }
        BlockState state = client.level.getBlockState(Mod.targetedInteractable);
        if (state.isAir()) return;
        VoxelShape shape = state.getShape(client.level, Mod.targetedInteractable);
        if (shape.isEmpty()) return;

        PoseStack pose = new PoseStack();
        pose.translate(Mod.targetedInteractable.getX() - camera.pos.x,
                Mod.targetedInteractable.getY() - camera.pos.y,
                Mod.targetedInteractable.getZ() - camera.pos.z);
        collector.submitShapeOutline(pose, shape, RenderTypes.secondaryBlockOutline(),
                0xE6FFD126, 2.0F, false);
    }
}
