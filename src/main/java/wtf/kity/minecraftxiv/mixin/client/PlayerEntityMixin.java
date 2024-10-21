package wtf.kity.minecraftxiv.mixin.client;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wtf.kity.minecraftxiv.ClientInit;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin {
    @Inject(method = "canInteractWithBlockAt", at = @At("RETURN"), cancellable = true)
    public void canInteractWithBlockAt(BlockPos pos, double additionalRange, CallbackInfoReturnable<Boolean> cir) {
        if (ClientInit.getCapabilities().unlimitedReach()) {
            cir.setReturnValue(true);
            cir.cancel();
        }
    }
}