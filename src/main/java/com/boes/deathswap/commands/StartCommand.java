package com.boes.deathswap.commands;

import com.boes.deathswap.DeathSwap;
import org.bukkit.command.CommandSender;

public record StartCommand(DeathSwap plugin) {

    public void execute(CommandSender sender) {
        plugin.getGame().loadSettings();
        plugin.getGameStarter().start();
        plugin.getLogger().info("DeathSwap game has been started successfully.");

    }
}
