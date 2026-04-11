package com.cleannrooster.dungeons_iso.mixin;

import com.cleannrooster.dungeons_iso.compat.SodiumCompat;
import com.cleannrooster.dungeons_iso.config.Config;
import com.cleannrooster.dungeons_iso.mod.Mod;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LightType;
import net.minecraft.world.chunk.ChunkNibbleArray;
import net.minecraft.world.chunk.light.ChunkLightProvider;
import net.minecraft.world.chunk.light.LightingProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Map;

@Mixin(LevelSlice.class)
public class LightingProviderMixin {
/*    @Inject(
            method = "getBaseLightLevel",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    public void getBaseLightLevelXIV(BlockPos pos, int ambientDarkness, CallbackInfoReturnable<Integer> cir) {
        boolean bool = false;

        if (Mod.enabled && Config.GSON.instance().fogOfWar    ) {


            if(SodiumCompat.fogOfWar != null && SodiumCompat.fogOfWar.realPoints != null) {
                try {
                    List<HitResult> points = SodiumCompat.fogOfWar.realPoints != null ? List.copyOf(SodiumCompat.fogOfWar.realPoints) : List.of();
                    for (HitResult result :
                            points) {
                        if (result.getPos().distanceTo(pos.toCenterPos())> 4 || pos.toCenterPos().distanceTo(MinecraftClient.getInstance().cameraEntity.getPos())<4) {
                            bool = true;
                        }
                        else{
                            bool = false;
                            break;
                        }
                    }
                }
                catch (Exception ignored) {
                }
            }

        }
        else{
            bool = true;
        }
        if (bool) {
            return;
        }
        cir.setReturnValue(0);

    }
    @Inject(
            method = "getLightLevel",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    public void getLightLevelXIV(LightType type, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        if (Mod.enabled && Config.GSON.instance().fogOfWar    ) {

            boolean bool = false;

            if(SodiumCompat.fogOfWar != null && SodiumCompat.fogOfWar.realPoints != null) {
                try {
                    List<HitResult> points = SodiumCompat.fogOfWar.realPoints != null ? List.copyOf(SodiumCompat.fogOfWar.realPoints) : List.of();
                    for (HitResult result :
                            points) {
                        if (result.getPos().distanceTo(pos.toCenterPos()) > 4 || pos.toCenterPos().distanceTo(MinecraftClient.getInstance().cameraEntity.getPos())<4) {
                            bool = true;
                        }
                        else{
                            bool = false;
                            break;
                        }
                    }
                }
                catch (Exception ignored) {
                }
            }
            if (bool) {
                return;
            }
            cir.setReturnValue(0);
        }

    }*/
}
