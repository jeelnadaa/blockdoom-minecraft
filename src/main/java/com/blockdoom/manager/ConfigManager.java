package com.blockdoom.manager;

import com.blockdoom.BlockDoomPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages plugin configuration loading, saving, and live property access.
 */
public class ConfigManager {
    private final BlockDoomPlugin plugin;
    private final File configFile;
    private FileConfiguration config;

    private int timerDuration;
    private int revealDelay;
    private boolean showNextBlockDuringTimer;
    private boolean protectPlayerBuilds;
    private boolean autoReloadOnConfigChange;
    private int chunksPerTick;
    private final Map<String, String> enabledDimensions = new ConcurrentHashMap<>();
    private final Set<Material> blacklist = ConcurrentHashMap.newKeySet();
    private final Map<String, String> messages = new ConcurrentHashMap<>();
    private final Map<String, String> sounds = new ConcurrentHashMap<>();

    private Runnable reloadCallback;

    public ConfigManager(BlockDoomPlugin plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }
        loadConfig();
    }

    public void setReloadCallback(Runnable reloadCallback) {
        this.reloadCallback = reloadCallback;
    }

    public synchronized void loadConfig() {
        config = YamlConfiguration.loadConfiguration(configFile);

        timerDuration = config.getInt("game.timer-duration", 60);
        revealDelay = config.getInt("game.reveal-delay", 5);
        showNextBlockDuringTimer = config.getBoolean("game.show-next-block-during-timer", false);
        protectPlayerBuilds = config.getBoolean("game.protect-player-builds", true);
        autoReloadOnConfigChange = config.getBoolean("game.auto-reload-on-config-change", true);

        enabledDimensions.clear();
        if (config.contains("game.enabled-dimensions")) {
            for (String key : config.getConfigurationSection("game.enabled-dimensions").getKeys(false)) {
                enabledDimensions.put(key, config.getString("game.enabled-dimensions." + key));
            }
        } else {
            enabledDimensions.put("overworld", "world");
            enabledDimensions.put("nether", "world_nether");
            enabledDimensions.put("end", "world_the_end");
        }
        resolveDimensionNames();

        chunksPerTick = config.getInt("performance.chunks-per-tick", 10);

        blacklist.clear();
        List<String> bList = config.getStringList("blacklist");
        for (String mName : bList) {
            try {
                blacklist.add(Material.valueOf(mName.toUpperCase()));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid material in blacklist: " + mName);
            }
        }

        messages.clear();
        if (config.contains("messages")) {
            for (String key : config.getConfigurationSection("messages").getKeys(false)) {
                messages.put(key, config.getString("messages." + key));
            }
        }

        sounds.clear();
        if (config.contains("sounds")) {
            for (String key : config.getConfigurationSection("sounds").getKeys(false)) {
                sounds.put(key, config.getString("sounds." + key));
            }
        }
    }

    public synchronized void saveConfig() {
        config.set("game.timer-duration", timerDuration);
        config.set("game.reveal-delay", revealDelay);
        config.set("game.show-next-block-during-timer", showNextBlockDuringTimer);
        config.set("game.protect-player-builds", protectPlayerBuilds);
        config.set("game.auto-reload-on-config-change", autoReloadOnConfigChange);
        config.set("performance.chunks-per-tick", chunksPerTick);

        List<String> bList = new ArrayList<>();
        for (Material m : blacklist) bList.add(m.name());
        config.set("blacklist", bList);

        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save config.yml: " + e.getMessage());
        }
    }

    private void triggerAutoReload() {
        saveConfig();
        if (autoReloadOnConfigChange && reloadCallback != null) {
            reloadCallback.run();
            plugin.getServer().broadcastMessage("§a[BlockDoom] Configuration updated and auto-reloaded successfully!");
        }
    }

    private void resolveDimensionNames() {
        List<World> worlds = Bukkit.getWorlds();
        if (worlds.isEmpty()) {
            return;
        }

        World firstNormal = null;
        World firstNether = null;
        World firstEnd = null;

        for (World w : worlds) {
            if (w.getEnvironment() == World.Environment.NORMAL && firstNormal == null) {
                firstNormal = w;
            } else if (w.getEnvironment() == World.Environment.NETHER && firstNether == null) {
                firstNether = w;
            } else if (w.getEnvironment() == World.Environment.THE_END && firstEnd == null) {
                firstEnd = w;
            }
        }

        // 1. Overworld
        String overworldName = enabledDimensions.get("overworld");
        if (overworldName == null || Bukkit.getWorld(overworldName) == null) {
            String detected = firstNormal != null ? firstNormal.getName() : worlds.get(0).getName();
            plugin.getLogger().info("Dimension 'overworld' (" + overworldName + ") not found. Auto-detected: " + detected);
            enabledDimensions.put("overworld", detected);
        }

        // 2. Nether
        String netherName = enabledDimensions.get("nether");
        if (netherName == null || Bukkit.getWorld(netherName) == null) {
            String detected = null;
            if (firstNether != null) {
                detected = firstNether.getName();
            } else {
                String base = enabledDimensions.get("overworld");
                if (Bukkit.getWorld(base + "_nether") != null) {
                    detected = base + "_nether";
                }
            }
            if (detected != null) {
                plugin.getLogger().info("Dimension 'nether' (" + netherName + ") not found. Auto-detected: " + detected);
                enabledDimensions.put("nether", detected);
            }
        }

        // 3. End
        String endName = enabledDimensions.get("end");
        if (endName == null || Bukkit.getWorld(endName) == null) {
            String detected = null;
            if (firstEnd != null) {
                detected = firstEnd.getName();
            } else {
                String base = enabledDimensions.get("overworld");
                if (Bukkit.getWorld(base + "_the_end") != null) {
                    detected = base + "_the_end";
                }
            }
            if (detected != null) {
                plugin.getLogger().info("Dimension 'end' (" + endName + ") not found. Auto-detected: " + detected);
                enabledDimensions.put("end", detected);
            }
        }
    }

    public int getTimerDuration() { return timerDuration; }
    public void setTimerDuration(int timerDuration) {
        this.timerDuration = Math.max(5, timerDuration);
        triggerAutoReload();
    }

    public int getRevealDelay() { return revealDelay; }
    public boolean isShowNextBlockDuringTimer() { return showNextBlockDuringTimer; }
    public void setShowNextBlockDuringTimer(boolean showNextBlockDuringTimer) {
        this.showNextBlockDuringTimer = showNextBlockDuringTimer;
        triggerAutoReload();
    }

    public boolean isProtectPlayerBuilds() { return protectPlayerBuilds; }
    public void setProtectPlayerBuilds(boolean protectPlayerBuilds) {
        this.protectPlayerBuilds = protectPlayerBuilds;
        triggerAutoReload();
    }

    public boolean isAutoReloadOnConfigChange() { return autoReloadOnConfigChange; }
    public void setAutoReloadOnConfigChange(boolean autoReloadOnConfigChange) {
        this.autoReloadOnConfigChange = autoReloadOnConfigChange;
        triggerAutoReload();
    }

    public int getChunksPerTick() { return chunksPerTick; }
    public void setChunksPerTick(int chunksPerTick) {
        this.chunksPerTick = Math.max(1, chunksPerTick);
        triggerAutoReload();
    }

    public Map<String, String> getEnabledDimensions() { return enabledDimensions; }
    public Set<Material> getBlacklist() { return blacklist; }

    public void addBlacklistMaterial(Material material) {
        if (material != null && material.isBlock()) {
            blacklist.add(material);
            triggerAutoReload();
        }
    }

    public void removeBlacklistMaterial(Material material) {
        if (material != null) {
            blacklist.remove(material);
            triggerAutoReload();
        }
    }

    public String getActionbarTimer() { return messages.getOrDefault("timer-tick", "<green>Time until next block deletion: <gold><bold>%time%s</bold></gold></green>"); }
    public String getActionbarTimerWithBlock() { return messages.getOrDefault("timer-tick-known", "<green>Next deletion: <gold><bold>%block%</bold></gold> in <gold><bold>%time%s</bold></gold></green>"); }
    public String getActionbarRevealing() { return messages.getOrDefault("block-revealing", "<red><bold>WARNING: %block% vaporizing in %time%s!</bold></red>"); }
    public String getActionbarPaused() { return messages.getOrDefault("game-paused", "<yellow><bold>GAME PAUSED</bold></yellow>"); }

    public String getTitleReveal() { return messages.getOrDefault("block-reveal-title", "<red><bold>WARNING!</bold></red>"); }
    public String getSubtitleReveal() { return messages.getOrDefault("block-reveal-subtitle", "<gold><bold>%block%</bold></gold> will be deleted across the world!"); }
    public String getTitleDeletionStart() { return messages.getOrDefault("deletion-start-title", "<dark_red><bold>DELETING %block%</bold></dark_red>"); }
    public String getSubtitleDeletionStart() { return messages.getOrDefault("deletion-start-subtitle", "<gray>All natural instances are disintegrating...</gray>"); }
    public String getTitleVictory() { return messages.getOrDefault("victory-title", "<yellow><bold>VICTORY!</bold></yellow>"); }
    public String getSubtitleVictory() { return messages.getOrDefault("victory-subtitle", "<green>The Ender Dragon is defeated! The world is saved!</green>"); }
    public String getTitleDefeat() { return messages.getOrDefault("defeat-title", "<dark_red><bold>DEFEAT!</bold></dark_red>"); }
    public String getSubtitleDefeat() { return messages.getOrDefault("defeat-subtitle", "<gray>Survival is impossible. The world has crumbled.</gray>"); }

    public String getSoundTimerTick() { return sounds.getOrDefault("timer-tick", "BLOCK_NOTE_BLOCK_HAT"); }
    public String getSoundReveal() { return sounds.getOrDefault("block-reveal", "ENTITY_ENDER_DRAGON_GROWL"); }
    public String getSoundDeletionStart() { return sounds.getOrDefault("deletion-start", "ENTITY_WITHER_SPAWN"); }
    public String getSoundVictory() { return sounds.getOrDefault("victory", "UI_TOAST_CHALLENGE_COMPLETE"); }
    public String getSoundDefeat() { return sounds.getOrDefault("defeat", "ENTITY_WITHER_DEATH"); }
}
