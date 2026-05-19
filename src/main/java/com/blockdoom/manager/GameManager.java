package com.blockdoom.manager;

import com.blockdoom.BlockDoomPlugin;
import com.blockdoom.model.GameState;
import com.blockdoom.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;

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
    private TimerManager timerManager;

    private GameState state = GameState.WAITING;
    private GameState previousState = GameState.RUNNING;
    private World activeDimension;
    private Material selectedMaterial;

    public GameManager(BlockDoomPlugin plugin, ConfigManager configManager, StorageManager storageManager,
                       DimensionManager dimensionManager, BlockSelectionManager blockSelectionManager,
                       DeletionManager deletionManager, UIManager uiManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.storageManager = storageManager;
        this.dimensionManager = dimensionManager;
        this.blockSelectionManager = blockSelectionManager;
        this.deletionManager = deletionManager;
        this.uiManager = uiManager;
        this.winLossManager = new WinLossManager(this);
    }

    public void setTimerManager(TimerManager timerManager) {
        this.timerManager = timerManager;
    }

    public GameState getState() { return state; }
    public World getActiveDimension() { return activeDimension; }
    public Material getSelectedMaterial() { return selectedMaterial; }
    public WinLossManager getWinLossManager() { return winLossManager; }

    public void start() {
        if (state == GameState.RUNNING) {
            return;
        }
        if (state == GameState.PAUSED) {
            state = (previousState != null && previousState != GameState.PAUSED) ? previousState : GameState.RUNNING;
            deletionManager.resume();
            MessageUtil.broadcast("<green><bold>GAME RESUMED:</bold></green> Deletion cycle and timers resumed.");
            return;
        }
        if (state == GameState.WAITING || state == GameState.WON || state == GameState.LOST) {
            timerManager.resetTimer();
        }
        state = GameState.RUNNING;
        deletionManager.resume();
        selectedMaterial = null;
        timerManager.start();
        MessageUtil.broadcast("<green><bold>GAME STARTED:</bold></green> The countdown has begun. Brace yourselves for deletion!");
    }

    public void pause() {
        if (state == GameState.RUNNING || state == GameState.REVEALING || state == GameState.DELETING) {
            previousState = state;
            state = GameState.PAUSED;
            deletionManager.pause();
            MessageUtil.broadcast("<yellow><bold>GAME PAUSED:</bold></yellow> All deletion timers and processes are paused.");
        }
    }

    public void skip() {
        if (state == GameState.RUNNING || state == GameState.PAUSED) {
            state = GameState.RUNNING;
            deletionManager.resume();
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
        World dimension = dimensionManager.selectDimension();
        if (dimension == null) return;

        state = GameState.DELETING;
        selectedMaterial = material;
        activeDimension = dimension;

        uiManager.broadcastDeletionStart(material);
        deletionManager.startDeletion(dimension, material, () -> {
            state = GameState.RUNNING;
            selectedMaterial = null;
            timerManager.resetTimer();
            MessageUtil.broadcast("<green><bold>CYCLE COMPLETE:</bold></green> All natural " + material.name() + " deleted! Starting next countdown.");
        });
    }

    public void reset() {
        state = GameState.WAITING;
        selectedMaterial = null;
        timerManager.stop();
        deletionManager.stop();
        storageManager.resetAll();
        timerManager.resetTimer();
        MessageUtil.broadcast("<yellow><bold>GAME RESET:</bold></yellow> All deletion registries and timers have been wiped clean.");
    }

    private boolean isScanning = false;

    public void performScanningTick(int remainingScanSeconds) {
        if (isScanning) return;
        isScanning = true;

        if (activeDimension == null || (remainingScanSeconds > 0 && activeDimension.getPlayers().isEmpty())) {
            activeDimension = dimensionManager.selectDimension();
        }
        if (activeDimension == null) {
            isScanning = false;
            return;
        }

        boolean aroundPlayers = remainingScanSeconds > 0;
        blockSelectionManager.selectBlockAsync(activeDimension, aroundPlayers, mat -> {
            isScanning = false;
            if (mat != null) {
                selectedMaterial = mat;
            } else if (remainingScanSeconds == 0) {
                List<String> enabledWorldNames = new ArrayList<>(configManager.getEnabledDimensions().values());
                trySelectAcrossDimensions(activeDimension, enabledWorldNames);
            }
        });
    }

    private void trySelectAcrossDimensions(World currentWorld, List<String> remainingWorldNames) {
        remainingWorldNames.removeIf(name -> name.equalsIgnoreCase(currentWorld.getName()));

        blockSelectionManager.selectBlockAsync(currentWorld, false, material -> {
            if (material != null) {
                selectedMaterial = material;
                activeDimension = currentWorld;
            } else if (!remainingWorldNames.isEmpty()) {
                String nextWorldName = remainingWorldNames.remove(0);
                World nextWorld = Bukkit.getWorld(nextWorldName);
                if (nextWorld != null) {
                    trySelectAcrossDimensions(nextWorld, remainingWorldNames);
                } else {
                    trySelectAcrossDimensions(currentWorld, remainingWorldNames);
                }
            } else {
                // All enabled dimensions are completely empty!
                MessageUtil.broadcast("<red>All enabled dimensions are completely empty of naturally generated solid blocks!</red>");
                winLossManager.handleNoProgression();
            }
        });
    }

    public void startRevealPhase() {
        if (state != GameState.RUNNING) return;

        if (selectedMaterial != null && activeDimension != null) {
            state = GameState.REVEALING;
            uiManager.broadcastReveal(selectedMaterial);
        } else {
            lose();
        }
    }

    public void startDeletionPhase() {
        if (state != GameState.REVEALING || selectedMaterial == null || activeDimension == null) return;

        state = GameState.DELETING;
        uiManager.broadcastDeletionStart(selectedMaterial);
        String matName = selectedMaterial.name();
        deletionManager.startDeletion(activeDimension, selectedMaterial, () -> {
            if (state == GameState.DELETING) {
                state = GameState.RUNNING;
                selectedMaterial = null;
                timerManager.resetTimer();
                MessageUtil.broadcast("<green><bold>CYCLE COMPLETE:</bold></green> All natural " + matName + " deleted! Starting next countdown.");
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
