package com.artillexstudios.axpathfinder.data;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

@Getter
public class PathPoint {
    private final String worldName;
    private final double x;
    private final double y;
    private final double z;

    public PathPoint(@NotNull String worldName, double x, double y, double z) {
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public PathPoint(@NotNull Location location) {
        this.worldName = location.getWorld().getName();
        this.x = location.getX();
        this.y = location.getY();
        this.z = location.getZ();
    }

    public Location toLocation() {
        World world = Bukkit.getWorld(worldName);
        if (world == null) throw new IllegalStateException("A világ nem található: " + worldName);
        return new Location(world, x, y, z);
    }

    public double distanceSquared(@NotNull PathPoint other) {
        if (!worldName.equals(other.worldName)) return Double.MAX_VALUE;

        double dx = x - other.x;
        double dy = y - other.y;
        double dz = z - other.z;

        return dx * dx + dy * dy + dz * dz;
    }

    public double distance(@NotNull PathPoint other) {
        return Math.sqrt(distanceSquared(other));
    }

    public double distance2D(@NotNull PathPoint other) {
        if (!worldName.equals(other.worldName)) return Double.MAX_VALUE;

        double dx = x - other.x;
        double dz = z - other.z;

        return Math.sqrt(dx * dx + dz * dz);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PathPoint pathPoint = (PathPoint) o;
        return Double.compare(pathPoint.x, x) == 0 &&
                Double.compare(pathPoint.y, y) == 0 &&
                Double.compare(pathPoint.z, z) == 0 &&
                Objects.equals(worldName, pathPoint.worldName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(worldName, x, y, z);
    }

    @Override
    public String toString() {
        return "PathPoint{" +
                "world='" + worldName + '\'' +
                ", x=" + x +
                ", y=" + y +
                ", z=" + z +
                '}';
    }
}
