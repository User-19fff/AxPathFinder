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

    public CommandPath(PathManager pathManager) {
        this.pathManager = pathManager;
    }

    @Subcommand("reload")
    @CommandPermission("axpath.reload")
    public void reload(@NotNull CommandSender sender) {
        plugin.getConfiguration().reload();
        plugin.getLanguage().reload();
        sender.sendMessage(MessageKeys.RELOAD.getMessage());
    }

    @Subcommand("start")
    @CommandPermission("axpath.start.others")
    public void onStartOther(@NotNull CommandSender sender, @NotNull Player target, @NotNull String pathName) {
        pathManager.startPath(target, pathName);
    }

    @Subcommand("cancel")
    @CommandPermission("axpath.cancel.others")
    public void onCancelOther(@NotNull Player player, @NotNull Player target) {
        int count = pathManager.cancelPlayerPaths(target);
        if (count > 0) {
            PlayerUtils.sendMessage(player, plugin.getLanguage().getString("commands.path-cancelled-other")
                    .replace("%player%", target.getName())
                    .replace("%count%", String.valueOf(count)));
        } else PlayerUtils.sendMessage(player, plugin.getLanguage().getString("commands.no-active-paths-other").replace("%player%", target.getName()));
    }
}
