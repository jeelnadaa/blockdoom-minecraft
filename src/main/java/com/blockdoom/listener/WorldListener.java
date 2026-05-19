package com.blockdoom.listener;

import com.blockdoom.BlockDoomPlugin;
import com.blockdoom.manager.DeletionManager;
import org.bukkit.Chunk;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

/**
 * Listens to chunk loading to queue newly loaded chunks into active deletion processing.
 */
public class WorldListener implements Listener {
    private final BlockDoomPlugin plugin;
    private final DeletionManager deletionManager;

    public WorldListener(BlockDoomPlugin plugin, DeletionManager deletionManager) {
        this.plugin = plugin;
        this.deletionManager = deletionManager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkLoad(ChunkLoadEvent event) {
        final Chunk chunk = event.getChunk();
        // Delay queuing by 5 ticks to ensure client has fully loaded the chunk and will receive block updates
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (chunk.isLoaded()) {
                deletionManager.queueChunkIfNecessary(chunk);
            }
        }, 5L);
    }
}
