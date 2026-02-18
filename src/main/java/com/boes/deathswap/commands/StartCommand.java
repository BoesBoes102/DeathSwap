package com.boes.deathswap.commands;

import com.boes.deathswap.DeathSwap;

public record StartCommand(DeathSwap plugin) {

    public void execute() {
        plugin.getGame().loadSettings();
        plugin.getGameStarter().start();
        plugin.getLogger().info("DeathSwap game has been started successfully.");

    }
}
