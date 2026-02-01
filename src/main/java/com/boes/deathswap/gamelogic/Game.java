package com.boes.deathswap.gamelogic;

import com.boes.deathswap.DeathSwap;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.stream.Collectors;

public class Game {

    public final DeathSwap plugin;
    private boolean running = false;
    private boolean stopping = false;
    private World gameWorld;
    private final Map<Player, PlayerSnapshot> snapshots = new HashMap<>();
    private final Map<Player, PlayerSnapshot> spectatorSnapshots = new HashMap<>();
    private final Set<Player> spectators = new HashSet<>();

    private int totalTimeSeconds = 300;
    private int cooldownSeconds = 30;
    private BukkitRunnable timerTask;
    private BossBar bossBar;
    
    private final Map<Player, LeftPlayerData> leftPlayers = new HashMap<>();
    private static final int REJOIN_TIMEOUT_SECONDS = 180; // 3 minutes

    public Game(DeathSwap plugin) {
        this.plugin = plugin;
    }

    public void setTotalTimeSeconds(int totalTimeSeconds) {
        this.totalTimeSeconds = totalTimeSeconds;
    }

    public int getTotalTimeSeconds() {
        return totalTimeSeconds;
    }

    public void setCooldownSeconds(int cooldownSeconds) {
        this.cooldownSeconds = cooldownSeconds;
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

    public World getGameWorld() {
        return gameWorld;
    }

    public void setGameWorld(World gameWorld) {
        this.gameWorld = gameWorld;
    }

    public Map<Player, PlayerSnapshot> getSnapshots() {
        return snapshots;
    }

    public Map<Player, PlayerSnapshot> getSpectatorSnapshots() {
        return spectatorSnapshots;
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

                // Update BossBar players
                bossBar.removeAll();
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getWorld().equals(gameWorld) || spectators.contains(p)) {
                        bossBar.addPlayer(p);
                    }
                }

                // Update BossBar display
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
                        if (p.getWorld().equals(gameWorld) || spectators.contains(p)) {
                            p.sendTitle(ChatColor.RED + String.valueOf(swapCooldown), ChatColor.YELLOW + "Swapping in...", 0, 21, 0);
                        }
                    }
                }

                if (swapCooldown <= 0) {
                    doSwap(alive);
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

    public void doSwap(List<Player> alivePlayers) {
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
        if (!running) return;

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
            message = ChatColor.GREEN + "DeathSwap winner: " + alive.get(0).getName();
        } else {
            message = ChatColor.YELLOW + "DeathSwap ended with no winner.";
        }

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getWorld().equals(gameWorld) || spectators.contains(p)) {
                p.sendMessage(message);
            }
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                new Stop(plugin, Game.this).stop();
            }
        }.runTaskLater(plugin, 40L); // 2 second delay
    }


    public List<Player> getAlivePlayers() {
        List<Player> alive = new ArrayList<>();
        if (gameWorld == null) return alive;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getWorld().equals(gameWorld) && p.getGameMode() == GameMode.SURVIVAL && !p.isDead()) {
                alive.add(p);
            }
        }
        return alive;
    }

    public void addSpectator(Player player, PlayerSnapshot snapshot) {
        spectators.add(player);
        if (snapshot != null) {
            spectatorSnapshots.put(player, snapshot);
        }
    }

    public void removeSpectator(Player player) {
        spectators.remove(player);
        spectatorSnapshots.remove(player);
    }

    public boolean isSpectating(Player player) {
        return spectators.contains(player);
    }

    public List<Player> getPlayersInGameWorld() {
        if (gameWorld == null) return new ArrayList<>();
        return Bukkit.getOnlinePlayers().stream()
                .filter(p -> p.getWorld().equals(gameWorld))
                .collect(Collectors.toList());
    }

    public void onPlayerLeave(Player player) {
        if (!running) return;
        
        PlayerSnapshot snapshot = new PlayerSnapshot(player);
        leftPlayers.put(player, new LeftPlayerData(snapshot, REJOIN_TIMEOUT_SECONDS));
    }

    public boolean handlePlayerRejoin(Player player) {
        LeftPlayerData data = leftPlayers.get(player);
        if (data == null) return true;
        
        leftPlayers.remove(player);
        
        if (!running) {
            data.snapshot.restore();
            return false;
        }

        return true;
    }

    public void restoreLeftPlayers() {
        for (LeftPlayerData data : leftPlayers.values()) {
            if (data.snapshot.player.isOnline()) {
                data.snapshot.restore();
            }
        }
        leftPlayers.clear();
    }

    private void tickLeftPlayerTimeouts() {
        List<Player> timedOut = new ArrayList<>();
        
        for (Map.Entry<Player, LeftPlayerData> entry : leftPlayers.entrySet()) {
            entry.getValue().secondsRemaining--;
            if (entry.getValue().secondsRemaining <= 0) {
                timedOut.add(entry.getKey());
            }
        }

        for (Player player : timedOut) {
            LeftPlayerData data = leftPlayers.remove(player);
            if (player.isOnline()) {
                data.snapshot.restore();
                player.sendMessage(ChatColor.RED + "You were automatically restored due to inactivity timeout.");
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
        public final Player player;
        public final Location location;
        public final ItemStack[] contents;
        public final ItemStack[] armor;
        public final GameMode gamemode;
        public final double health;
        public final int food;
        public final float saturation;

        public PlayerSnapshot(Player p) {
            player = p;
            location = p.getLocation();
            contents = p.getInventory().getContents();
            armor = p.getInventory().getArmorContents();
            gamemode = p.getGameMode();
            health = p.getHealth();
            food = p.getFoodLevel();
            saturation = p.getSaturation();
        }

        public void restore() {
            player.getInventory().setContents(contents);
            player.getInventory().setArmorContents(armor);
            
            // Ensure chunk is loaded before restoring
            if (location.getWorld() != null) {
                location.getChunk().load();
            }
            
            player.teleport(location);
            player.setGameMode(gamemode);
            player.setHealth(health);
            player.setFoodLevel(food);
            player.setSaturation(saturation);
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
