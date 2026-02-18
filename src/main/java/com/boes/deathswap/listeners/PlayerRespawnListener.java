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

        if (game.isSpectating(event.getPlayer()) || game.getParticipatingPlayers().contains(event.getPlayer().getUniqueId())) {
            event.setRespawnLocation(game.getGameWorld().getSpawnLocation());
        }
    }
}
