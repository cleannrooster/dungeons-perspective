package com.cleannrooster.dungeons_iso.network;

import com.google.gson.*;
import dev.isxander.yacl3.platform.YACLPlatform;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public record Capabilities(boolean targetFromCamera, boolean unlimitedReach) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<Capabilities> ID = new CustomPacketPayload.Type<>(Identifier.of("dungeons_iso", "capabilities"));
    public static final StreamCodec<RegistryFriendlyByteBuf, Capabilities> CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            Capabilities::targetFromCamera,
            ByteBufCodecs.BOOL,
            Capabilities::unlimitedReach,
            Capabilities::new
    );

    public static Capabilities all() {
        return new Capabilities(true, true);
    }

    public static Capabilities none() {
        return new Capabilities(false, false);
    }

    public static Capabilities load() {
        try {
            FileReader reader = new FileReader(YACLPlatform
                    .getConfigDir()
                    .resolve("dungeons_iso.capabilities.json")
                    .toFile());
            return new GsonBuilder()
                    .registerTypeAdapter(Capabilities.class, new CapabilitiesDeserializer())
                    .create()
                    .fromJson(reader, Capabilities.class);
        } catch (FileNotFoundException e) {
            return Capabilities.none();
        }
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public Capabilities withTargetFromCamera(boolean targetFromCamera) {
        return new Capabilities(targetFromCamera, this.unlimitedReach);
    }

    public Capabilities withUnlimitedReach(boolean unlimitedReach) {
        return new Capabilities(this.targetFromCamera, unlimitedReach);
    }

    public void save() throws IOException {
        FileWriter writer = new FileWriter(YACLPlatform
                .getConfigDir()
                .resolve("dungeons_iso.capabilities.json")
                .toFile());
        new GsonBuilder().setPrettyPrinting().create().toJson(this, Capabilities.class, writer);
        writer.close();
    }

    private static class CapabilitiesDeserializer implements JsonDeserializer<Capabilities> {
        @Override
        public Capabilities deserialize(
                JsonElement json, java.lang.reflect.Type typeOfT, JsonDeserializationContext context
        ) throws JsonParseException {
            Capabilities capabilities = Capabilities.none();
            if (json instanceof JsonObject object) {
                if (object.get("targetFromCamera") instanceof JsonPrimitive primitive) {
                    capabilities = capabilities.withTargetFromCamera(primitive.getAsBoolean());
                }
                if (object.get("unlimitedReach") instanceof JsonPrimitive primitive) {
                    capabilities = capabilities.withUnlimitedReach(primitive.getAsBoolean());
                }
            }
            return capabilities;
        }
    }
}
