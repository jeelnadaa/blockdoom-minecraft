package com.blockdoom.manager;

/**
 * Monitors game win/loss conditions.
 */
public class WinLossManager {
    private final GameManager gameManager;

    public WinLossManager(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    public void handleDragonDeath() {
        gameManager.win();
    }

    public void handleNoProgression() {
        gameManager.lose();
    }
}
