package com.artillexstudios.axpathfinder.managers;

import com.artillexstudios.axapi.scheduler.ScheduledTask;
import com.artillexstudios.axpathfinder.AxPathFinder;
import com.artillexstudios.axpathfinder.models.Path;
import com.artillexstudios.axpathfinder.models.PathRenderer;
import org.jetbrains.annotations.NotNull;

public class UpdateManager {
    private final AxPathFinder plugin = AxPathFinder.getInstance();
    private final PathManager pathManager;
    private final PathRenderer pathRenderer;
    private ScheduledTask updateTask;
    private ScheduledTask renderTask;

    public UpdateManager(@NotNull PathManager pathManager, @NotNull PathRenderer pathRenderer) {
        this.pathManager = pathManager;
        this.pathRenderer = pathRenderer;
    }

    public void startUpdates() {
        if (updateTask != null) updateTask.cancel();
        if (renderTask != null) renderTask.cancel();

        int pathUpdateInterval = plugin.getPaths().getInt("settings.pathfinding.path-update-interval", 5);
        int renderUpdateInterval = plugin.getPaths().getInt("settings.particles.update-interval", 3);

        pathUpdateInterval = Math.max(1, pathUpdateInterval);
        renderUpdateInterval = Math.max(1, renderUpdateInterval);

        updateTask = plugin.getScheduler().runTimer(pathManager::updateAllPaths, 10L, pathUpdateInterval);

        renderTask = plugin.getScheduler().runTimer(() -> {
            for (Path path : pathManager.getActivePaths().values()) {
                if (path.isActive() && !path.isExpired()) pathRenderer.renderPath(path);
            }
        }, 5L, renderUpdateInterval);
    }

    public void stopUpdates() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }

        if (renderTask != null) {
            renderTask.cancel();
            renderTask = null;
        }

        pathRenderer.stopAll();
    }
}