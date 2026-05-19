package com.blockdoom.manager;

import com.blockdoom.BlockDoomPlugin;
import com.blockdoom.model.GameState;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

/**
 * Manages the countdown timer cycle and schedules tick processing.
 */
public class TimerManager {
    private final BlockDoomPlugin plugin;
    private final GameManager gameManager;
    private final UIManager uiManager;
    private final ConfigManager configManager;

    private BukkitTask task;
    private int remainingSeconds;
    private int remainingRevealSeconds;
    private int remainingScanSeconds;

    public TimerManager(BlockDoomPlugin plugin, GameManager gameManager, UIManager uiManager, ConfigManager configManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
        this.uiManager = uiManager;
        this.configManager = configManager;
        this.remainingSeconds = configManager.getTimerDuration();
    }

    public void start() {
        if (task != null && !task.isCancelled()) {
            return;
        }
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 0L, 20L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    public void resetTimer() {
        this.remainingSeconds = configManager.getTimerDuration();
        this.remainingRevealSeconds = configManager.getRevealDelay();
        this.remainingScanSeconds = 5;
    }

    public void setRemainingSeconds(int seconds) {
        this.remainingSeconds = Math.max(1, seconds);
    }

    public int getRemainingSeconds() {
        return remainingSeconds;
    }

    private void tick() {
        GameState state = gameManager.getState();
        if (state == GameState.PAUSED) {
            uiManager.broadcastPaused();
            return;
        }

        if (state == GameState.RUNNING) {
            if (gameManager.getSelectedMaterial() == null) {
                uiManager.broadcastScanningTick(remainingScanSeconds);
                gameManager.performScanningTick(remainingScanSeconds);
                if (remainingScanSeconds > 0) {
                    remainingScanSeconds--;
                }
                return;
            }

            if (remainingSeconds > 0) {
                uiManager.broadcastTimerTick(remainingSeconds, gameManager.getSelectedMaterial());
                remainingSeconds--;
            } else {
                gameManager.startRevealPhase();
            }
        } else if (state == GameState.REVEALING) {
            if (gameManager.getSelectedMaterial() == null) {
                // Waiting for async block selection to finish, hold the reveal countdown
                return;
            }
            if (remainingRevealSeconds > 0) {
                uiManager.broadcastRevealingTick(gameManager.getSelectedMaterial(), remainingRevealSeconds);
                remainingRevealSeconds--;
            } else {
                gameManager.startDeletionPhase();
            }
        }
    }
}
