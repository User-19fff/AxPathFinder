package com.artillexstudios.axpathfinder.listeners;

import com.artillexstudios.axpathfinder.AxPathFinder;
import com.artillexstudios.axpathfinder.item.WandItem;
import com.artillexstudios.axpathfinder.managers.PathManager;
import com.artillexstudios.axpathfinder.utils.PlayerUtils;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.NotNull;

public class PlayerListener implements Listener {
    private final AxPathFinder plugin = AxPathFinder.getInstance();
    private final PathManager pathManager;
    private final WandItem wandItem;

    public PlayerListener(PathManager pathManager, WandItem wandItem) {
        this.pathManager = pathManager;
        this.wandItem = wandItem;
    }

    @EventHandler
    public void onPlayerQuit(final @NotNull PlayerQuitEvent event) {
        Player player = event.getPlayer();
        pathManager.cancelPlayerPaths(player);
    }

    @EventHandler
    public void onPlayerInteract(final @NotNull PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        Block clickedBlock = event.getClickedBlock();

        if (clickedBlock == null) return;
        if (!player.hasPermission("axpath.wand")) return;

        if (!wandItem.isWand(player.getInventory().getItemInMainHand())) return;

        event.setCancelled(true);

        if (player.isSneaking()) {
            if (!pathManager.hasWandFirstPoint(player)) {
                PlayerUtils.sendMessage(player, plugin.getLanguage().getString("wand.no-first-point"));
                return;
            }

            String pathName = "path_" + System.currentTimeMillis() % 10000;

            if (player.hasPermission("axpath.admin")) {
                PlayerUtils.sendMessage(player, plugin.getLanguage().getString("wand.enter-name"));
                plugin.getScheduler().runLater(() -> wandItem.handleSecondPoint(player, clickedBlock.getLocation(), pathName), 1L);
            } else wandItem.handleSecondPoint(player, clickedBlock.getLocation(), pathName);
        } else wandItem.handleFirstPoint(player, clickedBlock.getLocation());
    }
}
