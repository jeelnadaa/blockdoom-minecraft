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
 * Asynchronously scans all loaded chunk snapshots across the dimension to select valid natural blocks.
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

    public void selectBlockAsync(World dimension, boolean aroundPlayers, Consumer<Material> callback) {
        Set<ChunkSnapshot> snapshots = new HashSet<>();
        UUID worldId = dimension.getUID();
        int range = configManager.getSelectionRange();
        List<Player> players = dimension.getPlayers();

        // Gather chunk snapshots of chunks within the selection-range (chunks) of any player in this dimension
        for (Chunk chunk : dimension.getLoadedChunks()) {
            boolean inRange = false;
            if (!aroundPlayers || range <= 0 || players.isEmpty()) {
                inRange = true;
            } else {
                int cx = chunk.getX();
                int cz = chunk.getZ();
                for (Player player : players) {
                    int px = player.getLocation().getBlockX() >> 4;
                    int pz = player.getLocation().getBlockZ() >> 4;
                    if (Math.abs(cx - px) <= range && Math.abs(cz - pz) <= range) {
                        inRange = true;
                        break;
                    }
                }
            }
            if (inRange) {
                snapshots.add(chunk.getChunkSnapshot());
            }
        }

        if (snapshots.isEmpty()) {
            for (Chunk chunk : dimension.getLoadedChunks()) {
                snapshots.add(chunk.getChunkSnapshot());
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
                        // Exact block inspection across Y column
                        for (int y = minY; y < maxY; y++) {
                            if (configManager.isProtectPlayerBuilds() && storageManager.isProtected(worldId, worldX, y, worldZ)) {
                                continue;
                            }
                            Material mat = snapshot.getBlockType(x, y, z);
                            // Strictly filter to solid blocks (excludes flowers, grass, kelp, vines, fluids, air)
                            if (!mat.isBlock() || !mat.isSolid() || mat.isAir()) {
                                continue;
                            }
                            if (blacklist.contains(mat) || storageManager.isMaterialDeletedInWorld(worldId, mat)) {
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

    public void getRemainingBlocksAsync(World dimension, Consumer<Set<Material>> callback) {
        Set<ChunkSnapshot> snapshots = new HashSet<>();
        UUID worldId = dimension.getUID();

        for (Chunk chunk : dimension.getLoadedChunks()) {
            snapshots.add(chunk.getChunkSnapshot());
        }

        if (snapshots.isEmpty()) {
            callback.accept(Collections.emptySet());
            return;
        }

        Set<Material> blacklist = configManager.getBlacklist();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Set<Material> remaining = new HashSet<>();

            for (ChunkSnapshot snapshot : snapshots) {
                int sX = snapshot.getX();
                int sZ = snapshot.getZ();
                int minY = dimension.getMinHeight();
                int maxY = dimension.getMaxHeight();

                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        for (int y = minY; y < maxY; y++) {
                            Material mat = snapshot.getBlockType(x, y, z);
                            if (!mat.isBlock() || !mat.isSolid() || mat.isAir()) {
                                continue;
                            }
                            if (blacklist.contains(mat) || storageManager.isMaterialDeletedInWorld(worldId, mat)) {
                                continue;
                            }
                            remaining.add(mat);
                        }
                    }
                }
            }

            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(remaining));
        });
    }
}
