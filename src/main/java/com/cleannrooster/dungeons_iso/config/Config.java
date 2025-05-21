package com.cleannrooster.dungeons_iso.config;

import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import dev.isxander.yacl3.platform.YACLPlatform;

public class Config {
    public static final ConfigClassHandler<Config> GSON = ConfigClassHandler
            .createBuilder(Config.class)
            .serializer(config -> GsonConfigSerializerBuilder
                    .create(config)
                    .setPath(YACLPlatform.getConfigDir().resolve("dungeons_iso.json"))
                    .build())
            .build();

    @SerialEntry
    public boolean scrollWheelZoom = true;
    @SerialEntry
    public boolean dynamicCamera = false;
    @SerialEntry
    public boolean forceNoDefer = true;
    @SerialEntry

    public float moveFactor = 2.0F;
    @SerialEntry
    public float fov = 70.0F;

}
