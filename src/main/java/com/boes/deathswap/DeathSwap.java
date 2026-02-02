package com.boes.deathswap;

import com.boes.deathswap.commands.CommandManager;
import com.boes.deathswap.managers.ConfigManager;
import com.boes.deathswap.gamelogic.Game;
import com.boes.deathswap.gamelogic.Start;
import com.boes.deathswap.gamelogic.Stop;
import com.boes.deathswap.listeners.PVPListener;
import com.boes.deathswap.listeners.PlayerDeathListener;
import com.boes.deathswap.listeners.PlayerJoinQuitListener;
import com.boes.deathswap.listeners.PlayerRespawnListener;
import com.boes.deathswap.managers.WorldManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public class DeathSwap extends JavaPlugin {

    private Game game;
    private Start gameStarter;
    private Stop gameStopper;
    private ConfigManager configManager;
    private WorldManager worldManager;

    @Override
    public void onEnable() {

        configManager = new ConfigManager(this);
        worldManager = new WorldManager(this);

        game = new Game(this);
        game.loadSettings();
        
        gameStarter = new Start(this, game);
        gameStopper = new Stop(this, game);

        Bukkit.getPluginManager().registerEvents(new PlayerDeathListener(game), this);
        Bukkit.getPluginManager().registerEvents(new PlayerRespawnListener(game), this);
        Bukkit.getPluginManager().registerEvents(new PVPListener(game), this);
        Bukkit.getPluginManager().registerEvents(new PlayerJoinQuitListener(game), this);

        Objects.requireNonNull(getCommand("deathswap")).setExecutor(new CommandManager(this));
        Objects.requireNonNull(getCommand("deathswap")).setTabCompleter(new CommandManager(this));

        worldManager.ensurePregeneratedWorld();

        getLogger().info("DeathSwap enabled.");
    }

    @Override
    public void onDisable() {
        if (game != null && game.isRunning()) {
            gameStopper.stop();
        }
        if (worldManager != null) {
            worldManager.unforceAllChunks();
        }
        getLogger().info("DeathSwap disabled.");
    }

    public Game getGame() {
        return game;
    }

    public Start getGameStarter() {
        return gameStarter;
    }

    public Stop getGameStopper() {
        return gameStopper;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public WorldManager getWorldManager() {
        return worldManager;
    }
}
