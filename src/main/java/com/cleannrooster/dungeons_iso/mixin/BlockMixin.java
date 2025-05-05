package com.cleannrooster.dungeons_iso.mixin;

import com.cleannrooster.dungeons_iso.api.MinecraftClientAccessor;
import com.cleannrooster.dungeons_iso.mod.Mod;
import net.fabricmc.fabric.api.block.v1.FabricBlock;
import net.fabricmc.fabric.api.block.v1.FabricBlockState;
import net.fabricmc.fabric.impl.client.indigo.renderer.mesh.MutableQuadViewImpl;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.AbstractBlockRenderContext;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.BlockRenderInfo;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.BlockView;
import net.minecraft.world.RaycastContext;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = AbstractBlockRenderContext.class,priority = Integer.MIN_VALUE)
public abstract class BlockMixin  {
    @Shadow
    protected  BlockRenderInfo blockInfo;

    @Inject(
            method = "renderQuad", at = @At("HEAD"),cancellable = true,remap = false
    )
    private void renderQuadCleann(MutableQuadViewImpl quad, CallbackInfo callbackInfo) {

        AbstractBlockRenderContext info = (AbstractBlockRenderContext)(Object) this;
        if(MinecraftClient.getInstance() != null && MinecraftClient.getInstance().player instanceof ClientPlayerEntity player &&Mod.enabled  && ((MinecraftClientAccessor)MinecraftClient.getInstance()).shouldRebuild()) {
            Box box = new Box(player.getEyePos(),MinecraftClient.getInstance().gameRenderer.getCamera().getPos()).expand(1,0,1);
           /* if((blockInfo.blockPos.getX() < box.maxX && blockInfo.blockPos.getX() > box.minX - 1)
                    && (blockInfo.blockPos.getY() < box.maxY && blockInfo.blockPos.getY() > box.minY)
                    && (blockInfo.blockPos.getZ() < box.maxZ && blockInfo.blockPos.getZ() > box.minZ -1)
                ){*/
            if(MinecraftClient.getInstance().gameRenderer.getCamera() instanceof Camera camera &&blockInfo.blockPos.toCenterPos().y > player.getEyePos().y &&
                    (Math.abs(Vec3d.fromPolar(camera.getPitch(),camera.getYaw()).normalize().dotProduct(blockInfo.blockPos.toCenterPos().subtract( camera.getPos()).normalize())) > 0.90 || camera.getPos().distanceTo(blockInfo.blockPos.toCenterPos()) < 3)

                     && (blockInfo.blockPos.getX() < box.maxX && blockInfo.blockPos.getX() > box.minX-1)
                    && (blockInfo.blockPos.getY() < box.maxY && blockInfo.blockPos.getY() > box.minY)
                    && (blockInfo.blockPos.getZ() < box.maxZ && blockInfo.blockPos.getZ() > box.minZ-1))
            {
                callbackInfo.cancel();
            }
        }
    }


}
