package com.blockdoom;

import com.blockdoom.listener.BlockListener;
import com.blockdoom.listener.PlayerListener;
import com.blockdoom.listener.WorldListener;
import com.blockdoom.manager.*;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main plugin entry point for BlockDoom.
 */
public class BlockDoomPlugin extends JavaPlugin {
    private ConfigManager configManager;
    private StorageManager storageManager;
    private PlacementTrackingManager placementTrackingManager;
    private DimensionManager dimensionManager;
    private BlockSelectionManager blockSelectionManager;
    private DeletionManager deletionManager;
    private UIManager uiManager;
    private GameManager gameManager;
    private TimerManager timerManager;

    @Override
    public void onEnable() {
        getLogger().info("Initializing BlockDoom Plugin...");

        this.configManager = new ConfigManager(this);
        this.storageManager = new StorageManager(this);
        this.placementTrackingManager = new PlacementTrackingManager(storageManager);
        this.dimensionManager = new DimensionManager(configManager);
        this.blockSelectionManager = new BlockSelectionManager(this, configManager, storageManager);
        this.deletionManager = new DeletionManager(this, configManager, storageManager);
        this.uiManager = new UIManager(configManager);

        this.gameManager = new GameManager(
                this, configManager, storageManager, dimensionManager,
                blockSelectionManager, deletionManager, uiManager
        );

        this.timerManager = new TimerManager(this, gameManager, uiManager, configManager);
        this.gameManager.setTimerManager(timerManager);

        // Register Commands
        CommandManager commandManager = new CommandManager(this, gameManager, configManager, storageManager);
        PluginCommand cmd = getCommand("blockdoom");
        if (cmd != null) {
            cmd.setExecutor(commandManager);
            cmd.setTabCompleter(commandManager);
        }

        // Register Listeners
        Bukkit.getPluginManager().registerEvents(new BlockListener(configManager, placementTrackingManager, storageManager), this);
        Bukkit.getPluginManager().registerEvents(new PlayerListener(gameManager, configManager), this);
        Bukkit.getPluginManager().registerEvents(new WorldListener(this, deletionManager), this);
        Bukkit.getPluginManager().registerEvents(new com.blockdoom.gui.GUIListener(configManager), this);

        configManager.setReloadCallback(() -> {
            configManager.loadConfig();
            storageManager.loadAll();
            if (timerManager != null) {
                timerManager.resetTimer();
            }
        });

        getLogger().info("BlockDoom successfully enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Disabling BlockDoom...");

        if (timerManager != null) timerManager.stop();
        if (deletionManager != null) deletionManager.stop();
        if (storageManager != null) storageManager.saveAll();

        getLogger().info("BlockDoom successfully disabled!");
    }

    public ConfigManager getConfigManager() { return configManager; }
    public StorageManager getStorageManager() { return storageManager; }
    public GameManager getGameManager() { return gameManager; }
    public BlockSelectionManager getBlockSelectionManager() { return blockSelectionManager; }
}
