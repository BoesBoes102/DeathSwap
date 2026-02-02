package com.boes.deathswap.gamelogic;

import com.boes.deathswap.DeathSwap;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public record Start(DeathSwap plugin, Game game) {

    public void start() {
        if (game.isRunning()) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.hasPermission("deathswap.admin")) {
                    p.sendMessage("§cA DeathSwap game is already running!");
                }
            }
            return;
        }

        for (Player p : Bukkit.getOnlinePlayers()) {
            game.getSnapshots().put(p, new Game.PlayerSnapshot(p));
        }

        World worldToUse = plugin.getWorldManager().getPregeneratedWorld();
        if (worldToUse == null) {
            plugin.getLogger().warning("No pregenerated world found! Attempting to generate one...");
            plugin.getWorldManager().generatePregeneratedWorld();
            worldToUse = plugin.getWorldManager().getPregeneratedWorld();
        }

        if (worldToUse == null) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.sendMessage("§cFailed to start game: Pregenerated world could not be loaded.");
            }
            return;
        }

        final World world = worldToUse;
        game.setGameWorld(world);
        game.setRunning(true);

        final Map<Player, Location> spawnLocations = new HashMap<>();
        List<Location> potentialSpawns = new ArrayList<>(plugin.getWorldManager().getPotentialSpawns());
        Random random = new Random();
        
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (potentialSpawns.isEmpty()) {
                int radius = plugin.getConfigManager().getRadius();
                int x = random.nextInt(radius * 2) - radius;
                int z = random.nextInt(radius * 2) - radius;
                int y = world.getHighestBlockYAt(x, z) + 1;
                spawnLocations.put(p, new Location(world, x, y, z));
            } else {
                int index = random.nextInt(potentialSpawns.size());
                spawnLocations.put(p, potentialSpawns.remove(index));
            }
        }

        new BukkitRunnable() {
            int countdown = 10;

            @Override
            public void run() {
                if (countdown > 0) {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.sendTitle("§c" + countdown, "§eStarting DeathSwap...", 5, 20, 5);
                    }
                    countdown--;
                } else {
                    cancel();

                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.getInventory().clear();
                        p.setGameMode(GameMode.SURVIVAL);

                        Location spawnLoc = spawnLocations.get(p);
                        if (spawnLoc == null) {
                            int radius = plugin.getConfigManager().getRadius();
                            int x = random.nextInt(radius * 2) - radius;
                            int z = random.nextInt(radius * 2) - radius;
                            int y = world.getHighestBlockYAt(x, z) + 1;
                            spawnLoc = new Location(world, x, y, z);
                        }

                        p.teleport(spawnLoc);
                        p.setHealth(20);
                        p.setFoodLevel(20);
                        p.setSaturation(20f);

                        p.getInventory().addItem(new ItemStack(Material.COOKED_BEEF, 16));
                        p.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 0, false, false));

                        p.sendMessage("§aDeathSwap has started!");
                        p.sendMessage("§7Survive as long as you can and try to trap other players!");
                        p.sendMessage("§7Every " + game.getCooldownSeconds() + " seconds, you’ll swap positions with another player!");
                        p.sendMessage("§7PVP and Nether are disabled. The game lasts for " + game.formatTime(game.getTotalTimeSeconds()) + ".");
                    }

                    game.startLoop();
                }
            }
        }.runTaskTimer(plugin, 0, 20);
    }
}