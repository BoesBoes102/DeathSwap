package com.boes.deathswap.listeners;

import com.boes.deathswap.gamelogic.Game;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public record GameInteractionListener(Game game) implements Listener {

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (shouldCancel(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (shouldCancel(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (shouldCancel(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    private boolean shouldCancel(Player player) {
        if (!game.isRunning()) return false;
        if (!player.getWorld().equals(game.getGameWorld())) return false;
        
        return !game.getParticipatingPlayers().contains(player.getUniqueId());
    }
}
