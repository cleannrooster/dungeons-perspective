package com.cleannrooster.dungeons_iso.mixin;

import net.minecraft.world.level.lighting.LightEngine;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LightEngine.class)
public class WrapperProtoChunkMixin {
/*    @Inject(
            method = "getLightLevel",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    public void getLightLevelXIV(BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        boolean bool = false;

        if (Mod.enabled && Config.GSON.instance().fogOfWar    ) {


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

        }
        else{
            bool = true;
        }
        if (bool) {
            return;
        }
        cir.setReturnValue(0);

    }*/
}
