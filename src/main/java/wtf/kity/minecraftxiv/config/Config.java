package wtf.kity.minecraftxiv.config;

import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import dev.isxander.yacl3.platform.YACLPlatform;

public class Config {
    public static final ConfigClassHandler<Config> GSON = ConfigClassHandler.createBuilder(Config.class)
            .serializer(config -> GsonConfigSerializerBuilder.create(config)
                    .setPath(YACLPlatform.getConfigDir().resolve("minecraftxiv.json"))
                    .build())
            .build();
    
    @SerialEntry
    public boolean scrollWheelZoom = true;

    @SerialEntry
    public boolean movementCameraRelative = true;

    @SerialEntry
    public boolean targetFromCamera = false;

    @SerialEntry
    public boolean unlimitedReach = false;
}
