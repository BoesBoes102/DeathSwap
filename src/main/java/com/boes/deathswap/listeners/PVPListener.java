package com.boes.deathswap.listeners;

import com.boes.deathswap.gamelogic.Game;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public record PVPListener(Game game) implements Listener {

    @EventHandler
    public void onPvp(EntityDamageByEntityEvent event) {
        if (!game.isRunning()) return;
        if (!(event.getEntity() instanceof Player) || !(event.getDamager() instanceof Player)) return;
        if (!event.getEntity().getWorld().equals(game.getGameWorld())) return;

        event.setCancelled(true);
    }
}
