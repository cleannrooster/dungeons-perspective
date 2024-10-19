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

    @SerialEntry(comment = "Zoom with scroll wheel (override hotbar scrolling)")
    public boolean scrollWheelZoom = true;

    @SerialEntry(comment = "Pick target from cursor position instead of player line of sight")
    public boolean targetFromCamera = false;
}
