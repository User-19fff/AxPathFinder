package com.artillexstudios.axpathfinder.models;

import com.artillexstudios.axapi.libs.boostedyaml.boostedyaml.block.implementation.Section;
import com.artillexstudios.axapi.scheduler.ScheduledTask;
import com.artillexstudios.axpathfinder.AxPathFinder;
import com.artillexstudios.axpathfinder.data.ParticlePoint;
import com.artillexstudios.axpathfinder.data.PathPoint;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class PathRenderer {
    private final AxPathFinder plugin = AxPathFinder.getInstance();
    private final ConcurrentHashMap<String, ScheduledTask> renderTasks = new ConcurrentHashMap<>();
    private final int updateInterval;
    private final double heightOffset;
    private final int density;
    private final boolean showCheckpoints;

    private final int checkpointSpacing;
    private final double checkpointSize;

    public PathRenderer() {
        this.updateInterval = Math.max(1, plugin.getPaths().getInt("settings.particles.update-interval", 5));
        this.heightOffset = plugin.getPaths().getDouble("settings.particles.height-offset", 0.1);
        this.density = Math.max(1, plugin.getPaths().getInt("settings.particles.density", 3));
        this.showCheckpoints = plugin.getPaths().getBoolean("settings.particles.show-destination", true);

        this.checkpointSpacing = plugin.getPaths().getInt("settings.particles.checkpoint-spacing", 10);
        this.checkpointSize = plugin.getPaths().getDouble("settings.particles.checkpoint-size", 0.5);
    }

    public void renderPath(@NotNull Path path) {
        if (renderTasks.containsKey(path.getId())) return;

        Player player = plugin.getServer().getPlayer(path.getPlayerUUID());
        if (player == null || !player.isOnline()) return;

        if (path.getPathPoints().isEmpty()) {
            Location start = player.getLocation();
            Location destination = path.getDestinationLocation();

            if (destination != null) {
                PathFinder pathFinder = new PathFinder();
                List<PathPoint> calculatedPath = pathFinder.findPath(start, destination);

                if (calculatedPath != null && !calculatedPath.isEmpty()) {
                    List<PathPoint> enhancedPath = enhancePathWithPrecision(calculatedPath);
                    path.setPathPoints(enhancedPath);
                }
            }
        }

        ScheduledTask task = plugin.getScheduler().runTimer(() -> {
            if (!path.isActive() || path.isExpired() || !player.isOnline()) {
                stopRendering(path.getId());
                return;
            }

            renderParticles(path, player);
        }, 2, updateInterval);

        renderTasks.put(path.getId(), task);
    }

    public void stopRendering(@NotNull String pathId) {
        ScheduledTask task = renderTasks.remove(pathId);
        if (task != null) task.cancel();
    }

    public void stopAll() {
        for (ScheduledTask task : renderTasks.values()) {
            task.cancel();
        }
        renderTasks.clear();
    }

    @NotNull
    private List<ParticlePoint> generateDetailedPathWithCollisionDetection(@NotNull List<PathPoint> points, @NotNull World world) {
        List<ParticlePoint> particlePath = new ArrayList<>();
        if (points.size() < 2) return particlePath;

        for (int i = 0; i < points.size() - 1; i++) {
            PathPoint current = points.get(i);
            PathPoint next = points.get(i + 1);

            if (!current.getWorldName().equals(world.getName()) || !next.getWorldName().equals(world.getName())) {
                continue;
            }

            double dx = next.getX() - current.getX();
            double dy = next.getY() - current.getY();
            double dz = next.getZ() - current.getZ();

            double distance = Math.sqrt(dx*dx + dy*dy + dz*dz);
            int steps = Math.max(5, (int)(distance * density * 1.5));
            boolean significantHeightChange = Math.abs(dy) > 0.5;

            for (int step = 0; step < steps; step++) {
                double progress = (double) step / steps;
                double x = current.getX() + (dx * progress);
                double y = current.getY() + (dy * progress);
                double z = current.getZ() + (dz * progress);

                Location testLoc = new Location(world, x, y, z);

                Block block = world.getBlockAt(testLoc);
                if (!block.isPassable() && !block.isLiquid()) {
                    boolean adjusted = false;

                    for (double yOffset = 0.1; yOffset <= 1.0; yOffset += 0.1) {
                        Location adjustedLoc = new Location(world, x, y + yOffset, z);
                        Block adjustedBlock = world.getBlockAt(adjustedLoc);

                        if (adjustedBlock.isPassable() || adjustedBlock.isLiquid()) {
                            y += yOffset;
                            adjusted = true;
                            break;
                        }
                    }

                    if (!adjusted) continue;
                }

                y += heightOffset;

                if (significantHeightChange && step > 0 && step < steps - 1) {
                    float size = 1.0f;
                    particlePath.add(new ParticlePoint(new Location(world, x, y, z), size));

                    if (Math.abs(dy) > 1.0 && step % 2 == 0) {
                        double verticalOffset = dy > 0 ? 0.5 : -0.5;
                        particlePath.add(new ParticlePoint(new Location(world, x, y + verticalOffset, z), 0.5f));
                    }
                } else {
                    float size = 0.6f + (step == 0 || step == steps - 1 ? 0.2f : 0f);
                    particlePath.add(new ParticlePoint(new Location(world, x, y, z), size));
                }
            }
        }

        return particlePath;
    }

    @NotNull
    private List<PathPoint> enhancePathWithPrecision(@NotNull List<PathPoint> originalPath) {
        if (originalPath.size() <= 2) return originalPath;

        List<PathPoint> enhancedPath = Collections.synchronizedList(new ArrayList<>());
        enhancedPath.add(originalPath.getFirst());

        for (int i = 1; i < originalPath.size() - 1; i++) {
            PathPoint prev = originalPath.get(i - 1);
            PathPoint current = originalPath.get(i);
            PathPoint next = originalPath.get(i + 1);

            enhancedPath.add(current);

            double dxPrev = current.getX() - prev.getX();
            double dzPrev = current.getZ() - prev.getZ();
            double dxNext = next.getX() - current.getX();
            double dzNext = next.getZ() - current.getZ();

            if ((Math.abs(dxPrev) > 0.5 && Math.abs(dzNext) > 0.5) || (Math.abs(dzPrev) > 0.5 && Math.abs(dxNext) > 0.5)) {

                PathPoint corner = new PathPoint(
                        current.getWorldName(),
                        current.getX(),
                        current.getY(),
                        next.getZ()
                );

                if (!corner.equals(current) && !corner.equals(next)) {
                    enhancedPath.add(corner);
                }

                PathPoint secondCorner = new PathPoint(
                        current.getWorldName(),
                        next.getX(),
                        current.getY(),
                        current.getZ()
                );

                if (!secondCorner.equals(corner) && !secondCorner.equals(current) && !secondCorner.equals(next)) enhancedPath.add(secondCorner);
            }
        }

        enhancedPath.add(originalPath.getLast());

        List<PathPoint> finalPath = Collections.synchronizedList(new ArrayList<>());
        finalPath.add(enhancedPath.getFirst());

        for (int i = 1; i < enhancedPath.size(); i++) {
            PathPoint current = enhancedPath.get(i);
            PathPoint prev = enhancedPath.get(i - 1);

            double dy = current.getY() - prev.getY();
            if (Math.abs(dy) > 1.5) {
                int steps = Math.max(2, (int)(Math.abs(dy)));

                for (int step = 1; step < steps; step++) {
                    double t = (double) step / steps;
                    double x = prev.getX() + (current.getX() - prev.getX()) * t;
                    double z = prev.getZ() + (current.getZ() - prev.getZ()) * t;
                    double y = prev.getY() + (step * dy / steps);

                    finalPath.add(new PathPoint(current.getWorldName(), x, y, z));
                }
            }

            finalPath.add(current);
        }

        return finalPath;
    }

    private void renderParticles(@NotNull Path path, @NotNull Player player) {
        List<PathPoint> points = path.getPathPoints();
        if (points.isEmpty()) return;

        Section pathConfig = plugin.getPaths().getSection("paths." + path.getPathType());
        if (pathConfig == null) return;

        Section particleConfig = pathConfig.getSection("settings.particles");

        String particleType = particleConfig != null ?
                particleConfig.getString("type", plugin.getPaths().getString("settings.particles.default-type", "DUST")) :
                "DUST";

        String colorStr = particleConfig != null ?
                particleConfig.getString("color", plugin.getPaths().getString("settings.particles.default-color", "0,191,255")) :
                "0,191,255";

        String fadeColorStr = particleConfig != null ?
                particleConfig.getString("fade-color", null) :
                null;

        Color color = parseColor(colorStr);
        Color fadeColor = fadeColorStr != null ? parseColor(fadeColorStr) : null;
        Particle particle;

        try {
            particle = Particle.valueOf(particleType);
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().warning("Invalid particle type: " + particleType + ". Using DUST as fallback.");
            particle = Particle.DUST;
        }

        World world = player.getWorld();
        List<Integer> checkpointIndices = calculateCheckpoints(points);

        List<ParticlePoint> particlePath = generateDetailedPathWithCollisionDetection(points, world);

        for (ParticlePoint particlePoint : particlePath) {
            spawnParticle(particle, particlePoint.location(), player, color, fadeColor, particlePoint.size());
        }

        for (Integer checkpointIndex : checkpointIndices) {
            if (checkpointIndex >= points.size()) continue;

            PathPoint checkpoint = points.get(checkpointIndex);
            if (!checkpoint.getWorldName().equals(world.getName())) continue;

            Location checkpointLoc = checkpoint.toLocation().add(0, heightOffset, 0);
            renderCheckpointMarker(checkpointLoc, world, particle, player, color, Objects.requireNonNull(fadeColor), checkpointIndex == 0 || checkpointIndex == points.size() - 1);
        }

        if (!points.isEmpty() && showCheckpoints) {
            PathPoint destination = points.getLast();
            Location destLoc = destination.toLocation().add(0, heightOffset, 0);
            renderDestinationMarker(destLoc, world, particle, player, color, Objects.requireNonNull(fadeColor));
        }
    }

    private void renderCheckpointMarker(@NotNull Location loc, @NotNull World world, @NotNull Particle particle, @NotNull Player player, @NotNull Color color, @NotNull Color fadeColor, boolean isEndpoint) {
        double radius = checkpointSize;
        int particleCount = isEndpoint ? 16 : 12;
        float size = isEndpoint ? 1.3f : 1.0f;

        for (int j = 0; j < particleCount; j++) {
            double angle = 2 * Math.PI * j / particleCount;
            double x = loc.getX() + radius * Math.cos(angle);
            double y = loc.getY() + (isEndpoint ? 0.3 : 0.1);
            double z = loc.getZ() + radius * Math.sin(angle);

            Location particleLoc = new Location(world, x, y, z);
            spawnParticle(particle, particleLoc, player, color, fadeColor, size);
        }
    }

    private void renderDestinationMarker(@NotNull Location destLoc, @NotNull World world, @NotNull Particle particle, @NotNull Player player, @NotNull Color color, @NotNull Color fadeColor) {
        double radius = 0.7;
        int particles = 16;
        double animationOffset = (System.currentTimeMillis() % 2000) / 2000.0;

        for (int i = 0; i < particles; i++) {
            double angle = 2 * Math.PI * i / particles;
            double x = destLoc.getX() + radius * Math.cos(angle);
            double y = destLoc.getY() + 0.5 + Math.sin(animationOffset * Math.PI * 2) * 0.2;
            double z = destLoc.getZ() + radius * Math.sin(angle);

            Location particleLoc = new Location(world, x, y, z);
            spawnParticle(particle, particleLoc, player, color, fadeColor, 1.5f);
        }

        for (int i = 0; i < 5; i++) {
            double y = destLoc.getY() + i * 0.25;
            spawnParticle(particle, new Location(world, destLoc.getX(), y, destLoc.getZ()),
                    player, color, fadeColor, 1.5f);
        }
    }

    @NotNull
    private List<Integer> calculateCheckpoints(@NotNull List<PathPoint> points) {
        List<Integer> checkpoints = new ArrayList<>();
        if (points.size() <= 2) {
            return checkpoints;
        }

        checkpoints.add(0);

        double distanceAccumulator = 0;
        for (int i = 1; i < points.size(); i++) {
            PathPoint prev = points.get(i-1);
            PathPoint current = points.get(i);

            double segmentLength = prev.distance(current);
            distanceAccumulator += segmentLength;

            if (distanceAccumulator >= checkpointSpacing) {
                checkpoints.add(i);
                distanceAccumulator = 0;
            }
        }

        if (!checkpoints.contains(points.size() - 1)) checkpoints.add(points.size() - 1);
        return checkpoints;
    }

    private void spawnParticle(Particle particle, Location loc, Player player, Color color, Color fadeColor, float size) {
        if (particle == Particle.DUST && color != null) player.spawnParticle(particle, loc, 1, 0, 0, 0, 0, new Particle.DustOptions(color, size));
        else if (particle == Particle.DUST_COLOR_TRANSITION && color != null && fadeColor != null) player.spawnParticle(particle, loc, 1, 0, 0, 0, 0, new Particle.DustTransition(color, fadeColor, size));
        else player.spawnParticle(particle, loc, 1, 0, 0, 0, 0);
    }

    private Color parseColor(@NotNull String colorStr) {
        try {
            String[] rgb = colorStr.split(",");
            int r = Integer.parseInt(rgb[0].trim());
            int g = Integer.parseInt(rgb[1].trim());
            int b = Integer.parseInt(rgb[2].trim());

            return Color.fromRGB(r, g, b);
        } catch (Exception exception) {
            plugin.getLogger().warning("Failed to parse color: " + colorStr + ". Using default color.");
            return Color.AQUA;
        }
    }
}