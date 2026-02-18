package com.boes.deathswap.managers;

import com.boes.deathswap.DeathSwap;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class WorldManager {
    private final DeathSwap plugin;
    private static final String WORLD_PREFIX = "deathswap_";
    private String currentWorldName = null;
    private final List<Location> potentialSpawns = new ArrayList<>();
    private static final int PRELOAD_COUNT = 20;
    private boolean isLoadingSpawns = false;

    public WorldManager(DeathSwap plugin) {
        this.plugin = plugin;
    }

    public void ensurePregeneratedWorld() {
        new BukkitRunnable() {
            @Override
            public void run() {
                File container = Bukkit.getWorldContainer();
                File[] files = container.listFiles();
                File newestWorld = null;
                long lastModified = 0;

                if (files != null) {
                    for (File file : files) {
                        if (file.isDirectory() && file.getName().startsWith(WORLD_PREFIX)) {
                            if (file.lastModified() > lastModified) {
                                lastModified = file.lastModified();
                                newestWorld = file;
                            }
                        }
                    }
                }

                final String foundWorldName = (newestWorld != null) ? newestWorld.getName() : null;

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (foundWorldName != null) {
                            currentWorldName = foundWorldName;
                            plugin.getLogger().info("Found existing world: " + currentWorldName);
                            Bukkit.createWorld(new WorldCreator(currentWorldName));
                            loadPotentialSpawns();
                        } else {
                            generatePregeneratedWorld();
                        }
                        cleanupOldWorlds();
                    }
                }.runTask(plugin);
            }
        }.runTaskAsynchronously(plugin);
    }

    public void cleanupOldWorlds() {
        new BukkitRunnable() {
            @Override
            public void run() {
                File container = Bukkit.getWorldContainer();
                File[] files = container.listFiles();
                if (files == null) return;

                for (File file : files) {
                    if (file.isDirectory() && file.getName().startsWith(WORLD_PREFIX)) {
                        if (file.getName().equals(currentWorldName)) continue;

                        final String nameToDelete = file.getName();

                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                World world = Bukkit.getWorld(nameToDelete);
                                if (world != null) {
                                    Bukkit.unloadWorld(world, false);
                                }

                                new BukkitRunnable() {
                                    @Override
                                    public void run() {
                                        File worldDir = new File(Bukkit.getWorldContainer(), nameToDelete);
                                        if (worldDir.exists()) {
                                            deleteRecursive(worldDir);
                                            plugin.getLogger().info("Cleaned up old world: " + nameToDelete);
                                        }
                                    }
                                }.runTaskAsynchronously(plugin);
                            }
                        }.runTask(plugin);
                    }
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    public void generatePregeneratedWorld() {
        currentWorldName = WORLD_PREFIX + System.currentTimeMillis();
        plugin.getLogger().info("Generating new world: " + currentWorldName);
        Bukkit.createWorld(new WorldCreator(currentWorldName));
        loadPotentialSpawns();
    }

    private void loadPotentialSpawns() {
        if (isLoadingSpawns) return;

        World world = Bukkit.getWorld(currentWorldName);
        if (world == null) return;

        isLoadingSpawns = true;
        potentialSpawns.clear();
        Random random = new Random();
        int radius = plugin.getConfigManager().getRadius();


        for (int i = 0; i < PRELOAD_COUNT; i++) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    int x = random.nextInt(radius * 2) - radius;
                    int z = random.nextInt(radius * 2) - radius;

                    world.getChunkAtAsync(x >> 4, z >> 4).thenAccept(chunk -> Bukkit.getScheduler().runTask(plugin, () -> {
                        for (int dx = -1; dx <= 1; dx++) {
                            for (int dz = -1; dz <= 1; dz++) {
                                world.setChunkForceLoaded((x >> 4) + dx, (z >> 4) + dz, true);
                            }
                        }
                        Location loc = new Location(world, x, 0, z);
                        int y = world.getHighestBlockYAt(x, z);
                        loc.setY(y + 1);
                        potentialSpawns.add(loc);

                        if (potentialSpawns.size() == PRELOAD_COUNT) {
                            plugin.getLogger().info("World is ready to play!");
                            isLoadingSpawns = false;
                        }
                    }));
                }
            }.runTaskLater(plugin, i * 2L);
        }
    }

    public List<Location> getPotentialSpawns() {
        return potentialSpawns;
    }

    public void unforceAllChunks() {
        if (currentWorldName == null) return;
        World world = Bukkit.getWorld(currentWorldName);
        if (world == null) return;

        for (Location loc : potentialSpawns) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    world.setChunkForceLoaded((loc.getBlockX() >> 4) + dx, (loc.getBlockZ() >> 4) + dz, false);
                }
            }
        }
    }

    public void deleteWorld(String worldName) {
        if (worldName.equals(currentWorldName)) {
            unforceAllChunks();
            potentialSpawns.clear();
        }
        World world = Bukkit.getWorld(worldName);
        if (world != null) {
            world.setAutoSave(false);
            for (org.bukkit.entity.Player p : world.getPlayers()) {
                p.teleport(Bukkit.getWorlds().getFirst().getSpawnLocation());
            }
            Bukkit.unloadWorld(world, false);
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(2000);
                    File worldDir = new File(Bukkit.getWorldContainer(), worldName);
                    if (worldDir.exists()) {
                        deleteRecursive(worldDir);
                        plugin.getLogger().info("World deleted: " + worldName);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to delete world " + worldName);
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    public void deleteWorldAndRegenerate(String worldName) {
        deleteWorld(worldName);
        new BukkitRunnable() {
            @Override
            public void run() {
                generatePregeneratedWorld();
            }
        }.runTaskLater(plugin, 40L);
    }

    private void deleteRecursive(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursive(child);
                }
            }
        }
        file.delete();
    }

    public World getPregeneratedWorld() {
        if (currentWorldName == null || Bukkit.getWorld(currentWorldName) == null) {
            generatePregeneratedWorld();
        }
        return Bukkit.getWorld(currentWorldName);
    }
}