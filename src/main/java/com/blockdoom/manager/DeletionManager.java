package com.blockdoom.manager;

import com.blockdoom.BlockDoomPlugin;
import com.blockdoom.model.ChunkPos;
import com.blockdoom.util.ParticleUtil;
import com.blockdoom.util.SoundUtil;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * Manages the batched chunk-by-chunk deletion queue on the main thread.
 */
public class DeletionManager {
    private final BlockDoomPlugin plugin;
    private final ConfigManager configManager;
    private final StorageManager storageManager;

    private final LinkedList<ChunkPos> queue = new LinkedList<>();
    private BukkitTask task;
    private Runnable completionCallback;
    private Material activeMaterial;
    private World activeDimension;

    public DeletionManager(BlockDoomPlugin plugin, ConfigManager configManager, StorageManager storageManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.storageManager = storageManager;
    }

    public void startDeletion(World dimension, Material material, Runnable completionCallback) {
        this.activeDimension = dimension;
        this.activeMaterial = material;
        this.completionCallback = completionCallback;

        storageManager.addDeletedMaterial(material);
        queue.clear();

        // Queue all currently loaded chunks in the dimension, prioritizing chunks near players
        List<ChunkPos> loadedChunks = new ArrayList<>();
        for (Chunk chunk : dimension.getLoadedChunks()) {
            loadedChunks.add(ChunkPos.fromChunk(chunk));
        }

        // Sort by minimum distance to any active player
        loadedChunks.sort(Comparator.comparingDouble(pos -> {
            double minDist = Double.MAX_VALUE;
            for (org.bukkit.entity.Player player : dimension.getPlayers()) {
                double dist = Math.hypot((pos.x() << 4) - player.getLocation().getX(), (pos.z() << 4) - player.getLocation().getZ());
                if (dist < minDist) minDist = dist;
            }
            return minDist;
        }));

        queue.addAll(loadedChunks);

        if (task == null || task.isCancelled()) {
            task = Bukkit.getScheduler().runTaskTimer(plugin, this::processTick, 1L, 1L);
        }
    }

    public void queueChunkIfNecessary(Chunk chunk) {
        if (activeDimension == null || activeMaterial == null) return;
        if (!chunk.getWorld().getUID().equals(activeDimension.getUID())) return;
        ChunkPos pos = ChunkPos.fromChunk(chunk);
        if (!queue.contains(pos)) {
            queue.addLast(pos);
        }
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        queue.clear();
        activeMaterial = null;
        activeDimension = null;
    }

    private void processTick() {
        if (queue.isEmpty() || activeMaterial == null || activeDimension == null) {
            stop();
            if (completionCallback != null) {
                completionCallback.run();
                completionCallback = null;
            }
            return;
        }

        int chunksToProcess = configManager.getChunksPerTick();
        int maxBlocks = configManager.getMaxBlocksPerChunkTick();
        UUID worldId = activeDimension.getUID();

        for (int c = 0; c < chunksToProcess && !queue.isEmpty(); c++) {
            ChunkPos pos = queue.pollFirst();
            if (!activeDimension.isChunkLoaded(pos.x(), pos.z())) {
                continue;
            }

            Chunk chunk = activeDimension.getChunkAt(pos.x(), pos.z());
            int blocksDeleted = 0;
            int minY = activeDimension.getMinHeight();
            int maxY = activeDimension.getMaxHeight();

            for (int x = 0; x < 16 && blocksDeleted < maxBlocks; x++) {
                for (int z = 0; z < 16 && blocksDeleted < maxBlocks; z++) {
                    int worldX = (pos.x() << 4) + x;
                    int worldZ = (pos.z() << 4) + z;

                    for (int y = minY; y < maxY && blocksDeleted < maxBlocks; y++) {
                        if (storageManager.isProtected(worldId, worldX, y, worldZ)) {
                            continue;
                        }

                        Block block = chunk.getBlock(x, y, z);
                        if (block.getType() == activeMaterial) {
                            ParticleUtil.spawnDisintegrateParticle(block.getLocation(), block.getBlockData());
                            block.setType(Material.AIR, false);
                            blocksDeleted++;

                            if (blocksDeleted % 50 == 0) {
                                SoundUtil.playSoundAt(block.getLocation(), "BLOCK_STONE_BREAK", 0.5f, 0.8f);
                            }
                        }
                    }
                }
            }

            // If we hit the max block limit for this chunk, put it back at the front of the queue
            if (blocksDeleted >= maxBlocks) {
                queue.addFirst(pos);
                break; // Stop processing further chunks this tick to preserve TPS
            }
        }
    }
}
