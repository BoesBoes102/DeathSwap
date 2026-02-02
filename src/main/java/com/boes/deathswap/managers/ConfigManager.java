package com.boes.deathswap.managers;

import com.boes.deathswap.DeathSwap;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class ConfigManager {

    private final DeathSwap plugin;
    private FileConfiguration config;
    private File configFile;

    public ConfigManager(DeathSwap plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        configFile = new File(plugin.getDataFolder(), "config.yml");

        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }

        config = YamlConfiguration.loadConfiguration(configFile);
    }

    public void reloadConfig() {
        loadConfig();
        plugin.getGame().loadSettings();
        plugin.getLogger().info("Configuration reloaded successfully.");
    }

    public int getTotalTimeSeconds() {
        return config.getInt("game.total-time-seconds", 300);
    }

    public int getCooldownSeconds() {
        return config.getInt("game.cooldown-seconds", 30);
    }

    public int getRadius() {
        return config.getInt("game.radius", 5000);
    }

    public FileConfiguration getConfig() {
        return config;
    }
}