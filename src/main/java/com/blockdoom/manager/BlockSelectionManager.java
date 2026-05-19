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

        // Gather chunk snapshots directly around all active players in this dimension
        if (aroundPlayers && range > 0 && !players.isEmpty()) {
            for (Player player : players) {
                int px = player.getLocation().getBlockX() >> 4;
                int pz = player.getLocation().getBlockZ() >> 4;
                for (int dx = -range; dx <= range; dx++) {
                    for (int dz = -range; dz <= range; dz++) {
                        int cx = px + dx;
                        int cz = pz + dz;
                        try {
                            snapshots.add(dimension.getChunkAt(cx, cz).getChunkSnapshot());
                        } catch (Exception e) {
                            // Safety catch in case coordinate is out of bounds
                        }
                    }
                }
            }
        } else {
            // No player range restriction, gather all loaded chunks
            for (Chunk chunk : dimension.getLoadedChunks()) {
                snapshots.add(chunk.getChunkSnapshot());
            }
        }

        // Fallback: If snapshots set is empty, scan all loaded chunks in this dimension
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
                        for (int y = minY; y < maxY; y++) {
                            if (configManager.isProtectPlayerBuilds() && storageManager.isProtected(worldId, worldX, y, worldZ)) {
                                continue;
                            }
                            Material mat = snapshot.getBlockType(x, y, z);
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

            // Cross-check: Ensure the material belongs to this dimension's environment
            World.Environment env = dimension.getEnvironment();
            validCandidates.removeIf(mat -> !belongsToEnvironment(mat, env));

            // If all candidates were filtered out by cross-check, fallback to allowing any from the original list
            if (validCandidates.isEmpty()) {
                for (Map.Entry<Material, Integer> entry : materialCounts.entrySet()) {
                    if (entry.getValue() >= 10) {
                        validCandidates.add(entry.getKey());
                    }
                }
                if (validCandidates.isEmpty()) {
                    validCandidates.addAll(materialCounts.keySet());
                }
            }

            if (validCandidates.isEmpty()) {
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(null));
                return;
            }

            Material selected = validCandidates.get(new Random().nextInt(validCandidates.size()));
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(selected));
        });
    }

    public boolean belongsToEnvironment(Material material, World.Environment env) {
        String name = material.name();

        boolean isNetherExclusive = name.contains("NETHER") || name.contains("SOUL") || 
                                    name.contains("CRIMSON") || name.contains("WARPED") || 
                                    name.contains("BASALT") || name.contains("BLACKSTONE") || 
                                    name.equals("GLOWSTONE") || name.contains("QUARTZ") || 
                                    name.equals("ANCIENT_DEBRIS") || name.contains("NYLIUM") || 
                                    name.equals("SHROOMLIGHT") || name.equals("MAGMA_BLOCK") || 
                                    name.contains("GILDED_BLACKSTONE") || name.equals("CRYING_OBSIDIAN");

        boolean isEndExclusive = name.startsWith("END_") || name.contains("CHORUS") || 
                                 name.contains("PURPUR") || name.equals("ELYTRA") || 
                                 name.equals("DRAGON_EGG");

        if (env == World.Environment.NETHER) {
            return !isEndExclusive;
        } else if (env == World.Environment.THE_END) {
            return !isNetherExclusive;
        } else {
            return !isNetherExclusive && !isEndExclusive;
        }
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
                            if (blacklist.contains(mat) || storageManager.isMaterialDeletedInWorld(worldId, mat) || !belongsToEnvironment(mat, dimension.getEnvironment())) {
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
