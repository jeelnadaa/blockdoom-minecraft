package com.blockdoom.manager;

import com.blockdoom.BlockDoomPlugin;
import com.blockdoom.model.ChunkPos;
import com.blockdoom.util.ParticleUtil;
import com.blockdoom.util.SoundUtil;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * Manages the batched chunk-by-chunk deletion queue on the main thread, scoped per dimension.
 */
public class DeletionManager {
    private final BlockDoomPlugin plugin;
    private final ConfigManager configManager;
    private final StorageManager storageManager;
    private final NamespacedKey scrubKey;

    private final LinkedList<ChunkPos> queue = new LinkedList<>();
    private BukkitTask task;
    private Runnable completionCallback;
    private Material activeMaterial;
    private World activeDimension;

    public DeletionManager(BlockDoomPlugin plugin, ConfigManager configManager, StorageManager storageManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.storageManager = storageManager;
        this.scrubKey = new NamespacedKey(plugin, "scrub_cycle");
    }

    public void startDeletion(World dimension, Material material, Runnable completionCallback) {
        this.activeDimension = dimension;
        this.activeMaterial = material;
        this.completionCallback = completionCallback;

        UUID worldId = dimension.getUID();
        storageManager.addDeletedMaterial(worldId, material);
        queue.clear();

        int targetCycle = storageManager.getDeletedMaterialsForWorld(worldId).size();

        // Queue all currently loaded chunks in the dimension, prioritizing chunks near players
        List<ChunkPos> loadedChunks = new ArrayList<>();
        for (Chunk chunk : dimension.getLoadedChunks()) {
            Integer scrubbedCycle = chunk.getPersistentDataContainer().get(scrubKey, PersistentDataType.INTEGER);
            if (scrubbedCycle == null || scrubbedCycle < targetCycle) {
                loadedChunks.add(ChunkPos.fromChunk(chunk));
            }
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
        UUID worldId = chunk.getWorld().getUID();
        Set<Material> worldDeleted = storageManager.getDeletedMaterialsForWorld(worldId);
        if (worldDeleted.isEmpty()) {
            return;
        }

        int targetCycle = worldDeleted.size();
        Integer scrubbedCycle = chunk.getPersistentDataContainer().get(scrubKey, PersistentDataType.INTEGER);
        if (scrubbedCycle != null && scrubbedCycle == targetCycle) {
            return; // Chunk already fully scrubbed for this dimension's deletion cycles
        }

        ChunkPos pos = ChunkPos.fromChunk(chunk);
        if (!queue.contains(pos)) {
            queue.addLast(pos);
            if (task == null || task.isCancelled()) {
                task = Bukkit.getScheduler().runTaskTimer(plugin, this::processTick, 1L, 1L);
            }
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
        if (queue.isEmpty()) {
            if (task != null) {
                task.cancel();
                task = null;
            }
            if (completionCallback != null && activeMaterial != null && activeDimension != null) {
                completionCallback.run();
                completionCallback = null;
                activeMaterial = null;
                activeDimension = null;
            }
            return;
        }

        int chunksToProcess = configManager.getChunksPerTick();
        int maxBlocks = configManager.getMaxBlocksPerChunkTick();

        for (int c = 0; c < chunksToProcess && !queue.isEmpty(); c++) {
            ChunkPos pos = queue.pollFirst();
            World world = Bukkit.getWorld(pos.worldId());
            if (world == null || !world.isChunkLoaded(pos.x(), pos.z())) {
                continue;
            }

            UUID worldId = world.getUID();
            Set<Material> worldDeleted = storageManager.getDeletedMaterialsForWorld(worldId);
            if (worldDeleted.isEmpty()) {
                continue;
            }

            int targetCycle = worldDeleted.size();
            Chunk chunk = world.getChunkAt(pos.x(), pos.z());
            Integer scrubbedCycle = chunk.getPersistentDataContainer().get(scrubKey, PersistentDataType.INTEGER);
            if (scrubbedCycle != null && scrubbedCycle == targetCycle) {
                continue; // Already fully scrubbed
            }

            int blocksDeleted = 0;
            int minY = world.getMinHeight();
            int maxY = world.getMaxHeight();

            for (int x = 0; x < 16 && blocksDeleted < maxBlocks; x++) {
                for (int z = 0; z < 16 && blocksDeleted < maxBlocks; z++) {
                    int worldX = (pos.x() << 4) + x;
                    int worldZ = (pos.z() << 4) + z;

                    for (int y = minY; y < maxY && blocksDeleted < maxBlocks; y++) {
                        if (storageManager.isProtected(worldId, worldX, y, worldZ)) {
                            continue;
                        }

                        Block block = chunk.getBlock(x, y, z);
                        if (worldDeleted.contains(block.getType())) {
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
            } else {
                // Chunk completely scrubbed for this cycle! Save the cycle count into NBT.
                chunk.getPersistentDataContainer().set(scrubKey, PersistentDataType.INTEGER, targetCycle);
            }
        }
    }
}
