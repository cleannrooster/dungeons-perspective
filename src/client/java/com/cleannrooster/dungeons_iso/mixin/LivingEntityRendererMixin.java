package com.cleannrooster.dungeons_iso.mixin;

import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
/*    @Redirect(
            method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;"
                    + "Lnet/minecraft/client/util/math/PoseStack;"
                    + "Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/entity/model/EntityModel;render"
                            + "(Lnet/minecraft/client/util/math/PoseStack;"
                            + "Lnet/minecraft/client/render/VertexConsumer;III)V"
            )
    )
    public void render(
            EntityModel<? super S> instance,
            PoseStack matrices,
            VertexConsumer vertices,
            int light,
            int overlay,
            int color,
            @Local(argsOnly = true) S livingEntityRenderState,
            @Local(argsOnly = true) VertexConsumerProvider vertexConsumerProvider
    ) {
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        LocalPlayer player = Minecraft.getInstance().player;

        if (livingEntityRenderState instanceof PlayerRenderState playerEntityRenderState
                && player != null
                && playerEntityRenderState.id == player.getId()
                && camera.isThirdPerson()
                && camera.getMainCameraPos().distanceTo(player.getEyePos()) < 1.0) {
            // Same as spectator mode (ref. LivingEntityRenderer#getRenderLayer)
            vertices = vertexConsumerProvider.getBuffer(RenderLayer.getItemEntityTranslucentCull(player
                    .getSkinTextures()
                    .texture()));
            color = 0x26FFFFFF;
        }

        instance.render(matrices, vertices, light, overlay, color);
    }*/
}
