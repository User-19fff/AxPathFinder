package com.artillexstudios.axpathfinder.models;

import com.artillexstudios.axpathfinder.data.PathPoint;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class Path {
    @Getter private final String id;
    @Getter private final String pathType;
    @Getter private final UUID playerUUID;
    @Getter private Location startLocation;
    @Getter private Location destinationLocation;
    private final List<PathPoint> pathPoints = Collections.synchronizedList(new ArrayList<>());
    @Getter private long creationTime;
    @Getter private long expirationTime;
    @Getter private boolean active = true;
    @Getter private boolean trackPlayer;
    @Getter private int recalculateDistance;

    public Path(@NotNull String id,
                @NotNull String pathType,
                @NotNull Player player,
                @NotNull Location startLocation,
                @NotNull Location destinationLocation,
                int duration,
                boolean trackPlayer,
                int recalculateDistance) {
        this.id = id;
        this.pathType = pathType;
        this.playerUUID = player.getUniqueId();
        this.startLocation = startLocation;
        this.destinationLocation = destinationLocation;
        this.trackPlayer = trackPlayer;
        this.recalculateDistance = recalculateDistance;

        this.creationTime = System.currentTimeMillis();
        if (duration > 0) this.expirationTime = this.creationTime + (duration * 1000L);
        else this.expirationTime = 0;
    }

    public boolean isExpired() {
        if (expirationTime == 0) return false;
        return System.currentTimeMillis() > expirationTime;
    }

    public boolean hasReachedDestination(@Nullable Player player) {
        if (player == null || destinationLocation == null) return false;

        if (!player.getWorld().equals(destinationLocation.getWorld())) return false;

        double distanceSquared = player.getLocation().distanceSquared(destinationLocation);
        return distanceSquared <= 9.0;
    }

    public boolean needsRecalculation(@Nullable Player player) {
        if (!trackPlayer || player == null || pathPoints.isEmpty()) return false;

        PathPoint closestPoint = findClosestPointToPlayer(player);
        if (closestPoint == null) return true;

        double distanceSquared = player.getLocation().distanceSquared(closestPoint.toLocation());
        return distanceSquared > (recalculateDistance * recalculateDistance);
    }

    public PathPoint findClosestPointToPlayer(@Nullable Player player) {
        if (pathPoints.isEmpty() || player == null) return null;

        PathPoint closest = null;
        double closestDistSq = Double.MAX_VALUE;

        Location playerLoc = player.getLocation();

        for (PathPoint point : pathPoints) {
            Location pointLoc = point.toLocation();
            if (pointLoc.getWorld().equals(playerLoc.getWorld())) {
                double distSq = pointLoc.distanceSquared(playerLoc);
                if (distSq < closestDistSq) {
                    closestDistSq = distSq;
                    closest = point;
                }
            }
        }

        return closest;
    }

    public void updateStartLocation(@Nullable Player player) {
        if (player != null) this.startLocation = player.getLocation();
    }

    public void setPathPoints(List<PathPoint> points) {
        this.pathPoints.clear();
        if (points != null) this.pathPoints.addAll(points);
    }

    public void deactivate() {
        this.active = false;
    }


    public List<PathPoint> getPathPoints() {
        return new ArrayList<>(pathPoints);
    }
}
