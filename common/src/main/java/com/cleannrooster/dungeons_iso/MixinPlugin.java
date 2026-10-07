package com.cleannrooster.dungeons_iso;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class MixinPlugin  implements IMixinConfigPlugin {
    /**
     * Temporary test switch. Keep the Sodium compatibility sources and mixin declarations in
     * place, but do not apply them while validating the vanilla/Indigo culling path.
     */
    private static final boolean ENABLE_SODIUM_COMPAT = false;

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {

        if (!ENABLE_SODIUM_COMPAT && (mixinClassName.contains(".compat.sodium.")
                || mixinClassName.endsWith(".FabricBlockAccessMixin"))) {
            return false;
        }

        if (mixinClassName.contains(".compat.")) {
            String[] parts = mixinClassName.split("\\.");
            for (int i = 0; i < parts.length; i++) {
                if (parts[i].equals("compat") && i + 1 < parts.length) {
                    String modId = parts[i + 1];
                    return ModCompat.isModLoaded(modId);
                }
            }
            // This means there was a failure in parsing the mod id
            return false;
        }
        return true;
    }


    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
