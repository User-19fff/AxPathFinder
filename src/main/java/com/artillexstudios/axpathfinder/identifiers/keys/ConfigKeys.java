package com.artillexstudios.axpathfinder.identifiers.keys;

import com.artillexstudios.axapi.config.Config;
import com.artillexstudios.axpathfinder.AxPathFinder;
import com.artillexstudios.axpathfinder.processor.MessageProcessor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Getter
public enum ConfigKeys {
    ALIASES("aliases");

    private final String path;
    private static final Config config = AxPathFinder.getInstance().getConfiguration();

    ConfigKeys(@NotNull String path) {
        this.path = path;
    }

    public @NotNull String getString() {
        return MessageProcessor.process(config.getString(path));
    }

    public static @NotNull String getString(@NotNull String path) {
        return config.getString(path);
    }

    public boolean getBoolean() {
        return config.getBoolean(path);
    }

    public int getInt() {
        return config.getInt(path);
    }

    public List<String> getList() {
        return config.getList(path);
    }
}
