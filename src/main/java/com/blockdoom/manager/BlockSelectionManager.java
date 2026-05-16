package com.blockdoom.manager;

import com.blockdoom.BlockDoomPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Asynchronously scans loaded chunk snapshots near active players to select valid natural blocks.
 */
public class BlockSelectionManager {
    private final BlockDoomPlugin plugin;
    private final ConfigManager configManager;
    private final StorageManager storageManager;

    public BlockSelectionManager(BlockDoomPlugin plugin, ConfigManager configManager, StorageManager storageManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.storageManager = storageManager;
    }

    public void selectBlockAsync(World dimension, Consumer<Material> callback) {
        int radius = configManager.getScanRadius();
        Set<ChunkSnapshot> snapshots = new HashSet<>();
        UUID worldId = dimension.getUID();

        // Gather chunk snapshots on main thread for chunks around active players
        for (Player player : dimension.getPlayers()) {
            Chunk center = player.getLocation().getChunk();
            int cX = center.getX();
            int cZ = center.getZ();

            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    int tX = cX + dx;
                    int tZ = cZ + dz;
                    if (dimension.isChunkLoaded(tX, tZ)) {
                        snapshots.add(dimension.getChunkAt(tX, tZ).getChunkSnapshot());
                    }
                }
            }
        }

        if (snapshots.isEmpty()) {
            callback.accept(null);
            return;
        }

        Set<Material> blacklist = configManager.getBlacklist();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Map<Material, Integer> materialCounts = new HashMap<>();

            for (ChunkSnapshot snapshot : snapshots) {
                int sX = snapshot.getX();
                int sZ = snapshot.getZ();
                int minY = dimension.getMinHeight();
                int maxY = dimension.getMaxHeight();

                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        int worldX = (sX << 4) + x;
                        int worldZ = (sZ << 4) + z;
                        // Sample blocks with step 3 in Y for performance optimization
                        for (int y = minY; y < maxY; y += 3) {
                            if (storageManager.isProtected(worldId, worldX, y, worldZ)) {
                                continue;
                            }
                            Material mat = snapshot.getBlockType(x, y, z);
                            if (!mat.isBlock() || mat.isAir() || mat == Material.WATER || mat == Material.LAVA) {
                                continue;
                            }
                            if (blacklist.contains(mat) || storageManager.isMaterialDeletedGlobally(mat)) {
                                continue;
                            }
                            materialCounts.put(mat, materialCounts.getOrDefault(mat, 0) + 1);
                        }
                    }
                }
            }

            if (materialCounts.isEmpty()) {
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(null));
                return;
            }

            // Remove materials with extremely low count (< 10) unless nothing else exists
            List<Material> validCandidates = new ArrayList<>();
            for (Map.Entry<Material, Integer> entry : materialCounts.entrySet()) {
                if (entry.getValue() >= 10) {
                    validCandidates.add(entry.getKey());
                }
            }
            if (validCandidates.isEmpty()) {
                validCandidates.addAll(materialCounts.keySet());
            }

            Material selected = validCandidates.get(new Random().nextInt(validCandidates.size()));
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(selected));
        });
    }
}
