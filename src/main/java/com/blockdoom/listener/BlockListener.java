package com.blockdoom.listener;

import com.blockdoom.manager.ConfigManager;
import com.blockdoom.manager.PlacementTrackingManager;
import com.blockdoom.manager.StorageManager;
import com.blockdoom.manager.WorldRegenerationManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.UUID;

/**
 * Listens to block placement and break events to maintain protection of player-placed blocks.
 */
public class BlockListener implements Listener {
    private final ConfigManager configManager;
    private final PlacementTrackingManager placementTrackingManager;
    private final StorageManager storageManager;
    private final WorldRegenerationManager worldRegenerationManager;

    public BlockListener(ConfigManager configManager, PlacementTrackingManager placementTrackingManager, StorageManager storageManager, WorldRegenerationManager worldRegenerationManager) {
        this.configManager = configManager;
        this.placementTrackingManager = placementTrackingManager;
        this.storageManager = storageManager;
        this.worldRegenerationManager = worldRegenerationManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (worldRegenerationManager.isRegenerating()) {
            event.setCancelled(true);
            return;
        }
        if (!configManager.isProtectPlayerBuilds()) {
            return;
        }
        UUID worldId = event.getBlockPlaced().getWorld().getUID();
        if (storageManager.isMaterialDeletedInWorld(worldId, event.getBlockPlaced().getType())) {
            placementTrackingManager.protectBlock(event.getBlockPlaced());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (worldRegenerationManager.isRegenerating()) {
            event.setCancelled(true);
            return;
        }
        if (!configManager.isProtectPlayerBuilds()) {
            return;
        }
        UUID worldId = event.getBlock().getWorld().getUID();
        if (storageManager.isMaterialDeletedInWorld(worldId, event.getBlock().getType())) {
            placementTrackingManager.unprotectBlock(event.getBlock());
        }
    }
}
