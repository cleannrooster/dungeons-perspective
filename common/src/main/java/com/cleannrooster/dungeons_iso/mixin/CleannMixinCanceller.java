package com.cleannrooster.dungeons_iso.mixin;

import com.bawnorton.mixinsquared.api.MixinCanceller;

import java.util.List;

public class CleannMixinCanceller  implements MixinCanceller {
    @Override
    public boolean shouldCancel(List<String> targetClassNames, String mixinClassName) {
        /*
        if (mixinClassName.equals("net.dehydration.mixin.PotionItemMixin")) {
            Medieval.LOG.info("Disabled Dehydration PotionItemMixin");
            return true;
        }

         */
        return false;
    }
}
