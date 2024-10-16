package wtf.kity.minecraftxiv.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public class ModDisallowedPayload implements CustomPayload {

    public static final ModDisallowedPayload INSTANCE = new ModDisallowedPayload();
    public static final CustomPayload.Id<ModDisallowedPayload> ID = new CustomPayload.Id<>(Identifier.of("minecraftxiv", "is_disallowed"));
    public static final PacketCodec<RegistryByteBuf, ModDisallowedPayload> CODEC = PacketCodec.unit(INSTANCE);

    private ModDisallowedPayload() { }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
