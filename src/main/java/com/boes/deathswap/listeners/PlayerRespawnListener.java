package com.boes.deathswap.listeners;

import com.boes.deathswap.gamelogic.Game;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;

public record PlayerRespawnListener(Game game) implements Listener {

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (!game.isRunning()) return;
        if (game.getGameWorld() == null) return;

        // If the player is part of the game (as spectator or otherwise)
        if (game.isSpectating(event.getPlayer()) || game.getSnapshots().containsKey(event.getPlayer())) {
            event.setRespawnLocation(game.getGameWorld().getSpawnLocation());
        }
    }
}
