package com.cleannrooster.dungeons_iso.mixin.compat.sodium;

import com.cleannrooster.dungeons_iso.api.BlockCuller;
import com.cleannrooster.dungeons_iso.api.BlockCullerUser;
import com.cleannrooster.dungeons_iso.api.MinecraftClientAccessor;
import com.cleannrooster.dungeons_iso.api.cullers.FloodCuller;
import com.cleannrooster.dungeons_iso.api.cullers.room.RoomScanner;
import com.cleannrooster.dungeons_iso.api.cullers.room.RoomSnapshot;
import com.cleannrooster.dungeons_iso.api.cullers.room.SightlineScanner;
import com.cleannrooster.dungeons_iso.compat.SodiumCompat;
import com.cleannrooster.dungeons_iso.config.Config;
import com.cleannrooster.dungeons_iso.mod.Mod;
import net.caffeinemc.mods.sodium.api.util.ColorARGB;
import net.caffeinemc.mods.sodium.client.model.light.LightMode;
import net.caffeinemc.mods.sodium.client.model.light.LightPipeline;
import net.caffeinemc.mods.sodium.client.model.light.LightPipelineProvider;
import net.caffeinemc.mods.sodium.client.model.light.data.QuadLightData;
import net.caffeinemc.mods.sodium.client.render.frapi.helper.ColorHelper;
import net.caffeinemc.mods.sodium.client.render.frapi.mesh.MutableQuadViewImpl;
import net.caffeinemc.mods.sodium.client.render.frapi.render.AbstractBlockRenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.IceBlock;
import net.minecraft.block.TranslucentBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.screen.EnchantmentScreenHandler;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Map;

import static com.google.common.collect.ImmutableList.of;
import static net.minecraft.client.render.RenderPhase.*;

@Mixin(AbstractBlockRenderContext.class)
public abstract class AbstractRenderContextMixin implements BlockCullerUser {
    @Shadow    protected BlockPos pos;
    @Shadow
    protected BlockState state;
    @Shadow
    protected RenderLayer type;
@Shadow
private  MutableQuadViewImpl editorQuad;

    @Shadow
    protected LightPipelineProvider lighters;
@Shadow
    protected  QuadLightData quadLightData ;
    @Inject(at = @At("RETURN"), method = "isFaceCulled", cancellable = true)
    protected final void isFaceCulledDungeons(@Nullable Direction direction, CallbackInfoReturnable<Boolean> ci) {
        try {
            // Sodium decides face occlusion geometrically: a face is culled when the neighbouring
            // block's shape covers it. That test has no idea we cancelled the neighbour's
            // rendering, so every face that was hidden behind a removed block stays hidden after
            // the block is gone — you look down into a room and see through the tops of the walls.
            // Wherever the occluding neighbour is one the room or shape culler removes, the face
            // has to come back.
            int nx = 0, ny = 0, nz = 0;
            boolean neighbourRemoved = false;
            if (direction != null && Mod.enabled) {
                nx = pos.getX() + direction.getOffsetX();
                ny = pos.getY() + direction.getOffsetY();
                nz = pos.getZ() + direction.getOffsetZ();
                neighbourRemoved = RoomScanner.INSTANCE.test(nx, ny, nz) == RoomSnapshot.CULL
                        || SightlineScanner.INSTANCE.shouldCull(nx, ny, nz);
            }

            if (neighbourRemoved) {
                // Back-face culling still gets the final say, matching how the branches below
                // resolve it, so enabling backCull does not start punching holes here instead.
                ci.setReturnValue(Config.GSON.instance().backCull
                        && direction.pointsTo(MinecraftClient.getInstance().gameRenderer.getCamera().getYaw()));
                return;
            }

            VoxelShape selfShape = direction != null ? state.getCullingFace(MinecraftClient.getInstance().world, pos, direction) : null;

            if (MinecraftClient.getInstance() != null && MinecraftClient.getInstance().player != null && Mod.enabled && !(state.getBlock() instanceof TranslucentBlock) && Mod.shouldRebuild()) {
                if (MinecraftClient.getInstance().cameraEntity != null) {
                    boolean bool = pos.toCenterPos().getY() > MinecraftClient.getInstance().cameraEntity.getBlockPos().up().getY();
                    boolean boo3 = pos.toCenterPos().distanceTo(Mod.preMod) < Mod.getZoom() * Mod.zoomMetric * 1.25F;
                    boolean bool3 = false;

                    if (bool && boo3) {


                    }

                    boolean bool2 = Mod.preMod.subtract(MinecraftClient.getInstance().cameraEntity.getPos()).dotProduct(pos.toCenterPos().subtract(MinecraftClient.getInstance().cameraEntity.getPos())) > 0;
                    BlockPos.Mutable mutable = MinecraftClient.getInstance().cameraEntity.getBlockPos().mutableCopy();

                    while(mutable.getY() > MinecraftClient.getInstance().world.getBottomY() && !MinecraftClient.getInstance().world.getBlockState(mutable).blocksMovement()) {
                        mutable.move(Direction.DOWN);
                    }
                    if(pos.getY() > mutable.getY() -8){
                        return;
                    }
                    if(!(state.getBlock() instanceof TranslucentBlock)) {

                            ci.setReturnValue((Config.GSON.instance().backCull && direction != null && direction.pointsTo(MinecraftClient.getInstance().gameRenderer.getCamera().getYaw())));

                    }
                    return;
                }


            }
            if(Mod.enabled){
                if(!(state.getBlock() instanceof TranslucentBlock)){
                    if((Config.GSON.instance().backCull &&  direction != null && direction.pointsTo(MinecraftClient.getInstance().gameRenderer.getCamera().getYaw()))){

                        ci.setReturnValue((Config.GSON.instance().backCull && direction != null && direction.pointsTo(MinecraftClient.getInstance().gameRenderer.getCamera().getYaw())));
                        return;
                    }

                }


            }
        }
        catch(Exception ignored){

        }
    }

}
