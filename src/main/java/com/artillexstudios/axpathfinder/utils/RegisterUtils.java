package com.artillexstudios.axpathfinder.utils;

import com.artillexstudios.axpathfinder.AxPathFinder;
import com.artillexstudios.axpathfinder.commands.CommandPath;
import com.artillexstudios.axpathfinder.handler.ErrorHandler;
import com.artillexstudios.axpathfinder.identifiers.keys.ConfigKeys;
import com.artillexstudios.axpathfinder.listeners.PlayerListener;
import com.artillexstudios.axpathfinder.managers.PathManager;
import com.artillexstudios.axpathfinder.managers.UpdateManager;
import com.artillexstudios.axpathfinder.models.PathRenderer;
import lombok.experimental.UtilityClass;
import org.bukkit.Bukkit;
import revxrsal.commands.bukkit.BukkitCommandHandler;
import revxrsal.commands.orphan.Orphans;

import java.util.Locale;

@SuppressWarnings("deprecation")
@UtilityClass
public class RegisterUtils {
    public static PathManager pathManager;
    public static PathRenderer pathRenderer;
    public static UpdateManager updateManager;
    private static final AxPathFinder plugin = AxPathFinder.getInstance();

    public void registerAll() {
        pathRenderer = new PathRenderer();
        pathManager = new PathManager(pathRenderer);
        updateManager = new UpdateManager(pathManager, pathRenderer);

        registerCommands();
        Bukkit.getServer().getPluginManager().registerEvents(new PlayerListener(pathManager), plugin);

        updateManager.startUpdates();
    }

    public void shutdown() {
        if (updateManager != null) updateManager.stopUpdates();
        if (pathManager != null) pathManager.cancelAllPaths();
        if (pathRenderer != null) pathRenderer.stopAll();
    }

    private void registerCommands() {
        LoggerUtils.info("### Registering commands... ###");

        BukkitCommandHandler handler = BukkitCommandHandler.create(plugin);

        handler.getTranslator().add(new ErrorHandler());
        handler.setLocale(new Locale("en", "US"));
        handler.register(Orphans.path(ConfigKeys.ALIASES.getList().toArray(String[]::new)).handler(new CommandPath(pathManager)));
        handler.registerBrigadier();

        LoggerUtils.info("### Successfully registered exception handlers... ###");
    }
}
