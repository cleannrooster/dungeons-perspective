package wtf.kity.minecraftxiv;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import wtf.kity.minecraftxiv.network.Capabilities;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public class ServerInit implements DedicatedServerModInitializer {
    public static MinecraftServer minecraftServer;

    @Override
    public void onInitializeServer() {
        ServerLifecycleEvents.SERVER_STARTED.register((MinecraftServer minecraftServer) -> {
            ServerInit.minecraftServer = minecraftServer;
        });
        PayloadTypeRegistry.playS2C().register(Capabilities.ID, Capabilities.CODEC);
    }
}