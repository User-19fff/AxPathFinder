package com.artillexstudios.axpathfinder.listeners;

import com.artillexstudios.axpathfinder.managers.PathManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

public class PlayerListener implements Listener {
    private final PathManager pathManager;

    public PlayerListener(@NotNull PathManager pathManager) {
        this.pathManager = pathManager;
    }

    @EventHandler
    public void onPlayerQuit(final @NotNull PlayerQuitEvent event) {
        Player player = event.getPlayer();
        pathManager.cancelPlayerPaths(player);
    }
}
