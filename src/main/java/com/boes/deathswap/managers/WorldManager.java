package com.boes.deathswap.managers;

import com.boes.deathswap.DeathSwap;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.Location;
import org.bukkit.Chunk;
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

    public WorldManager(DeathSwap plugin) {
        this.plugin = plugin;
    }

    public void ensurePregeneratedWorld() {
        cleanupOldWorlds();
        if (currentWorldName == null) {
            generatePregeneratedWorld();
        } else {
            loadPotentialSpawns();
        }
    }

    public void cleanupOldWorlds() {
        File container = Bukkit.getWorldContainer();
        File[] files = container.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory() && file.getName().startsWith(WORLD_PREFIX)) {
                if (currentWorldName != null && file.getName().equals(currentWorldName)) continue;

                World world = Bukkit.getWorld(file.getName());
                if (world != null) {
                    Bukkit.unloadWorld(world, false);
                }
                
                deleteRecursive(file);
                plugin.getLogger().info("Cleaned up old world: " + file.getName());
            }
        }
    }

    public void generatePregeneratedWorld() {
        currentWorldName = WORLD_PREFIX + System.currentTimeMillis();
        plugin.getLogger().info("Generating new world: " + currentWorldName);
        Bukkit.createWorld(new WorldCreator(currentWorldName));
        loadPotentialSpawns();
    }

    private void loadPotentialSpawns() {
        World world = Bukkit.getWorld(currentWorldName);
        if (world == null) return;

        potentialSpawns.clear();
        Random random = new Random();
        int radius = plugin.getConfigManager().getRadius();
        
        plugin.getLogger().info("Pre-loading " + PRELOAD_COUNT + " chunks for spawn locations...");
        
        for (int i = 0; i < PRELOAD_COUNT; i++) {
            int x = random.nextInt(radius * 2) - radius;
            int z = random.nextInt(radius * 2) - radius;

            Chunk chunk = world.getChunkAt(x >> 4, z >> 4);
            chunk.load(true);
            
            Location loc = new Location(world, x, 0, z);

            world.setChunkForceLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4, true);

            int y = world.getHighestBlockYAt(x, z);
            loc.setY(y + 1);
            
            potentialSpawns.add(loc);
        }
        plugin.getLogger().info("Finished pre-loading spawn chunks.");
    }

    public List<Location> getPotentialSpawns() {
        return potentialSpawns;
    }

    public void unforceAllChunks() {
        if (currentWorldName == null) return;
        World world = Bukkit.getWorld(currentWorldName);
        if (world == null) return;

        for (Location loc : potentialSpawns) {
            world.setChunkForceLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4, false);
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
                p.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
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
                    plugin.getLogger().warning("Failed to delete world " + worldName + " (will be cleaned up on next start): " + e.getMessage());
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