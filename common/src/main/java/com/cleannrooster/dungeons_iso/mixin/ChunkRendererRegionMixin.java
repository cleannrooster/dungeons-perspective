package com.cleannrooster.dungeons_iso.mixin;

import com.cleannrooster.dungeons_iso.api.cullers.room.TerrainCulling;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.render.chunk.ChunkRendererRegion;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Culls terrain on every non-Sodium chunk renderer, by lying to the mesher about what is there.
 *
 * <p>The obvious hook — cancelling {@code BlockRenderManager.renderBlock} — does not work in
 * practice. Fabric API's Indigo renderer {@code @Redirect}s that exact call inside
 * {@link net.minecraft.client.render.chunk.SectionBuilder}, sending anything with a non-vanilla
 * model to {@code TerrainRenderContext.tessellateBlock} instead, so with Sodium removed and Fabric
 * API present the injection sits on a call that is never made.
 *
 * <p>{@code ChunkRendererRegion} is the snapshot of the world a section is built from, and it is
 * upstream of that fork: vanilla, Indigo and NeoForge all ask it what block is at a position.
 * Reporting air is therefore a single hook that removes the block from every one of them — and it
 * removes it properly, because everything else the mesher derives from that state follows along:
 *
 * <ul>
 *   <li>the fluid is gone, since {@code SectionBuilder} reads it off the returned state;</li>
 *   <li>the block entity is gone, for the same reason;</li>
 *   <li>faces on the neighbouring blocks come back, because vanilla's occlusion test asks this
 *       region what is next door and now hears air — no separate face hook needed;</li>
 *   <li>smooth lighting relights those faces as if the space were open;</li>
 *   <li>the section stops being marked closed in the visibility graph, so traversal reaches
 *       through the opening instead of hiding what the roof used to cover.</li>
 * </ul>
 *
 * <p>Sodium bypasses this class entirely — it builds from its own world slice — which is why the
 * hooks under {@code mixin.compat.sodium} still exist and why this one stands down when Sodium is
 * installed.
 */
@Mixin(ChunkRendererRegion.class)
public abstract class ChunkRendererRegionMixin {

    @ModifyReturnValue(method = "getBlockState", at = @At("RETURN"))
    private BlockState cullTerrainXIV(BlockState original, BlockPos pos) {
        // Hot path: called for every block and every neighbour of every section build.
        if (TerrainCulling.SODIUM || TerrainCulling.idle()) {
            return original;
        }
        try {
            if (TerrainCulling.shouldRemove(original, pos.getX(), pos.getY(), pos.getZ())) {
                return Blocks.AIR.getDefaultState();
            }
        } catch (Exception ignored) {
        }
        return original;
    }
}
