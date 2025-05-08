package com.artillexstudios.axpathfinder.utils;

import com.artillexstudios.axpathfinder.processor.MessageProcessor;
import lombok.experimental.UtilityClass;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@UtilityClass
public class PlayerUtils {
    public void sendMessages(@NotNull Player player, @NotNull List<String> messages) {
        for (String message : messages) {
            player.sendMessage(MessageProcessor.process(message));
        }
    }

    public static void sendMessage(@NotNull Player player, String message) {
        if (message == null || message.isEmpty()) return;
        player.sendMessage(MessageProcessor.process(message));
    }
}
