package com.cleannrooster.dungeons_iso.mixin;

import com.cleannrooster.dungeons_iso.api.BuiltChunkAccessor;
import com.cleannrooster.dungeons_iso.api.ChunkDataAccessor;
import com.cleannrooster.dungeons_iso.api.MinecraftClientAccessor;
import com.cleannrooster.dungeons_iso.api.WorldRendererAccessor;
import com.cleannrooster.dungeons_iso.mod.Mod;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.chunk.Chunk;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.concurrent.BlockingQueue;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin implements WorldRendererAccessor {


        @Override
    public BlockingQueue<ChunkBuilder.BuiltChunk> chunks() {
        return builtChunks;
    }


    @Shadow
    private BlockingQueue<ChunkBuilder.BuiltChunk> builtChunks;


 /*   @Inject(method = "getEntitiesToRender", at = @At("TAIL"))
    void getEntitiesToRender(Camera camera, Frustum frustum, List<Entity> output, CallbackInfoReturnable<Boolean> cir) {
        PlayerEntity player = MinecraftClient.getInstance().player;

        if (player != null && Mod.enabled && Config.GSON.instance().lockOnTargeting
                && ClientInit.cycleTargetBinding.wasPressed()) {
            // Wrap around if we're already targeting, but we don't hit anything
            int wrapAround = Mod.lockOnTarget != null ? 1 : 0;
            do {
                Mod.lockOnTarget = output
                        .stream()
                        .filter(
                                entity -> {
                                    if (entity == player) return false;
                                    if (!entity.isAttackable()) return false;
                                    if (entity.isInvisibleTo(player)) return false;
                                    if (Mod.lockOnTarget != null &&
                                            player.distanceTo(entity) <= player.distanceTo(Mod.lockOnTarget)) {
                                        return false;
                                    }

                                    // No blocks in the way
                                    return player.getWorld().raycast(new RaycastContext(
                                            camera.getPos(),
                                            entity.getEyePos(),
                                            RaycastContext.ShapeType.OUTLINE,
                                            RaycastContext.FluidHandling.NONE,
                                            player
                                    )).getType() == HitResult.Type.MISS;
                                }
                        )
                        .min(Comparator.comparingDouble(player::distanceTo))
                        .orElse(null);
            } while (Mod.lockOnTarget == null && wrapAround-- > 0);
        }
    }*/
}
