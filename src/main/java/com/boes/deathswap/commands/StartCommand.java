package com.boes.deathswap.commands;

import com.boes.deathswap.DeathSwap;
import com.boes.deathswap.gamelogic.Start;

public record StartCommand(DeathSwap plugin) {

    public void execute() {
        plugin.getGame().loadSettings();
        Start starter = new Start(plugin, plugin.getGame());
        plugin.getGame().setGameStarter(starter);
        starter.start();

    }
}
