package com.boes.deathswap.commands;

import com.boes.deathswap.DeathSwap;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CommandManager implements CommandExecutor, TabCompleter {

    private final StartCommand startCommand;
    private final StopCommand stopCommand;
    private final ReloadCommand reloadCommand;
    private final SpectateCommand spectateCommand;
    private final LeaveCommand leaveCommand;

    public CommandManager(DeathSwap plugin) {
        this.startCommand = new StartCommand(plugin);
        this.stopCommand = new StopCommand(plugin);
        this.reloadCommand = new ReloadCommand(plugin);
        this.spectateCommand = new SpectateCommand(plugin);
        this.leaveCommand = new LeaveCommand(plugin);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§cUsage: /deathswap <start|stop|reload|spectate|leave>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "start" -> {
                if (!sender.hasPermission("deathswap.admin")) {
                    sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
                    return true;
                }
                startCommand.execute();
            }
            case "stop" -> {
                if (!sender.hasPermission("deathswap.admin")) {
                    sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
                    return true;
                }
                stopCommand.execute(sender);
            }
            case "reload" -> {
                if (!sender.hasPermission("deathswap.admin")) {
                    sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
                    return true;
                }
                reloadCommand.execute(sender);
            }
            case "spectate" -> spectateCommand.execute(sender);
            case "leave" -> leaveCommand.execute(sender);
            default -> sender.sendMessage("§cUnknown subcommand.");
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = Arrays.asList("start", "stop", "reload", "spectate", "leave");
            return completions.stream()
                    .filter(c -> c.startsWith(args[0].toLowerCase()))
                    .toList();
        }
        return Collections.emptyList();
    }
}
