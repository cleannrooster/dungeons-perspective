package com.cleannrooster.dungeons_iso.mixin;

import com.bawnorton.mixinsquared.adjuster.tools.AdjustableAnnotationNode;
import com.bawnorton.mixinsquared.api.MixinAnnotationAdjuster;
import org.objectweb.asm.tree.MethodNode;

import java.util.List;

public class CleannMixinAdjuster  implements MixinAnnotationAdjuster {
    @Override
    public AdjustableAnnotationNode adjust(List<String> targetClassNames, String mixinClassName, MethodNode method, AdjustableAnnotationNode annotation) {
        /*
        if (mixinClassName.equals("net.dehydration.mixin.PotionItemMixin")) {
            return null;
        }

         */
        return annotation;
    }
}
