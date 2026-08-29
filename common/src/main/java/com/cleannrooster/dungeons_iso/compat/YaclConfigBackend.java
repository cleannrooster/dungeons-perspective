package com.cleannrooster.dungeons_iso.compat;

import com.cleannrooster.dungeons_iso.config.Config;
import com.cleannrooster.dungeons_iso.config.ConfigBackend;
import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import dev.isxander.yacl3.platform.YACLPlatform;

public final class YaclConfigBackend implements ConfigBackend<Config> {
    private final ConfigClassHandler<Config> handler = ConfigClassHandler
            .createBuilder(Config.class)
            .serializer(config -> GsonConfigSerializerBuilder.create(config)
                    .setPath(YACLPlatform.getConfigDir().resolve("dungeons_iso_v5.json"))
                    .build())
            .build();

    public Config instance() { return handler.instance(); }
    public void load() { handler.load(); }
    public void save() { handler.save(); }
    public Object handler() { return handler; }
}
