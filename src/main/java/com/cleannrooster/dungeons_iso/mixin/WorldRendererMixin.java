package com.cleannrooster.dungeons_iso.mixin;

import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {
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
