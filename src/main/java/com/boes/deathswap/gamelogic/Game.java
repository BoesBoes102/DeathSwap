package com.boes.deathswap.gamelogic;

import com.boes.deathswap.DeathSwap;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.UUID;

public class Game {

    public final DeathSwap plugin;
    private boolean running = false;
    private boolean starting = false;
    private boolean stopping = false;
    private World gameWorld;
    private final Map<UUID, PlayerSnapshot> snapshots = new HashMap<>();
    private final Map<UUID, PlayerSnapshot> spectatorSnapshots = new HashMap<>();
    private final Set<UUID> spectators = new HashSet<>();
    private final Set<UUID> participatingPlayers = new HashSet<>();
    
    private final Map<UUID, LeftPlayerData> leftPlayers = new HashMap<>();
    private static final int REJOIN_TIMEOUT_SECONDS = 60;

    private int totalTimeSeconds = 600;
    private int cooldownSeconds = 60;
    private BukkitRunnable timerTask;
    private BossBar bossBar;

    public Game(DeathSwap plugin) {
        this.plugin = plugin;
    }

    public int getCooldownSeconds() {
        return cooldownSeconds;
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public boolean isStarting() {
        return starting;
    }

    public void setStarting(boolean starting) {
        this.starting = starting;
    }

    public World getGameWorld() {
        return gameWorld;
    }

    public void setGameWorld(World gameWorld) {
        this.gameWorld = gameWorld;
    }

    public Map<UUID, PlayerSnapshot> getSnapshots() {
        return snapshots;
    }

    public Map<UUID, PlayerSnapshot> getSpectatorSnapshots() {
        return spectatorSnapshots;
    }

    public Set<UUID> getParticipatingPlayers() {
        return participatingPlayers;
    }

    public void loadSettings() {
        this.totalTimeSeconds = plugin.getConfigManager().getTotalTimeSeconds();
        this.cooldownSeconds = plugin.getConfigManager().getCooldownSeconds();
    }

    public void startLoop() {
        stopping = false;
        if (timerTask != null) timerTask.cancel();
        if (bossBar != null) bossBar.removeAll();

        bossBar = Bukkit.createBossBar(ChatColor.GOLD + "Time remaining: " + ChatColor.WHITE + formatTime(totalTimeSeconds), BarColor.YELLOW, BarStyle.SOLID);

        timerTask = new BukkitRunnable() {
            private int timeRemaining = totalTimeSeconds;
            private int swapCooldown = cooldownSeconds;

            @Override
            public void run() {
                if (!running || stopping) {
                    if (bossBar != null) {
                        bossBar.removeAll();
                        bossBar = null;
                    }
                    cancel();
                    return;
                }

                tickLeftPlayerTimeouts();

                bossBar.removeAll();
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (participatingPlayers.contains(p.getUniqueId()) || spectators.contains(p.getUniqueId())) {
                        bossBar.addPlayer(p);
                    }
                }

                bossBar.setTitle(ChatColor.GOLD + "Time remaining: " + ChatColor.WHITE + formatTime(timeRemaining));
                bossBar.setProgress(Math.max(0.0, Math.min(1.0, (double) timeRemaining / totalTimeSeconds)));

                List<Player> alive = getAlivePlayers();
                if (alive.size() <= 1) {
                    endGame(alive);
                    cancel();
                    return;
                }

                if (timeRemaining <= 0) {
                    endGame(alive);
                    cancel();
                    return;
                }

                if (swapCooldown <= 10 && swapCooldown > 0) {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        if (participatingPlayers.contains(p.getUniqueId()) || spectators.contains(p.getUniqueId())) {
                            p.sendTitle(ChatColor.RED + String.valueOf(swapCooldown), ChatColor.YELLOW + "Swapping in...", 0, 21, 0);
                        }
                    }
                }

                if (swapCooldown <= 0) {
                    executeSwap(alive);
                    swapCooldown = cooldownSeconds;
                } else {
                    swapCooldown--;
                }

                timeRemaining--;
            }
        };
        timerTask.runTaskTimer(plugin, 20, 20);
    }

    public void stopGame() {
        running = false;
        stopping = false;
        participatingPlayers.clear();
        restoreLeftPlayers();
        if (timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }
        if (bossBar != null) {
            bossBar.removeAll();
            bossBar = null;
        }
    }

    public void executeSwap(List<Player> alivePlayers) {
        if (alivePlayers.size() < 2) return;

        Map<Player, Location> swapData = new HashMap<>();
        for (int i = 0; i < alivePlayers.size(); i++) {
            Player from = alivePlayers.get(i);
            Player to = alivePlayers.get((i + 1) % alivePlayers.size());
            swapData.put(from, to.getLocation().clone());
        }

        for (Player player : alivePlayers) {
            Location loc = swapData.get(player);
            if (loc != null) {
                player.teleport(loc);
                player.sendTitle(ChatColor.RED + "SWAP!", "", 10, 40, 10);
            }
        }
    }

    public void onPlayerDeath(Player player) {
        if (!running || !participatingPlayers.contains(player.getUniqueId())) return;

        if (player.getGameMode() != GameMode.SPECTATOR) {
            player.setGameMode(GameMode.SPECTATOR);
            player.sendMessage(ChatColor.RED + "You died and are now a spectator.");
            addSpectator(player, null);
            
            if (!player.getWorld().equals(gameWorld) && gameWorld != null) {
                player.teleport(gameWorld.getSpawnLocation());
            }
        }

        List<Player> alive = getAlivePlayers();
        if (alive.size() <= 1) {
            endGame(alive);
        }
    }

    private void endGame(List<Player> alive) {
        if (stopping) return;
        stopping = true;
        
        String message;
        if (alive.size() == 1) {
            message = ChatColor.GREEN + "DeathSwap winner: " + alive.getFirst().getName();
        } else {
            message = ChatColor.YELLOW + "DeathSwap ended with no winner.";
        }

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getWorld().equals(gameWorld) || spectators.contains(p.getUniqueId())) {
                p.sendMessage(message);
            }
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                new Stop(plugin, Game.this).stop();
            }
        }.runTaskLater(plugin, 40L);
    }


    public List<Player> getAlivePlayers() {
        List<Player> alive = new ArrayList<>();
        if (gameWorld == null) return alive;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (participatingPlayers.contains(p.getUniqueId()) && p.getWorld().equals(gameWorld) && p.getGameMode() == GameMode.SURVIVAL && !p.isDead()) {
                alive.add(p);
            }
        }
        return alive;
    }

    public void addSpectator(Player player, PlayerSnapshot snapshot) {
        spectators.add(player.getUniqueId());
        if (snapshot != null) {
            spectatorSnapshots.put(player.getUniqueId(), snapshot);
        }
    }

    public void removeSpectator(Player player) {
        spectators.remove(player.getUniqueId());
        spectatorSnapshots.remove(player.getUniqueId());
    }

    public boolean isSpectating(Player player) {
        return spectators.contains(player.getUniqueId());
    }

    public void onPlayerLeave(Player player) {
        if (!running) return;
        
        PlayerSnapshot snapshot = new PlayerSnapshot(player);
        leftPlayers.put(player.getUniqueId(), new LeftPlayerData(snapshot, REJOIN_TIMEOUT_SECONDS));
    }

    public boolean handlePlayerRejoin(Player player) {
        LeftPlayerData data = leftPlayers.get(player.getUniqueId());
        if (data == null) return true;
        
        leftPlayers.remove(player.getUniqueId());
        
        if (!running) {
            data.snapshot.restore(player);
            return false;
        }

        return true;
    }

    public void restoreLeftPlayers() {
        for (LeftPlayerData data : leftPlayers.values()) {
            Player p = Bukkit.getPlayer(data.snapshot.uuid);
            if (p != null && p.isOnline()) {
                data.snapshot.restore(p);
            }
        }
        leftPlayers.clear();
    }

    private void tickLeftPlayerTimeouts() {
        List<UUID> timedOut = new ArrayList<>();
        
        for (Map.Entry<UUID, LeftPlayerData> entry : leftPlayers.entrySet()) {
            entry.getValue().secondsRemaining--;
            if (entry.getValue().secondsRemaining <= 0) {
                timedOut.add(entry.getKey());
            }
        }

        for (UUID uuid : timedOut) {
            LeftPlayerData data = leftPlayers.remove(uuid);
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                data.snapshot.restore(player);
            }
        }
    }

    public String formatTime(int totalSeconds) {
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        if (minutes > 0) {
            return minutes + " minute" + (minutes > 1 ? "s" : "") + (seconds > 0 ? " " + seconds + "s" : "");
        }
        return seconds + "s";
    }

    public static class PlayerSnapshot {
        public final UUID uuid;
        public final Location location;
        public final ItemStack[] contents;
        public final ItemStack[] armor;
        public final GameMode gamemode;
        public final double health;
        public final int food;
        public final float saturation;
        public final Collection<PotionEffect> effects;

        public PlayerSnapshot(Player p) {
            uuid = p.getUniqueId();
            location = p.getLocation();
            contents = p.getInventory().getContents();
            armor = p.getInventory().getArmorContents();
            gamemode = p.getGameMode();
            health = p.getHealth();
            food = p.getFoodLevel();
            saturation = p.getSaturation();
            effects = p.getActivePotionEffects();
        }

        public void restore(Player player) {
            player.getInventory().setContents(contents);
            player.getInventory().setArmorContents(armor);

            if (location.getWorld() != null) {
                location.getChunk().load();
            }
            
            player.teleport(location);
            player.setGameMode(gamemode);
            player.setHealth(health);
            player.setFoodLevel(food);
            player.setSaturation(saturation);
            
            for (PotionEffect effect : player.getActivePotionEffects()) {
                player.removePotionEffect(effect.getType());
            }
            player.addPotionEffects(effects);
        }
    }

    private static class LeftPlayerData {
        public final PlayerSnapshot snapshot;
        public int secondsRemaining;

        public LeftPlayerData(PlayerSnapshot snapshot, int secondsRemaining) {
            this.snapshot = snapshot;
            this.secondsRemaining = secondsRemaining;
        }
    }
}
