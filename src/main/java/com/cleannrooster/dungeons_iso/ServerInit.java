package com.cleannrooster.dungeons_iso;

import com.cleannrooster.dungeons_iso.network.Capabilities;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

import java.io.IOException;

public class ServerInit implements DedicatedServerModInitializer {
    public static Capabilities capabilities;

    @Override
    public void onInitializeServer() {
        capabilities = Capabilities.load();

        PayloadTypeRegistry.playC2S().register(Capabilities.ID, Capabilities.CODEC);
        PayloadTypeRegistry.playS2C().register(Capabilities.ID, Capabilities.CODEC);

        ServerPlayConnectionEvents.JOIN.register((networkHandler, packetSender, minecraftServer) -> {
            packetSender.sendPacket(capabilities);
        });

        ServerPlayNetworking.registerGlobalReceiver(Capabilities.ID, (payload, context) -> {
            if (!context.player().hasPermissionLevel(2)) {
                return;
            }

            if (!payload.equals(capabilities)) {
                capabilities = payload;
                try {
                    capabilities.save();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                for (ServerPlayerEntity player : context.server().getPlayerManager().getPlayerList()) {
                    ServerPlayNetworking.send(player, capabilities);
                }
            }
        });
    }
}
