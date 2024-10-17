package wtf.kity.minecraftxiv.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record Capabilities(boolean targetFromCamera) implements CustomPayload {
    public static final CustomPayload.Id<Capabilities> ID = new CustomPayload.Id<>(Identifier.of("minecraftxiv", "capabilities"));
    public static final PacketCodec<RegistryByteBuf, Capabilities> CODEC = PacketCodec.tuple(
            PacketCodecs.BOOL, Capabilities::targetFromCamera,
            Capabilities::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public static Capabilities all() {
        return new Capabilities(true);
    }

    public static Capabilities none() {
        return new Capabilities(false);
    }
}
