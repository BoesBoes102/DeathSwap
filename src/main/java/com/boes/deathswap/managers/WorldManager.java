package com.boes.deathswap.managers;

import com.boes.deathswap.DeathSwap;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;

public class WorldManager {
    private final DeathSwap plugin;
    private static final String WORLD_PREFIX = "deathswap_";
    private String currentWorldName = null;

    public WorldManager(DeathSwap plugin) {
        this.plugin = plugin;
    }

    public void ensurePregeneratedWorld() {
        cleanupOldWorlds();
        if (currentWorldName == null) {
            generatePregeneratedWorld();
        }
    }

    public void cleanupOldWorlds() {
        File container = Bukkit.getWorldContainer();
        File[] files = container.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory() && file.getName().startsWith(WORLD_PREFIX)) {
                // If it's the current world, skip it
                if (currentWorldName != null && file.getName().equals(currentWorldName)) continue;
                
                // If it's loaded, unload it first
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
    }

    public void deleteWorld(String worldName) {
        World world = Bukkit.getWorld(worldName);
        if (world != null) {
            world.setAutoSave(false);
            // Move players out before unloading
            for (org.bukkit.entity.Player p : world.getPlayers()) {
                p.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
            }
            Bukkit.unloadWorld(world, false);
        }

        // Run deletion asynchronously to avoid lag, but it will be picked up on next startup if it fails
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    // Give extra time for OS to release file locks
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
        }.runTaskLater(plugin, 40L); // Wait 2 seconds before generating next one
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