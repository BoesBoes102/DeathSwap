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

        if (event.getEntity() instanceof Player victim) {
            if (!game.getParticipatingPlayers().contains(victim.getUniqueId()) && victim.getWorld().equals(game.getGameWorld())) {
                event.setCancelled(true);
                return;
            }
        }

        if (!(event.getEntity() instanceof Player victim) || !(event.getDamager() instanceof Player attacker)) return;

        if (!game.getParticipatingPlayers().contains(victim.getUniqueId()) || 
            !game.getParticipatingPlayers().contains(attacker.getUniqueId())) {
            if (victim.getWorld().equals(game.getGameWorld())) {
                event.setCancelled(true);
            }
            return;
        }

        if (!victim.getWorld().equals(game.getGameWorld())) return;

        event.setCancelled(true);
    }
}
