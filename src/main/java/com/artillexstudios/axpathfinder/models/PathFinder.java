package com.artillexstudios.axpathfinder.models;

import com.artillexstudios.axapi.config.Config;
import com.artillexstudios.axpathfinder.AxPathFinder;
import com.artillexstudios.axpathfinder.data.PathPoint;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class PathFinder {
    private final int maxSearchDistance;
    private final boolean allowDiagonal;
    private final boolean checkLineOfSight;

    private final Set<Material> passableMaterials = Collections.synchronizedSet(new HashSet<>());

    public PathFinder() {
        AxPathFinder plugin = AxPathFinder.getInstance();
        Config pathConfig = plugin.getPaths();
        this.maxSearchDistance = pathConfig.getInt("settings.pathfinding.max-search-distance", 100);
        this.allowDiagonal = pathConfig.getBoolean("settings.pathfinding.allow-diagonal", true);
        this.checkLineOfSight = pathConfig.getBoolean("settings.pathfinding.check-line-of-sight", true);

        initPassableMaterials();
    }

    private void initPassableMaterials() {
        passableMaterials.add(Material.AIR);
        passableMaterials.add(Material.CAVE_AIR);
        passableMaterials.add(Material.VOID_AIR);

        passableMaterials.add(Material.SHORT_GRASS);
        passableMaterials.add(Material.TALL_GRASS);
        passableMaterials.add(Material.FERN);
        passableMaterials.add(Material.LARGE_FERN);

        passableMaterials.add(Material.POPPY);
        passableMaterials.add(Material.DANDELION);
        passableMaterials.add(Material.BLUE_ORCHID);
        passableMaterials.add(Material.ALLIUM);
        passableMaterials.add(Material.AZURE_BLUET);
        passableMaterials.add(Material.RED_TULIP);
        passableMaterials.add(Material.ORANGE_TULIP);
        passableMaterials.add(Material.WHITE_TULIP);
        passableMaterials.add(Material.PINK_TULIP);
        passableMaterials.add(Material.OXEYE_DAISY);
        passableMaterials.add(Material.LILY_OF_THE_VALLEY);
        passableMaterials.add(Material.CORNFLOWER);

        passableMaterials.add(Material.TORCH);
        passableMaterials.add(Material.WALL_TORCH);
        passableMaterials.add(Material.REDSTONE_TORCH);
        passableMaterials.add(Material.REDSTONE_WALL_TORCH);
        passableMaterials.add(Material.SNOW);
        passableMaterials.add(Material.RAIL);
        passableMaterials.add(Material.POWERED_RAIL);
        passableMaterials.add(Material.DETECTOR_RAIL);
        passableMaterials.add(Material.ACTIVATOR_RAIL);
        passableMaterials.add(Material.COBWEB);
        passableMaterials.add(Material.VINE);

        passableMaterials.add(Material.WATER);
        passableMaterials.add(Material.LAVA);
    }

    public List<PathPoint> findPath(Location start, Location end) {
        if (start == null || end == null || start.getWorld() == null || end.getWorld() == null) {
            return null;
        }

        if (!start.getWorld().equals(end.getWorld())) {
            return null;
        }

        return findPathAStar(new PathPoint(start), new PathPoint(end));
    }

    @Nullable
    private List<PathPoint> findPathAStar(@NotNull PathPoint start, PathPoint end) {
        World world = org.bukkit.Bukkit.getWorld(start.getWorldName());
        if (world == null) return null;

        if (!isPassable(world, start) || !isPassable(world, end)) {
            return findNearestPassablePoint(start, end);
        }

        if (checkLineOfSight && hasLineOfSight(start, end)) {
            List<PathPoint> directPath = Collections.synchronizedList(new ArrayList<>());
            directPath.add(start);
            directPath.add(end);
            return directPath;
        }

        PriorityQueue<Node> openSet = new PriorityQueue<>(Comparator.comparingDouble(n -> n.fScore));
        ConcurrentHashMap<String, Node> nodeMap = new ConcurrentHashMap<>();
        Set<String> closedSet = Collections.synchronizedSet(new HashSet<>());

        Node startNode = new Node(start);
        startNode.gScore = 0;
        startNode.fScore = heuristic(start, end);
        openSet.add(startNode);
        nodeMap.put(pointToKey(start), startNode);

        int iterations = 0;
        int maxIterations = maxSearchDistance * 3;

        while (!openSet.isEmpty() && iterations < maxIterations) {
            iterations++;
            Node current = openSet.poll();

            if (current.point.equals(end) || current.point.distance2D(end) < 2) {
                return reconstructPath(current);
            }

            String currentKey = pointToKey(current.point);

            if (closedSet.contains(currentKey)) continue;

            closedSet.add(currentKey);

            for (PathPoint neighbor : getNeighbors(current.point)) {
                String neighborKey = pointToKey(neighbor);

                if (closedSet.contains(neighborKey)) continue;
                if (!isPassable(world, neighbor)) continue;
                if (checkLineOfSight && !hasLineOfSight(current.point, neighbor)) continue;

                double tentativeGScore = current.gScore + current.point.distance(neighbor);

                Node neighborNode = nodeMap.get(neighborKey);

                if (neighborNode == null) {
                    neighborNode = new Node(neighbor);
                    neighborNode.parent = current;
                    neighborNode.gScore = tentativeGScore;
                    neighborNode.fScore = tentativeGScore + heuristic(neighbor, end);

                    nodeMap.put(neighborKey, neighborNode);
                    openSet.add(neighborNode);
                } else if (tentativeGScore < neighborNode.gScore) {
                    neighborNode.parent = current;
                    neighborNode.gScore = tentativeGScore;
                    neighborNode.fScore = tentativeGScore + heuristic(neighbor, end);

                    openSet.remove(neighborNode);
                    openSet.add(neighborNode);
                }
            }

            if (closedSet.size() > maxSearchDistance) return findBestApproximatePath(nodeMap.values(), end);
        }

        return findBestApproximatePath(nodeMap.values(), end);
    }

    @NotNull
    private List<PathPoint> findBestApproximatePath(@NotNull Collection<Node> nodes, PathPoint destination) {
        Node bestNode = null;
        double bestDistance = Double.MAX_VALUE;

        for (Node node : nodes) {
            double distance = node.point.distance(destination);
            if (distance < bestDistance) {
                bestDistance = distance;
                bestNode = node;
            }
        }

        if (bestNode != null) {
            List<PathPoint> path = reconstructPath(bestNode);

            if (!path.isEmpty() && path.getLast().distance(destination) > 2) {
                path.add(destination);
            }

            return path;
        }

        List<PathPoint> fallBackPath = Collections.synchronizedList(new ArrayList<>());

        fallBackPath.add(new PathPoint(destination.getWorldName(),
                destination.getX(),
                destination.getY(),
                destination.getZ()));

        return fallBackPath;
    }

    @Nullable
    private List<PathPoint> findNearestPassablePoint(@NotNull PathPoint start, PathPoint end) {
        World world = org.bukkit.Bukkit.getWorld(start.getWorldName());
        if (world == null) return null;

        if (!isPassable(world, start)) {
            start = findPassablePointNear(start, world);
            if (start == null) return null;
        }

        if (!isPassable(world, end)) {
            PathPoint newEnd = findPassablePointNear(end, world);
            if (newEnd != null) end = newEnd;
        }

        List<PathPoint> directPath = Collections.synchronizedList(new ArrayList<>());
        directPath.add(start);
        directPath.add(end);
        return directPath;
    }

    @Nullable
    private PathPoint findPassablePointNear(PathPoint point, World world) {
        for (int dx = -3; dx <= 3; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -3; dz <= 3; dz++) {
                    PathPoint testPoint = new PathPoint(
                            point.getWorldName(),
                            point.getX() + dx,
                            point.getY() + dy,
                            point.getZ() + dz
                    );

                    if (isPassable(world, testPoint)) {
                        return testPoint;
                    }
                }
            }
        }

        return null;
    }

    private static class Node {
        PathPoint point;
        Node parent;
        double gScore = Double.MAX_VALUE;
        double fScore = Double.MAX_VALUE;

        Node(PathPoint point) {
            this.point = point;
        }
    }

    @NotNull
    private List<PathPoint> reconstructPath(Node endNode) {
        List<PathPoint> path = Collections.synchronizedList(new ArrayList<>());

        for (Node current = endNode; current != null; current = current.parent) {
            path.add(current.point);
        }

        Collections.reverse(path);

        return optimizePath(path);
    }

    @NotNull
    private List<PathPoint> optimizePath(@NotNull List<PathPoint> path) {
        if (path.size() <= 2) return path;

        List<PathPoint> optimized = Collections.synchronizedList(new ArrayList<>());
        optimized.add(path.getFirst());

        int index = 0;
        while (index < path.size() - 1) {
            PathPoint current = path.get(index);

            int farthestVisible = index + 1;
            for (int i = index + 2; i < path.size(); i++) {
                if (hasLineOfSight(current, path.get(i))) farthestVisible = i;
            }

            optimized.add(path.get(farthestVisible));
            index = farthestVisible;
        }

        return optimized;
    }

    private String pointToKey(@NotNull PathPoint point) {
        return point.getWorldName() + ":" + point.getX() + ":" + point.getY() + ":" + point.getZ();
    }

    private double heuristic(@NotNull PathPoint a, PathPoint b) {
        return a.distance(b);
    }

    private boolean isPassable(@NotNull World world, @NotNull PathPoint point) {
        Block block = world.getBlockAt((int) point.getX(), (int) point.getY(), (int) point.getZ());
        Block above = world.getBlockAt((int) point.getX(), (int) point.getY() + 1, (int) point.getZ());
        Block below = world.getBlockAt((int) point.getX(), (int) point.getY() - 1, (int) point.getZ());

        boolean blockPassable = passableMaterials.contains(block.getType());
        boolean abovePassable = passableMaterials.contains(above.getType());
        boolean belowSolid = !passableMaterials.contains(below.getType()) || below.getType() == Material.WATER;

        return blockPassable && abovePassable && belowSolid;
    }

    private boolean hasLineOfSight(@NotNull PathPoint a, PathPoint b) {
        World world = Bukkit.getWorld(a.getWorldName());
        if (world == null || !a.getWorldName().equals(b.getWorldName())) return false;

        double distance = a.distance(b);
        if (distance > 20) return false;

        double dx = b.getX() - a.getX();
        double dy = b.getY() - a.getY();
        double dz = b.getZ() - a.getZ();

        int steps = (int) (distance * 2);

        for (int i = 1; i < steps; i++) {
            double t = (double) i / steps;
            double x = a.getX() + (dx * t);
            double y = a.getY() + (dy * t);
            double z = a.getZ() + (dz * t);

            Block block = world.getBlockAt((int) x, (int) y, (int) z);

            if (!passableMaterials.contains(block.getType())) return false;
        }

        return true;
    }

    @NotNull
    private List<PathPoint> getNeighbors(@NotNull PathPoint point) {
        List<PathPoint> neighbors = Collections.synchronizedList(new ArrayList<>());

        neighbors.add(new PathPoint(point.getWorldName(), point.getX() + 1, point.getY(), point.getZ()));
        neighbors.add(new PathPoint(point.getWorldName(), point.getX() - 1, point.getY(), point.getZ()));
        neighbors.add(new PathPoint(point.getWorldName(), point.getX(), point.getY(), point.getZ() + 1));
        neighbors.add(new PathPoint(point.getWorldName(), point.getX(), point.getY(), point.getZ() - 1));

        if (allowDiagonal) {
            neighbors.add(new PathPoint(point.getWorldName(), point.getX() + 1, point.getY(), point.getZ() + 1));
            neighbors.add(new PathPoint(point.getWorldName(), point.getX() + 1, point.getY(), point.getZ() - 1));
            neighbors.add(new PathPoint(point.getWorldName(), point.getX() - 1, point.getY(), point.getZ() + 1));
            neighbors.add(new PathPoint(point.getWorldName(), point.getX() - 1, point.getY(), point.getZ() - 1));
        }

        neighbors.add(new PathPoint(point.getWorldName(), point.getX(), point.getY() + 1, point.getZ()));
        neighbors.add(new PathPoint(point.getWorldName(), point.getX(), point.getY() - 1, point.getZ()));

        return neighbors;
    }
}