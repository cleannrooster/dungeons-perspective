package wtf.kity.minecraftxiv;

import eu.midnightdust.lib.config.MidnightConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import wtf.kity.minecraftxiv.network.Capabilities;

public class Config extends MidnightConfig {
    public static final String OPTIONS = "options";
    public static final String CAPABILITIES = "capabilities";

    @Entry(category = OPTIONS) public static boolean scrollWheelZoom;

    @Comment(category = CAPABILITIES) public static Comment capabilities;
    @Entry(category = CAPABILITIES) public static boolean targetFromCamera;

    @Environment(EnvType.SERVER)
    @Override
    public void writeChanges(String modID) {
        super.writeChanges(modID);

        for (ServerPlayerEntity player : ServerInit.minecraftServer.getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(player, new Capabilities(Config.targetFromCamera));
        }
    }
}
