package com.boes.deathswap.gamelogic;

import com.boes.deathswap.DeathSwap;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.Player;

public record Stop(DeathSwap plugin, Game game) {

    public boolean stop() {
        if (!game.isRunning()) return false;
        game.stopGame();

        World gameWorld = game.getGameWorld();
        if (gameWorld == null) return true;

        for (Player p : Bukkit.getOnlinePlayers()) {
            // Ensure they are alive so teleport works
            if (p.isDead()) {
                p.spigot().respawn();
            }

            Game.PlayerSnapshot snapshot = game.getSnapshots().get(p);
            if (snapshot == null) {
                snapshot = game.getSpectatorSnapshots().get(p);
            }

            if (snapshot != null) {
                snapshot.restore();
            }

            // Fallback for players still in the game world
            if (p.getWorld().equals(gameWorld)) {
                p.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
                if (p.getGameMode() == GameMode.SPECTATOR) {
                    p.setGameMode(GameMode.SURVIVAL);
                }
            }
        }

        String worldName = gameWorld.getName();

        game.getSnapshots().clear();
        game.getSpectatorSnapshots().clear();

        String worldToDelete = worldName;
        plugin.getWorldManager().deleteWorld(worldToDelete);

        game.setGameWorld(null);
        return true;
    }
}
