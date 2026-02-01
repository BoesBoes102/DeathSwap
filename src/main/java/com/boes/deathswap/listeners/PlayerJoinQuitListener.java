package com.boes.deathswap.listeners;

import com.boes.deathswap.gamelogic.Game;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public record PlayerJoinQuitListener(Game game) implements Listener {

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        game.onPlayerLeave(player);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (!game.handlePlayerRejoin(player)) {
            if (player.getWorld().equals(game.getGameWorld())) {
            }
        }
    }
}