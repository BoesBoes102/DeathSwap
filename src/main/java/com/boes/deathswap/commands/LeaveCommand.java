package com.boes.deathswap.commands;

import com.boes.deathswap.DeathSwap;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public record LeaveCommand(DeathSwap plugin) {

    public void execute(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be executed by a player.");
            return;
        }

        if (!plugin.getGame().isRunning()) {
            player.sendMessage(ChatColor.RED + "No game is currently running.");
            return;
        }

        if (!player.getWorld().equals(plugin.getGame().getGameWorld())) {
            player.sendMessage(ChatColor.RED + "You are not in the game world.");
            return;
        }

        if (player.getGameMode() != GameMode.SPECTATOR) {
            player.sendMessage(ChatColor.RED + "You must be dead or spectating to leave.");
            return;
        }

        com.boes.deathswap.gamelogic.Game.PlayerSnapshot snapshot = plugin.getGame().getSnapshots().get(player.getUniqueId());
        com.boes.deathswap.gamelogic.Game.PlayerSnapshot spectatorSnapshot = plugin.getGame().getSpectatorSnapshots().get(player.getUniqueId());
        
        if (snapshot != null) {
            snapshot.restore(player);
            plugin.getGame().getSnapshots().remove(player.getUniqueId());
        } else if (spectatorSnapshot != null) {
            spectatorSnapshot.restore(player);
            plugin.getGame().getSpectatorSnapshots().remove(player.getUniqueId());
        } else {
            player.setGameMode(GameMode.SURVIVAL);
            player.teleport(player.getWorld().getSpawnLocation());
        }

        plugin.getGame().removeSpectator(player);
        player.sendMessage(ChatColor.YELLOW + "You have left the DeathSwap game.");
        plugin.getLogger().info(player.getName() + " left the game.");
    }
}