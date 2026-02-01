package com.boes.deathswap.commands;

import com.boes.deathswap.DeathSwap;
import org.bukkit.command.CommandSender;

public record ReloadCommand(DeathSwap plugin) {

    public void execute(CommandSender sender) {
        try {
            plugin.getConfigManager().reloadConfig();
            sender.sendMessage("§aConfiguration reloaded successfully.");
        } catch (Exception e) {
            sender.sendMessage("§cFailed to reload configuration: " + e.getMessage());
            plugin.getLogger().severe("Failed to reload config: " + e.getMessage());
        }
    }
}