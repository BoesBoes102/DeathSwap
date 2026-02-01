package com.boes.deathswap.listeners;

import com.boes.deathswap.gamelogic.Game;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public record PlayerDeathListener(Game game) implements Listener {

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        if (!game.isRunning()) return;
        if (!player.getWorld().equals(game.getGameWorld())) return;

        Bukkit.getScheduler().runTaskLater(game.plugin, () -> game.onPlayerDeath(player), 1L);
    }
}
