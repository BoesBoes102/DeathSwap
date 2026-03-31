package com.boes.deathswap.gamelogic;

import com.boes.deathswap.DeathSwap;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class Start {
    private final DeathSwap plugin;
    private final Game game;
    private BukkitRunnable countdownTask;

    public Start(DeathSwap plugin, Game game) {
        this.plugin = plugin;
        this.game = game;
    }

    public void start() {
        if (game.isRunning()) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.hasPermission("deathswap.admin")) {
                    p.sendMessage("§cA DeathSwap game is already running!");
                }
            }
            return;
        }

        if (Bukkit.getOnlinePlayers().size() < 2) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.hasPermission("deathswap.admin")) {
                    p.sendMessage("§cNot enough players to start! You need at least 2 players.");
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
            Location spawnLoc;
            if (potentialSpawns.isEmpty()) {
                int radius = plugin.getConfigManager().getRadius();
                spawnLoc = findSafeSpawnLocation(world, radius, random);
            } else {
                int index = random.nextInt(potentialSpawns.size());
                spawnLoc = potentialSpawns.remove(index);
                while (!isSafeSpawnLocation(spawnLoc) && !potentialSpawns.isEmpty()) {
                    index = random.nextInt(potentialSpawns.size());
                    spawnLoc = potentialSpawns.remove(index);
                }
            }
            spawnLocations.put(p, spawnLoc);
        }

        plugin.getLogger().info("DeathSwap game has been started successfully.");

        for (Location loc : spawnLocations.values()) {
            int chunkX = loc.getBlockX() >> 4;
            int chunkZ = loc.getBlockZ() >> 4;
            world.getChunkAt(chunkX, chunkZ).load(true);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx != 0 || dz != 0) {
                        world.getChunkAtAsync(chunkX + dx, chunkZ + dz);
                    }
                }
            }
        }

        countdownTask = new BukkitRunnable() {
            int countdown = 10;

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
                    countdownTask = null;
                    startTeleportSequence(playersToTeleport, spawnLocations, world);
                }
            }
        };
        countdownTask.runTaskTimer(plugin, 0, 20);
    }

    private void startTeleportSequence(List<Player> playersToTeleport, Map<Player, Location> spawnLocations, World world) {
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
                        p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 100, 0, false, false));

                        Location spawnLoc = spawnLocations.get(p);
                        p.teleport(spawnLoc);
                        p.sendMessage("§eTeleporting... Please wait for other players.");
                    }
                    playerIndex++;
                } else {
                    cancel();
                    startBlindnessCountdown(playersToTeleport);
                }
            }
        }.runTaskTimer(plugin, 0, 4);
    }

    private void startBlindnessCountdown(List<Player> playersToTeleport) {
        new BukkitRunnable() {
            int countdown = 5;

            @Override
            public void run() {
                if (countdown > 0) {
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
                            p.sendMessage("§7Every " + game.getCooldownSeconds() + " seconds, you'll swap positions with another player!");
                        }
                    }
                    game.startLoop();
                }
            }
        }.runTaskTimer(plugin, 100, 1);
    }

    public void cancelCountdown() {
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
    }

    private Location findSafeSpawnLocation(World world, int radius, Random random) {
        int maxAttempts = 50;
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            int x = random.nextInt(radius * 2) - radius;
            int z = random.nextInt(radius * 2) - radius;
            int y = world.getHighestBlockYAt(x, z) + 1;
            Location loc = new Location(world, x, y, z);
            if (isSafeSpawnLocation(loc)) {
                return loc;
            }
        }

        int x = random.nextInt(radius * 2) - radius;
        int z = random.nextInt(radius * 2) - radius;
        int y = world.getHighestBlockYAt(x, z) + 1;
        return new Location(world, x, y, z);
    }

    private boolean isSafeSpawnLocation(Location loc) {
        if (loc == null || loc.getWorld() == null) {
            return false;
        }

         Block block = loc.getBlock();
        if (block.isLiquid()) {
            return false;
        }

        Block below = loc.clone().subtract(0, 1, 0).getBlock();
        if (below.isLiquid() || below.getType() == Material.AIR) {
            return false;
        }

        Block above = loc.clone().add(0, 1, 0).getBlock();
        if (above.isLiquid()) {
            return false;
        }
        
        return true;
    }
}