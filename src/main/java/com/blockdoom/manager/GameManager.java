package com.blockdoom.manager;

import com.blockdoom.BlockDoomPlugin;
import com.blockdoom.model.GameState;
import com.blockdoom.util.MessageUtil;
import org.bukkit.Material;
import org.bukkit.World;

/**
 * Coordinates game state transitions and overall gameplay lifecycle.
 */
public class GameManager {
    private final BlockDoomPlugin plugin;
    private final ConfigManager configManager;
    private final StorageManager storageManager;
    private final DimensionManager dimensionManager;
    private final BlockSelectionManager blockSelectionManager;
    private final DeletionManager deletionManager;
    private final UIManager uiManager;
    private final WinLossManager winLossManager;
    private final WorldRegenerationManager worldRegenerationManager;
    private TimerManager timerManager;

    private GameState state = GameState.WAITING;
    private World activeDimension;
    private Material selectedMaterial;

    public GameManager(BlockDoomPlugin plugin, ConfigManager configManager, StorageManager storageManager,
                       DimensionManager dimensionManager, BlockSelectionManager blockSelectionManager,
                       DeletionManager deletionManager, UIManager uiManager,
                       WorldRegenerationManager worldRegenerationManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.storageManager = storageManager;
        this.dimensionManager = dimensionManager;
        this.blockSelectionManager = blockSelectionManager;
        this.deletionManager = deletionManager;
        this.uiManager = uiManager;
        this.worldRegenerationManager = worldRegenerationManager;
        this.winLossManager = new WinLossManager(this);
    }

    public void setTimerManager(TimerManager timerManager) {
        this.timerManager = timerManager;
    }

    public GameState getState() { return state; }
    public World getActiveDimension() { return activeDimension; }
    public Material getSelectedMaterial() { return selectedMaterial; }
    public WinLossManager getWinLossManager() { return winLossManager; }

    public void prepareNextCycleBlockIfConfigured() {
        if (configManager.isShowNextBlockDuringTimer()) {
            World dim = dimensionManager.selectDimension();
            if (dim != null) {
                blockSelectionManager.selectBlockAsync(dim, mat -> {
                    selectedMaterial = mat;
                    activeDimension = dim;
                });
            }
        } else {
            selectedMaterial = null;
        }
    }

    public void start() {
        if (state == GameState.RUNNING) {
            return;
        }
        if (worldRegenerationManager.isRegenerating()) {
            MessageUtil.broadcast("<red>Cannot start game while world is regenerating!</red>");
            return;
        }
        if (state == GameState.WAITING || state == GameState.WON || state == GameState.LOST) {
            timerManager.resetTimer();
        }
        state = GameState.RUNNING;
        prepareNextCycleBlockIfConfigured();
        timerManager.start();
        MessageUtil.broadcast("<green><bold>GAME STARTED:</bold></green> The countdown has begun. Brace yourselves for deletion!");
    }

    public void pause() {
        if (state == GameState.RUNNING || state == GameState.REVEALING || state == GameState.DELETING) {
            state = GameState.PAUSED;
            MessageUtil.broadcast("<yellow><bold>GAME PAUSED:</bold></yellow> All deletion timers and processes are paused.");
        }
    }

    public void skip() {
        if (state == GameState.RUNNING || state == GameState.PAUSED) {
            state = GameState.RUNNING;
            timerManager.setRemainingSeconds(1);
            MessageUtil.broadcast("<gold><bold>CYCLE SKIPPED:</bold></gold> Jumping immediately to next block reveal!");
        }
    }

    public void forceStart() {
        start();
    }

    public void forceDelete(Material material) {
        if (material == null || !material.isBlock()) {
            return;
        }
        if (worldRegenerationManager.isRegenerating()) return;
        World dimension = dimensionManager.selectDimension();
        if (dimension == null) return;

        state = GameState.DELETING;
        selectedMaterial = material;
        activeDimension = dimension;

        uiManager.broadcastDeletionStart(material);
        deletionManager.startDeletion(dimension, material, () -> {
            state = GameState.RUNNING;
            timerManager.resetTimer();
            prepareNextCycleBlockIfConfigured();
            MessageUtil.broadcast("<green><bold>CYCLE COMPLETE:</bold></green> All natural " + material.name() + " deleted! Starting next countdown.");
        });
    }

    public void regenerate() {
        state = GameState.WAITING;
        selectedMaterial = null;
        timerManager.stop();
        deletionManager.stop();
        worldRegenerationManager.regenerateWorlds(() -> {
            state = GameState.WAITING;
            timerManager.resetTimer();
        });
    }

    public void startRevealPhase() {
        if (state != GameState.RUNNING) return;

        if (configManager.isShowNextBlockDuringTimer() && selectedMaterial != null && activeDimension != null) {
            state = GameState.REVEALING;
            uiManager.broadcastReveal(selectedMaterial);
            return;
        }

        activeDimension = dimensionManager.selectDimension();
        if (activeDimension == null) {
            lose();
            return;
        }

        blockSelectionManager.selectBlockAsync(activeDimension, material -> {
            if (material == null) {
                winLossManager.handleNoProgression();
                return;
            }
            selectedMaterial = material;
            state = GameState.REVEALING;
            uiManager.broadcastReveal(material);
        });
    }

    public void startDeletionPhase() {
        if (state != GameState.REVEALING || selectedMaterial == null || activeDimension == null) return;

        state = GameState.DELETING;
        uiManager.broadcastDeletionStart(selectedMaterial);
        deletionManager.startDeletion(activeDimension, selectedMaterial, () -> {
            if (state == GameState.DELETING) {
                state = GameState.RUNNING;
                timerManager.resetTimer();
                prepareNextCycleBlockIfConfigured();
                MessageUtil.broadcast("<green><bold>CYCLE COMPLETE:</bold></green> All natural " + selectedMaterial.name() + " deleted! Starting next countdown.");
            }
        });
    }

    public void win() {
        state = GameState.WON;
        timerManager.stop();
        deletionManager.stop();
        uiManager.broadcastVictory();
    }

    public void lose() {
        state = GameState.LOST;
        timerManager.stop();
        deletionManager.stop();
        uiManager.broadcastDefeat();
    }
}
