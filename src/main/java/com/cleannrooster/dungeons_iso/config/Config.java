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
                    .setPath(YACLPlatform.getConfigDir().resolve("dungeons_iso_v5.json"))
                    .build())
            .build();
    @SerialEntry
    public boolean XIV =  false;

    @SerialEntry
    public boolean onStartup =  true;
    @SerialEntry
    public boolean force =  false;

    @SerialEntry
    public boolean scrollWheelZoom = true;
    @SerialEntry
    public boolean dynamicCamera = false;
    @SerialEntry
    public boolean forceNoDefer =  false;
    @SerialEntry
    public boolean cameraRelative =  true;

    @SerialEntry
    public boolean turnToMouse =  true;
    @SerialEntry
    public boolean clipToSpace =  false;

    @SerialEntry
    public boolean additionalMeleeAssistance =  false;
    @SerialEntry
    public boolean forceAutoJump =  true;
    @SerialEntry
    public boolean rollTowardsCursor =  true;
    @SerialEntry

    public float moveFactor_v3 = 0.5F;
    @SerialEntry
    public float fov = 45.0F;
    @SerialEntry
    public float zoomFactor = 1.5F;
    @SerialEntry
    public float zNearFactor = 1F;

    @SerialEntry
    public boolean ortho = false;
    @SerialEntry

    public boolean clickToMove = false;

    @SerialEntry

    public boolean frustumCulling = false;

}
