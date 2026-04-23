package com.cleannrooster.dungeons_iso.mixin;

import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import org.spongepowered.asm.mixin.Mixin;

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
                        if (result.getPos().distanceTo(pos.toCenterPos())> 4 || pos.toCenterPos().distanceTo(Minecraft.getInstance().cameraEntity.getPos())<4) {
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
    public void getLightLevelXIV(LightLayer type, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        if (Mod.enabled && Config.GSON.instance().fogOfWar    ) {

            boolean bool = false;

            if(SodiumCompat.fogOfWar != null && SodiumCompat.fogOfWar.realPoints != null) {
                try {
                    List<HitResult> points = SodiumCompat.fogOfWar.realPoints != null ? List.copyOf(SodiumCompat.fogOfWar.realPoints) : List.of();
                    for (HitResult result :
                            points) {
                        if (result.getPos().distanceTo(pos.toCenterPos()) > 4 || pos.toCenterPos().distanceTo(Minecraft.getInstance().cameraEntity.getPos())<4) {
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
