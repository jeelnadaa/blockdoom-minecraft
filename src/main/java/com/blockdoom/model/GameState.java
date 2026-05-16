package com.blockdoom.model;

/**
 * Represents the current state of the BlockDoom gameplay loop.
 */
public enum GameState {
    /**
     * Plugin is ready but the game has not started yet.
     */
    WAITING,

    /**
     * Timer is actively ticking down towards a block selection.
     */
    RUNNING,

    /**
     * The game cycle is paused.
     */
    PAUSED,

    /**
     * Timer reached 0; the block has been selected and revealed, waiting 5 seconds.
     */
    REVEALING,

    /**
     * The 5-second reveal window ended; block deletion is actively in progress.
     */
    DELETING,

    /**
     * Players defeated the Ender Dragon.
     */
    WON,

    /**
     * Progression became impossible.
     */
    LOST
}
