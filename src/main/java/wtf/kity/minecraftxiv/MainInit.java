package wtf.kity.minecraftxiv;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import wtf.kity.minecraftxiv.network.Capabilities;

import java.io.IOException;

public class MainInit implements ModInitializer {
    public static MinecraftServer minecraftServer;
    public static Capabilities capabilities;

    @Override
    public void onInitialize() {
        capabilities = Capabilities.load();
        if (capabilities == null) {
            capabilities = Capabilities.none();
        }
        ServerLifecycleEvents.SERVER_STARTED.register((MinecraftServer minecraftServer) -> MainInit.minecraftServer = minecraftServer);
        PayloadTypeRegistry.playS2C().register(Capabilities.ID, Capabilities.CODEC);
        PayloadTypeRegistry.playC2S().register(Capabilities.ID, Capabilities.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(Capabilities.ID, (payload, context) -> {
            if (!context.player().hasPermissionLevel(2)) {
                return;
            }

            if (!payload.equals(capabilities)) {
                capabilities = payload;
                try {
                    Capabilities.save(capabilities);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                for (ServerPlayerEntity player : minecraftServer.getPlayerManager().getPlayerList()) {
                    ServerPlayNetworking.send(player, capabilities);
                }
            }
        });
    }
}
