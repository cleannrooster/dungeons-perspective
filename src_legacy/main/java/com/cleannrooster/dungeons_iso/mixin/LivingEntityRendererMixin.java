package com.cleannrooster.dungeons_iso.mixin;

import net.minecraft.client.render.entity.LivingEntityRenderer;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
/*    @Redirect(
            method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;"
                    + "Lnet/minecraft/client/util/math/MatrixStack;"
                    + "Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/entity/model/EntityModel;render"
                            + "(Lnet/minecraft/client/util/math/MatrixStack;"
                            + "Lnet/minecraft/client/render/VertexConsumer;III)V"
            )
    )
    public void render(
            EntityModel<? super S> instance,
            MatrixStack matrices,
            VertexConsumer vertices,
            int light,
            int overlay,
            int color,
            @Local(argsOnly = true) S livingEntityRenderState,
            @Local(argsOnly = true) VertexConsumerProvider vertexConsumerProvider
    ) {
        Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();
        ClientPlayerEntity player = MinecraftClient.getInstance().player;

        if (livingEntityRenderState instanceof PlayerEntityRenderState playerEntityRenderState
                && player != null
                && playerEntityRenderState.id == player.getId()
                && camera.isThirdPerson()
                && camera.getPos().distanceTo(player.getEyePos()) < 1.0) {
            // Same as spectator mode (ref. LivingEntityRenderer#getRenderLayer)
            vertices = vertexConsumerProvider.getBuffer(RenderLayer.getItemEntityTranslucentCull(player
                    .getSkinTextures()
                    .texture()));
            color = 0x26FFFFFF;
        }

        instance.render(matrices, vertices, light, overlay, color);
    }*/
}
