package wtf.kity.minecraftxiv;

import eu.midnightdust.lib.config.MidnightConfig;
import net.fabricmc.api.ModInitializer;

public class MainInit implements ModInitializer {
    @Override
    public void onInitialize() {
        MidnightConfig.init("minecraftxiv", Config.class);
    }
}
