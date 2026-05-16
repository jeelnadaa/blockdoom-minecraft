package com.blockdoom.listener;

import com.blockdoom.manager.DeletionManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

/**
 * Listens to chunk loading to queue newly loaded chunks into active deletion processing.
 */
public class WorldListener implements Listener {
    private final DeletionManager deletionManager;

    public WorldListener(DeletionManager deletionManager) {
        this.deletionManager = deletionManager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkLoad(ChunkLoadEvent event) {
        deletionManager.queueChunkIfNecessary(event.getChunk());
    }
}
