package wtf.kity.minecraftxiv.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, M extends EntityModel<T>> {
    @Redirect(
            method = "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/entity/model/EntityModel;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;III)V"
            )
    )
    public void render(EntityModel<T> instance, MatrixStack matrices, VertexConsumer vertices, int light, int overlay, int color, @Local(argsOnly = true) T livingEntity, @Local(argsOnly = true) VertexConsumerProvider vertexConsumerProvider) {
        Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();
        ClientPlayerEntity player = MinecraftClient.getInstance().player;

        if (livingEntity == player && camera.isThirdPerson() && camera.getPos().distanceTo(livingEntity.getEyePos()) < 1.0) {
            // Same as spectator mode (ref. LivingEntityRenderer#getRenderLayer)
            vertices = vertexConsumerProvider.getBuffer(RenderLayer.getItemEntityTranslucentCull(player.getSkinTextures().texture()));
            color = 0x26FFFFFF;
        }

        instance.render(matrices, vertices, light, overlay, color);
    }
}
