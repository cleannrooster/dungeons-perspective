package com.cleannrooster.dungeons_iso.config;

public interface ConfigBackend<T> {
    T instance();
    void load();
    void save();
    Object handler();
}
