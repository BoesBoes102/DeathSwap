package com.boes.deathswap.commands;

import com.boes.deathswap.DeathSwap;
import org.bukkit.command.CommandSender;

public record StopCommand(DeathSwap plugin) {

    public void execute(CommandSender sender) {
        if (plugin.getGameStopper().stop()) {
            sender.sendMessage("§cDeathSwap stopped.");
        } else {
            sender.sendMessage("§cNo game is currently running.");
        }
    }
}
