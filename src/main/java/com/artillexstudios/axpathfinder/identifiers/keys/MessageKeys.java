package com.artillexstudios.axpathfinder.identifiers.keys;

import com.artillexstudios.axapi.config.Config;
import com.artillexstudios.axpathfinder.AxPathFinder;
import com.artillexstudios.axpathfinder.processor.MessageProcessor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Getter
public enum MessageKeys {
    RELOAD("messages.reload"),

    NO_PERMISSION("messages.no-permission"),
    PLAYER_REQUIRED("messages.player-required"),
    CANT_SPAWN_MORE("messages.cant-spawn-more"),
    CANT_SPAWN_HERE("messages.cant-spawn-here");

    private final String path;
    private static final Config config = AxPathFinder.getInstance().getLanguage();

    MessageKeys(@NotNull String path) {
        this.path = path;
    }

    public @NotNull String getMessage() {
        return MessageProcessor.process(config.getString(path))
                .replace("%prefix%", MessageProcessor.process(config.getString("prefix")));
    }

    public List<String> getMessages() {
        return config.getStringList(path)
                .stream()
                .toList();
    }
}
