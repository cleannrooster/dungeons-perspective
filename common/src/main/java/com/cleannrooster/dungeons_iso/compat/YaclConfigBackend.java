package com.cleannrooster.dungeons_iso.compat;

import com.cleannrooster.dungeons_iso.config.Config;
import com.cleannrooster.dungeons_iso.config.ConfigBackend;
import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import dev.isxander.yacl3.platform.YACLPlatform;

/**
 * Config persistence through YACL, exactly as the mod has always done it.
 *
 * <p>Every reference to a YACL class in the persistence path lives in this file, so a install
 * without YACL never loads it. Instantiated reflectively from {@code ConfigHolder}.
 */
public final class YaclConfigBackend implements ConfigBackend<Config> {

    private final ConfigClassHandler<Config> handler = ConfigClassHandler
            .createBuilder(Config.class)
            .serializer(config -> GsonConfigSerializerBuilder
                    .create(config)
                    .setPath(YACLPlatform.getConfigDir().resolve("dungeons_iso_v5.json"))
                    .build())
            .build();

    @Override
    public Config instance() {
        return this.handler.instance();
    }

    @Override
    public void load() {
        this.handler.load();
    }

    @Override
    public void save() {
        this.handler.save();
    }

    @Override
    public Object handler() {
        return this.handler;
    }
}
