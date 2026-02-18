package com.boes.deathswap.commands;

import com.boes.deathswap.DeathSwap;
import com.boes.deathswap.gamelogic.Game;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public record SpectateCommand(DeathSwap plugin) {

    public void execute(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be executed by a player.");
            return;
        }

        if (!plugin.getGame().isRunning()) {
            player.sendMessage(ChatColor.RED + "No game is currently running.");
            return;
        }

        if (player.getWorld().equals(plugin.getGame().getGameWorld())) {
            player.sendMessage(ChatColor.RED + "You are already in the game world.");
            return;
        }

        Game.PlayerSnapshot snapshot = new Game.PlayerSnapshot(player);

        player.setGameMode(GameMode.SPECTATOR);
        player.teleport(plugin.getGame().getGameWorld().getSpawnLocation());
        plugin.getGame().addSpectator(player, snapshot);
        
        player.sendMessage(ChatColor.AQUA + "You are now spectating the DeathSwap game.");
        plugin.getLogger().info(player.getName() + " is now spectating.");
    }
}