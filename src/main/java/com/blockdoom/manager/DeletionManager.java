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
 * Completely scrubs each polled chunk in full per tick without partial block limits.
 */
public class DeletionManager {
    private final BlockDoomPlugin plugin;
    private final ConfigManager configManager;
    private final StorageManager storageManager;
    private final NamespacedKey scrubKey;

    private final LinkedList<ChunkPos> activeQueue = new LinkedList<>();
    private final LinkedList<ChunkPos> backgroundQueue = new LinkedList<>();
    private final Set<ChunkPos> queuedSet = new HashSet<>(); // O(1) lookup set

    private BukkitTask task;
    private Runnable completionCallback;
    private Material activeMaterial;
    private World activeDimension;
    private boolean isPaused = false;

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
        this.isPaused = false;

        UUID worldId = dimension.getUID();
        storageManager.addDeletedMaterial(worldId, material);
        activeQueue.clear();
        queuedSet.clear();

        int targetCycle = storageManager.getDeletedMaterialsForWorld(worldId).size();

        // Queue all currently loaded chunks in the dimension
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

        activeQueue.addAll(loadedChunks);
        queuedSet.addAll(loadedChunks);

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
        if (queuedSet.add(pos)) {
            backgroundQueue.addLast(pos);
            if (task == null || task.isCancelled()) {
                task = Bukkit.getScheduler().runTaskTimer(plugin, this::processTick, 1L, 1L);
            }
        }
    }

    public void pause() {
        this.isPaused = true;
    }

    public void resume() {
        this.isPaused = false;
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        activeQueue.clear();
        backgroundQueue.clear();
        queuedSet.clear();
        activeMaterial = null;
        activeDimension = null;
        isPaused = false;
    }

    private void processTick() {
        if (isPaused) {
            return;
        }

        if (activeQueue.isEmpty()) {
            if (completionCallback != null && activeMaterial != null && activeDimension != null) {
                completionCallback.run();
                completionCallback = null;
                activeMaterial = null;
                activeDimension = null;
            }
            if (backgroundQueue.isEmpty()) {
                if (task != null) {
                    task.cancel();
                    task = null;
                }
                return;
            }
        }

        int chunksProcessed = 0;
        int maxChunksToProcess = configManager.getChunksPerTick();

        while (chunksProcessed < maxChunksToProcess && (!activeQueue.isEmpty() || !backgroundQueue.isEmpty())) {
            ChunkPos pos = !activeQueue.isEmpty() ? activeQueue.pollFirst() : backgroundQueue.pollFirst();
            queuedSet.remove(pos);

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

            int minY = world.getMinHeight();
            int maxY = world.getMaxHeight();
            int blocksDeleted = 0;

            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    int worldX = (pos.x() << 4) + x;
                    int worldZ = (pos.z() << 4) + z;

                    for (int y = minY; y < maxY; y++) {
                        if (configManager.isProtectPlayerBuilds() && storageManager.isProtected(worldId, worldX, y, worldZ)) {
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

            // Chunk completely scrubbed in full! Save the cycle count into NBT.
            chunk.getPersistentDataContainer().set(scrubKey, PersistentDataType.INTEGER, targetCycle);
            chunksProcessed++;
        }
    }
}
