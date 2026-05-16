package com.blockdoom.manager;

import org.bukkit.block.Block;

/**
 * Manages tracking of player-placed blocks to protect them from deletion.
 */
public class PlacementTrackingManager {
    private final StorageManager storageManager;

    public PlacementTrackingManager(StorageManager storageManager) {
        this.storageManager = storageManager;
    }

    public void protectBlock(Block block) {
        storageManager.addProtectedBlock(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ());
    }

    public void unprotectBlock(Block block) {
        storageManager.removeProtectedBlock(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ());
    }

    public boolean isProtected(Block block) {
        return storageManager.isProtected(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ());
    }

    public boolean isProtected(java.util.UUID worldId, int x, int y, int z) {
        return storageManager.isProtected(worldId, x, y, z);
    }
}
