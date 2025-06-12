package com.cleannrooster.dungeons_iso.mixin;

import com.cleannrooster.dungeons_iso.api.BuiltChunkAccessor;
import com.cleannrooster.dungeons_iso.api.ChunkDataAccessor;
import com.cleannrooster.dungeons_iso.api.MinecraftClientAccessor;
import com.cleannrooster.dungeons_iso.api.WorldRendererAccessor;
import com.cleannrooster.dungeons_iso.mod.Mod;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer;
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

@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin implements WorldRendererAccessor {
    @Shadow
    private double lastTranslucentSortX;
    private static Vec3d getMaxIntensityColorXIV(float hue) {
        float f = 5.99999F;
        int i = (int)(MathHelper.clamp(hue, 0.0F, 1.0F) * 5.99999F);
        float g = hue * 5.99999F - (float)i;
        Vec3d var10000;
        switch (i) {
            case 0:
                var10000 = new Vec3d(1.0, (double)g, 0.0);
                break;
            case 1:
                var10000 = new Vec3d((double)(1.0F - g), 1.0, 0.0);
                break;
            case 2:
                var10000 = new Vec3d(0.0, 1.0, (double)g);
                break;
            case 3:
                var10000 = new Vec3d(0.0, 1.0 - (double)g, 1.0);
                break;
            case 4:
                var10000 = new Vec3d((double)g, 0.0, 1.0);
                break;
            case 5:
                var10000 = new Vec3d(1.0, 0.0, 1.0 - (double)g);
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + i);
        }

        return var10000;
    }

    private static Vec3d shiftHueXIV(float red, float green, float blue, float hueOffset) {
        Vec3d vec3d = getMaxIntensityColorXIV(hueOffset).multiply((double)red);
        Vec3d vec3d2 = getMaxIntensityColorXIV((hueOffset + 0.33333334F) % 1.0F).multiply((double)green);
        Vec3d vec3d3 = getMaxIntensityColorXIV((hueOffset + 0.6666667F) % 1.0F).multiply((double)blue);
        Vec3d vec3d4 = vec3d.add(vec3d2).add(vec3d3);
        double d = Math.max(Math.max(1.0, vec3d4.x), Math.max(vec3d4.y, vec3d4.z));
        return new Vec3d(vec3d4.x / d, vec3d4.y / d, vec3d4.z / d);
    }
    private static void drawCuboidShapeOutlineXIV(MatrixStack matrices, VertexConsumer vertexConsumer, VoxelShape shape, double offsetX, double offsetY, double offsetZ, float red, float green, float blue, float alpha) {
        MatrixStack.Entry entry = matrices.peek();
        shape.forEachEdge((minX, minY, minZ, maxX, maxY, maxZ) -> {
            float k = (float)(maxX - minX);
            float l = (float)(maxY - minY);
            float m = (float)(maxZ - minZ);
            float n = MathHelper.sqrt(k * k + l * l + m * m);
            k /= n;
            l /= n;
            m /= n;
            vertexConsumer.vertex(entry, (float)(minX + offsetX), (float)(minY + offsetY), (float)(minZ + offsetZ)).color(red, green, blue, alpha).normal(entry, k, l, m);
            vertexConsumer.vertex(entry, (float)(maxX + offsetX), (float)(maxY + offsetY), (float)(maxZ + offsetZ)).color(red, green, blue, alpha).normal(entry, k, l, m);
        });
    }
        @Override
    public ObjectArrayList<ChunkBuilder.BuiltChunk> chunks() {
        return builtChunks;
    }

    @Shadow
    private double lastTranslucentSortY;
    @Shadow
    private double lastTranslucentSortZ;
    @Shadow
    private  ObjectArrayList<ChunkBuilder.BuiltChunk> builtChunks;

    @Inject(method = "renderLayer", at = @At("HEAD"),cancellable = true)
    private void renderLayerDungeons(RenderLayer renderLayer, double x, double y, double z, Matrix4f matrix4f, Matrix4f positionMatrix, CallbackInfo callbackInfo) {
        if(Mod.enabled ){


        }

    }

    @Inject(method = "drawShapeOutline", at = @At("HEAD"),cancellable = true)
    private static void drawShapeOutlineXIV(MatrixStack matrices, VertexConsumer vertexConsumer, VoxelShape shape, double offsetX, double offsetY, double offsetZ, float red, float green, float blue, float alpha, boolean colorize, CallbackInfo info) {
        if(Mod.enabled  ) {
            info.cancel();
            List<Box> list = shape.getBoundingBoxes();
            if (!list.isEmpty()) {
                int i = colorize ? list.size() : list.size() * 8;
                drawCuboidShapeOutlineXIV(matrices, vertexConsumer, VoxelShapes.cuboid((Box) list.get(0).expand(0.2)), offsetX, offsetY, offsetZ, red, green, blue, alpha);

                for (int j = 1; j < list.size(); ++j) {
                    Box box = (Box) list.get(j).expand(0.2);
                    float f = (float) j / (float) i;
                    Vec3d vec3d = shiftHueXIV(red, green, blue, f);
                    drawCuboidShapeOutlineXIV(matrices, vertexConsumer, VoxelShapes.cuboid(box), offsetX, offsetY, offsetZ, (float) vec3d.x, (float) vec3d.y, (float) vec3d.z, alpha);
                }

            }
        }
    }
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
