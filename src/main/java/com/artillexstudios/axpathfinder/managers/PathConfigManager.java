package com.artillexstudios.axpathfinder.managers;

import com.artillexstudios.axapi.config.Config;
import com.artillexstudios.axpathfinder.AxPathFinder;
import org.jetbrains.annotations.NotNull;

public class PathConfigManager {
    private static final AxPathFinder plugin = AxPathFinder.getInstance();
    private static final Config pathsConfig = plugin.getPaths();

    public static void addPath(@NotNull String pathId) {
        if (pathsConfig.get("paths." + pathId) != null) {
            pathsConfig.set("paths." + pathId + ".messages.started", plugin.getConfiguration().getStringList("default-messages.started"));
            pathsConfig.set("paths." + pathId + ".messages.finished", plugin.getConfiguration().getStringList("default-messages.finished"));
            pathsConfig.set("paths." + pathId + ".settings.duration", 120);
            pathsConfig.set("paths." + pathId + ".settings.track-player", true);
            pathsConfig.set("paths." + pathId + ".settings.recalculate-distance", 10);
            pathsConfig.set("paths." + pathId + ".settings.particles.type", "DUST");
            pathsConfig.set("paths." + pathId + ".settings.particles.color", "0,191,255");
            pathsConfig.save();
        }
    }

    public static void setPathDestination(@NotNull String pathId, @NotNull String world, double x, double y, double z) {
        addPath(pathId);

        pathsConfig.set("paths." + pathId + ".destination.world", world);
        pathsConfig.set("paths." + pathId + ".destination.x", x);
        pathsConfig.set("paths." + pathId + ".destination.y", y);
        pathsConfig.set("paths." + pathId + ".destination.z", z);

        pathsConfig.save();
    }
}
