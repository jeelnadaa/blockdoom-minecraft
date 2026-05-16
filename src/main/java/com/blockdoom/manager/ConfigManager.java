package com.blockdoom.manager;

import com.blockdoom.BlockDoomPlugin;
import com.blockdoom.util.MessageUtil;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

/**
 * Manages configuration loading, live reloading, and on-the-fly value updates.
 */
public class ConfigManager {
    private final BlockDoomPlugin plugin;
    private Runnable reloadCallback;

    private int timerDuration;
    private int revealDelay;
    private int scanRadius;
    private boolean showNextBlockDuringTimer;
    private boolean protectPlayerBuilds;
    private boolean autoReloadOnConfigChange;
    private int chunksPerTick;
    private int maxBlocksPerChunkTick;

    private final Map<String, String> enabledDimensions = new HashMap<>();
    private final Set<Material> blacklist = new HashSet<>();

    private String prefix;
    private String actionbarTimer;
    private String actionbarTimerWithBlock;
    private String actionbarRevealing;
    private String actionbarPaused;
    private String titleReveal;
    private String subtitleReveal;
    private String titleDeletionStart;
    private String subtitleDeletionStart;
    private String titleVictory;
    private String subtitleVictory;
    private String titleDefeat;
    private String subtitleDefeat;

    private String soundTimerTick;
    private String soundReveal;
    private String soundDeletionStart;
    private String soundVictory;
    private String soundDefeat;

    public ConfigManager(BlockDoomPlugin plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig();
        loadConfig();
    }

    public void setReloadCallback(Runnable reloadCallback) {
        this.reloadCallback = reloadCallback;
    }

    public void loadConfig() {
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        this.timerDuration = config.getInt("game.timer-duration", 60);
        this.revealDelay = config.getInt("game.reveal-delay", 5);
        this.scanRadius = config.getInt("game.scan-radius", 3);
        this.showNextBlockDuringTimer = config.getBoolean("game.show-next-block-during-timer", false);
        this.protectPlayerBuilds = config.getBoolean("game.protect-player-builds", true);
        this.autoReloadOnConfigChange = config.getBoolean("game.auto-reload-on-config-change", true);

        this.chunksPerTick = config.getInt("performance.chunks-per-tick", 10);
        this.maxBlocksPerChunkTick = config.getInt("performance.max-blocks-per-chunk-tick", 500);

        this.enabledDimensions.clear();
        if (config.contains("game.enabled-dimensions")) {
            for (String key : config.getConfigurationSection("game.enabled-dimensions").getKeys(false)) {
                this.enabledDimensions.put(key.toLowerCase(), config.getString("game.enabled-dimensions." + key));
            }
        } else {
            this.enabledDimensions.put("overworld", "blockdoom_overworld");
            this.enabledDimensions.put("nether", "blockdoom_nether");
            this.enabledDimensions.put("end", "blockdoom_end");
        }

        this.blacklist.clear();
        List<String> blacklistStr = config.getStringList("blacklist");
        for (String matName : blacklistStr) {
            try {
                Material mat = Material.valueOf(matName.toUpperCase());
                this.blacklist.add(mat);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid material in blacklist: " + matName);
            }
        }

        this.prefix = config.getString("messages.prefix", "<dark_gray>[<red><bold>BlockDoom</bold></red>]</dark_gray> ");
        MessageUtil.setPrefix(this.prefix);

        this.actionbarTimer = config.getString("messages.actionbar-timer", "<yellow>Next deletion in: <gold><bold>%time%</bold></gold></yellow>");
        this.actionbarTimerWithBlock = config.getString("messages.actionbar-timer-with-block", "<yellow>Next deletion: <red><bold>%block%</bold></red> in <gold><bold>%time%</bold></gold></yellow>");
        this.actionbarRevealing = config.getString("messages.actionbar-revealing", "<red><bold>%block%</bold> will be deleted in <gold>%time%s</gold>!</red>");
        this.actionbarPaused = config.getString("messages.actionbar-paused", "<gray><italic>Deletion Cycle Paused</italic></gray>");
        this.titleReveal = config.getString("messages.title-reveal", "<red><bold>%block%</bold></red>");
        this.subtitleReveal = config.getString("messages.subtitle-reveal", "<gray>Will be erased in 5 seconds...</gray>");
        this.titleDeletionStart = config.getString("messages.title-deletion-start", "<dark_red><bold>DISINTEGRATION!</bold></dark_red>");
        this.subtitleDeletionStart = config.getString("messages.subtitle-deletion-start", "<yellow>All natural %block% is vanishing!</yellow>");
        this.titleVictory = config.getString("messages.title-victory", "<gold><bold>VICTORY!</bold></gold>");
        this.subtitleVictory = config.getString("messages.subtitle-victory", "<yellow>The Ender Dragon has been defeated!</yellow>");
        this.titleDefeat = config.getString("messages.title-defeat", "<dark_red><bold>DEFEAT!</bold></dark_red>");
        this.subtitleDefeat = config.getString("messages.subtitle-defeat", "<gray>Progression is impossible. Game Over.</gray>");

        this.soundTimerTick = config.getString("sounds.timer-tick", "BLOCK_NOTE_BLOCK_BIT");
        this.soundReveal = config.getString("sounds.reveal", "ENTITY_ENDER_DRAGON_GROWL");
        this.soundDeletionStart = config.getString("sounds.deletion-start", "ENTITY_GENERIC_EXPLODE");
        this.soundVictory = config.getString("sounds.victory", "UI_TOAST_CHALLENGE_COMPLETE");
        this.soundDefeat = config.getString("sounds.defeat", "ENTITY_WITHER_DEATH");
    }

    private void triggerAutoReload() {
        if (autoReloadOnConfigChange && reloadCallback != null) {
            reloadCallback.run();
            MessageUtil.broadcast("<green>BlockDoom configuration updated and auto-reloaded successfully!</green>");
        }
    }

    public void setTimerDuration(int seconds) {
        this.timerDuration = Math.max(5, seconds);
        plugin.getConfig().set("game.timer-duration", this.timerDuration);
        plugin.saveConfig();
        triggerAutoReload();
    }

    public void setScanRadius(int radius) {
        this.scanRadius = Math.max(1, Math.min(8, radius));
        plugin.getConfig().set("game.scan-radius", this.scanRadius);
        plugin.saveConfig();
        triggerAutoReload();
    }

    public void setShowNextBlockDuringTimer(boolean show) {
        this.showNextBlockDuringTimer = show;
        plugin.getConfig().set("game.show-next-block-during-timer", show);
        plugin.saveConfig();
        triggerAutoReload();
    }

    public void setProtectPlayerBuilds(boolean protect) {
        this.protectPlayerBuilds = protect;
        plugin.getConfig().set("game.protect-player-builds", protect);
        plugin.saveConfig();
        triggerAutoReload();
    }

    public void setAutoReloadOnConfigChange(boolean autoReload) {
        this.autoReloadOnConfigChange = autoReload;
        plugin.getConfig().set("game.auto-reload-on-config-change", autoReload);
        plugin.saveConfig();
        triggerAutoReload();
    }

    public void setChunksPerTick(int chunks) {
        this.chunksPerTick = Math.max(1, Math.min(100, chunks));
        plugin.getConfig().set("performance.chunks-per-tick", this.chunksPerTick);
        plugin.saveConfig();
        triggerAutoReload();
    }

    public void addBlacklistMaterial(Material mat) {
        if (mat == null || blacklist.contains(mat)) return;
        blacklist.add(mat);
        List<String> list = plugin.getConfig().getStringList("blacklist");
        if (!list.contains(mat.name())) {
            list.add(mat.name());
            plugin.getConfig().set("blacklist", list);
            plugin.saveConfig();
            triggerAutoReload();
        }
    }

    public void removeBlacklistMaterial(Material mat) {
        if (mat == null || !blacklist.contains(mat)) return;
        blacklist.remove(mat);
        List<String> list = plugin.getConfig().getStringList("blacklist");
        list.remove(mat.name());
        plugin.getConfig().set("blacklist", list);
        plugin.saveConfig();
        triggerAutoReload();
    }

    public int getTimerDuration() { return timerDuration; }
    public int getRevealDelay() { return revealDelay; }
    public int getScanRadius() { return scanRadius; }
    public boolean isShowNextBlockDuringTimer() { return showNextBlockDuringTimer; }
    public boolean isProtectPlayerBuilds() { return protectPlayerBuilds; }
    public boolean isAutoReloadOnConfigChange() { return autoReloadOnConfigChange; }
    public int getChunksPerTick() { return chunksPerTick; }
    public int getMaxBlocksPerChunkTick() { return maxBlocksPerChunkTick; }
    public Map<String, String> getEnabledDimensions() { return enabledDimensions; }
    public Set<Material> getBlacklist() { return blacklist; }

    public String getActionbarTimer() { return actionbarTimer; }
    public String getActionbarTimerWithBlock() { return actionbarTimerWithBlock; }
    public String getActionbarRevealing() { return actionbarRevealing; }
    public String getActionbarPaused() { return actionbarPaused; }
    public String getTitleReveal() { return titleReveal; }
    public String getSubtitleReveal() { return subtitleReveal; }
    public String getTitleDeletionStart() { return titleDeletionStart; }
    public String getSubtitleDeletionStart() { return subtitleDeletionStart; }
    public String getTitleVictory() { return titleVictory; }
    public String getSubtitleVictory() { return subtitleVictory; }
    public String getTitleDefeat() { return titleDefeat; }
    public String getSubtitleDefeat() { return subtitleDefeat; }

    public String getSoundTimerTick() { return soundTimerTick; }
    public String getSoundReveal() { return soundReveal; }
    public String getSoundDeletionStart() { return soundDeletionStart; }
    public String getSoundVictory() { return soundVictory; }
    public String getSoundDefeat() { return soundDefeat; }
}
