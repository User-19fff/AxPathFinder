package com.artillexstudios.axpathfinder.commands;

import com.artillexstudios.axpathfinder.AxPathFinder;
import com.artillexstudios.axpathfinder.identifiers.keys.MessageKeys;
import com.artillexstudios.axpathfinder.item.WandItem;
import com.artillexstudios.axpathfinder.managers.PathManager;
import com.artillexstudios.axpathfinder.utils.PlayerUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.bukkit.annotation.CommandPermission;
import revxrsal.commands.orphan.OrphanCommand;

public class CommandPath implements OrphanCommand {
    private final AxPathFinder plugin = AxPathFinder.getInstance();
    private final PathManager pathManager;
    private final WandItem wandItem;

    public CommandPath(PathManager pathManager, WandItem wandItem) {
        this.pathManager = pathManager;
        this.wandItem = wandItem;
    }

    @Subcommand("reload")
    @CommandPermission("axpath.reload")
    public void reload(@NotNull CommandSender sender) {
        plugin.getConfiguration().reload();
        plugin.getLanguage().reload();
        sender.sendMessage(MessageKeys.RELOAD.getMessage());
    }

    @Subcommand("wand")
    @CommandPermission("axpath.wand")
    public void onWand(@NotNull Player player) {
        player.getInventory().addItem(wandItem.createWand());
        PlayerUtils.sendMessage(player, plugin.getLanguage().getString("commands.wand"));
    }

    @Subcommand("start")
    @CommandPermission("axpath.start.others")
    public void onStartOther(Player player, Player target, String pathName) {
        startPath(player, target, pathName);
    }

    @Subcommand("cancel")
    @CommandPermission("axpath.cancel.others")
    public void onCancelOther(Player player, Player target) {
        int count = pathManager.cancelPlayerPaths(target);
        if (count > 0) {
            PlayerUtils.sendMessage(player, plugin.getLanguage().getString("commands.path-cancelled-other")
                    .replace("%player%", target.getName())
                    .replace("%count%", String.valueOf(count)));
        } else PlayerUtils.sendMessage(player, plugin.getLanguage().getString("commands.no-active-paths-other").replace("%player%", target.getName()));
    }

    private void startPath(Player sender, Player target, String pathName) {
        boolean success = pathManager.startPath(target, pathName);

        if (success) {
            if (sender != null && sender != target) {
                PlayerUtils.sendMessage(sender, plugin.getLanguage().getString("commands.path-started-other")
                        .replace("%player%", target.getName())
                        .replace("%path%", pathName));
            }
        } else if (sender != null) {
            PlayerUtils.sendMessage(sender, plugin.getLanguage().getString("commands.path-not-found")
                    .replace("%path%", pathName));
        }
    }
}
