package com.artillexstudios.axpathfinder.managers;

import com.artillexstudios.axapi.libs.boostedyaml.boostedyaml.block.implementation.Section;
import com.artillexstudios.axpathfinder.AxPathFinder;
import com.artillexstudios.axpathfinder.data.PathPoint;
import com.artillexstudios.axpathfinder.models.Path;
import com.artillexstudios.axpathfinder.models.PathFinder;
import com.artillexstudios.axpathfinder.models.PathRenderer;
import com.artillexstudios.axpathfinder.utils.PlayerUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PathManager {
    private final AxPathFinder plugin = AxPathFinder.getInstance();
    private final ConcurrentHashMap<String, Path> activePaths = new ConcurrentHashMap<>();
    private final PathFinder pathfinder;
    private final PathRenderer pathRenderer;

    public PathManager(PathRenderer pathRenderer) {
        this.pathfinder = new PathFinder();
        this.pathRenderer = pathRenderer;
    }

    public void startPath(@Nullable Player player, @Nullable String pathType) {
        if (player == null || pathType == null) return;

        UUID playerUUID = player.getUniqueId();
        for (Path path : activePaths.values()) {
            if (path.getPlayerUUID().equals(playerUUID)) return;
        }

        Section pathConfig = plugin.getPaths().getSection("paths." + pathType);

        if (pathConfig == null) {
            plugin.getLogger().warning("Az útvonal nem létezik: " + pathType);
            return;
        }

        Section destSection = pathConfig.getSection("destination");
        if (destSection == null) {
            plugin.getLogger().warning("Az útvonalnak nincs célpontja: " + pathType);
            return;
        }

        String worldName = destSection.getString("world", "world");
        double x = destSection.getDouble("x");
        double y = destSection.getDouble("y");
        double z = destSection.getDouble("z");

        Location destination = new Location(Bukkit.getWorld(worldName), x, y, z);

        int duration = pathConfig.getInt("settings.duration", 120);
        boolean trackPlayer = pathConfig.getBoolean("settings.track-player", true);
        int recalculateDistance = pathConfig.getInt("settings.recalculate-distance", 10);

        String pathId = UUID.randomUUID().toString().substring(0, 8);

        Path path = new Path(pathId, pathType, player, player.getLocation(), destination,
                duration, trackPlayer, recalculateDistance);

        List<PathPoint> points = pathfinder.findPath(player.getLocation(), destination);

        if (points == null || points.isEmpty()) {
            plugin.getLogger().warning("Nem sikerült útvonalat találni: " + player.getName() + " -> " + destination);
            return;
        }

        path.setPathPoints(points);

        activePaths.put(pathId, path);

        List<String> startMessages = pathConfig.getStringList("messages.started");
        PlayerUtils.sendMessages(player, startMessages);

    }

    public int cancelPlayerPaths(@Nullable Player player) {
        if (player == null) return 0;

        UUID playerUUID = player.getUniqueId();
        int cancelCount = 0;

        for (Map.Entry<String, Path> entry : activePaths.entrySet()) {
            if (entry.getValue().getPlayerUUID().equals(playerUUID)) {
                String pathId = entry.getKey();
                entry.getValue().deactivate();
                pathRenderer.stopRendering(pathId);
                activePaths.remove(pathId);
                cancelCount++;
            }
        }

        return cancelCount;
    }

    public void cancelAllPaths() {
        for (String pathId : activePaths.keySet()) {
            Path path = activePaths.get(pathId);
            path.deactivate();
            pathRenderer.stopRendering(pathId);
        }

        activePaths.clear();
    }

    public void updatePath(@NotNull String pathId) {
        Path path = activePaths.get(pathId);
        if (path == null || !path.isActive()) return;

        Player player = Bukkit.getPlayer(path.getPlayerUUID());
        if (player == null || !player.isOnline()) {
            pathRenderer.stopRendering(pathId);
            activePaths.remove(pathId);
            return;
        }

        if (path.isExpired()) {
            Section pathConfig = plugin.getPaths().getSection("paths." + path.getPathType());
            if (pathConfig != null) {
                List<String> expiredMessages = pathConfig.getStringList("messages.expired");
                PlayerUtils.sendMessages(player, expiredMessages);
            }

            pathRenderer.stopRendering(pathId);
            activePaths.remove(pathId);
            return;
        }

        if (path.hasReachedDestination(player)) {
            Section pathConfig = plugin.getPaths().getSection("paths." + path.getPathType());
            if (pathConfig != null) {
                List<String> finishMessages = pathConfig.getStringList("messages.finished");
                PlayerUtils.sendMessages(player, finishMessages);
            }

            pathRenderer.stopRendering(pathId);
            activePaths.remove(pathId);
            return;
        }

        if (path.isTrackPlayer() && path.needsRecalculation(player)) {
            path.updateStartLocation(player);
            List<PathPoint> newPoints = pathfinder.findPath(player.getLocation(), path.getDestinationLocation());

            if (newPoints != null && !newPoints.isEmpty()) path.setPathPoints(newPoints);
        }

    }

    public void updateAllPaths() {
        for (String pathId : activePaths.keySet().toArray(new String[0])) {
            updatePath(pathId);
        }
    }

    public boolean createPathFromPoints(String pathName, Location startLoc, Location endLoc) {
        if (pathName == null || startLoc == null || endLoc == null) {
            return false;
        }

        PathConfigManager.setPathDestination(pathName,
                endLoc.getWorld().getName(),
                endLoc.getX(),
                endLoc.getY(),
                endLoc.getZ());

        return true;
    }

    public Map<String, Path> getActivePaths() {
        return activePaths;
    }
}