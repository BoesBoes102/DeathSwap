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
            game.getSnapshots().put(p.getUniqueId(), new Game.PlayerSnapshot(p));
            game.getParticipatingPlayers().add(p.getUniqueId());
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
        game.setStarting(true);

        world.setTime(1000);

        final List<Player> playersToTeleport = new ArrayList<>(Bukkit.getOnlinePlayers());
        final Map<Player, Location> spawnLocations = new HashMap<>();
        List<Location> potentialSpawns = new ArrayList<>(plugin.getWorldManager().getPotentialSpawns());
        Random random = new Random();

        for (Player p : playersToTeleport) {
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
            int playerIndex = 0;

            @Override
            public void run() {
                if (playerIndex < playersToTeleport.size()) {
                    Player p = playersToTeleport.get(playerIndex);
                    if (p.isOnline()) {
                        for (PotionEffect effect : p.getActivePotionEffects()) {
                            p.removePotionEffect(effect.getType());
                        }
                        p.getInventory().clear();
                        p.setGameMode(GameMode.SURVIVAL);
                        p.setHealth(20);
                        p.setFoodLevel(20);
                        p.setSaturation(20f);
                        p.getInventory().addItem(new ItemStack(Material.COOKED_BEEF, 16));
                        p.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 0, false, false));
                        p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, Integer.MAX_VALUE, 0, false, false));

                        Location spawnLoc = spawnLocations.get(p);
                        p.teleport(spawnLoc);
                        p.sendMessage("§eTeleporting... Please wait for other players.");
                    }
                    playerIndex++;
                } else {
                    cancel();
                    startPostTeleportCountdown();
                }
            }

            private void startPostTeleportCountdown() {
                new BukkitRunnable() {
                    int countdown = 5;

                    @Override
                    public void run() {
                        if (countdown > 0) {
                            for (Player p : playersToTeleport) {
                                if (p.isOnline()) {
                                    p.sendTitle("§c" + countdown, "§eStarting soon...", 0, 21, 0);
                                }
                            }
                            countdown--;
                        } else {
                            cancel();
                            game.setStarting(false);
                            for (Player p : playersToTeleport) {
                                if (p.isOnline()) {
                                    p.removePotionEffect(PotionEffectType.BLINDNESS);
                                    p.sendTitle("§aGO!", "§eDeathSwap has started!", 5, 20, 5);
                                    p.sendMessage("§aDeathSwap has started!");
                                    p.sendMessage("§7Survive as long as you can and try to trap other players!");
                                    p.sendMessage("§7Every " + game.getCooldownSeconds() + " seconds, you’ll swap positions with another player!");
                                }
                            }
                            game.startLoop();
                        }
                    }
                }.runTaskTimer(plugin, 20, 20);
            }
        }.runTaskTimer(plugin, 0, 10);
    }
}