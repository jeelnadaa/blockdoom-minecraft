package com.blockdoom.manager;

import com.blockdoom.BlockDoomPlugin;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles persistence of deleted materials (per dimension and globally) and player-placed protected blocks.
 */
public class StorageManager {
    private final BlockDoomPlugin plugin;
    private final File deletedMaterialsFile;
    private final File placementsFile;

    private final Set<Material> allDeletedMaterials = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Set<Material>> deletedMaterialsPerWorld = new ConcurrentHashMap<>();
    // worldUUID -> (chunkKey -> set of packed block coords)
    private final Map<UUID, Map<Long, Set<Integer>>> protectedBlocks = new ConcurrentHashMap<>();

    public StorageManager(BlockDoomPlugin plugin) {
        this.plugin = plugin;
        this.deletedMaterialsFile = new File(plugin.getDataFolder(), "deleted_materials.yml");
        this.placementsFile = new File(plugin.getDataFolder(), "player_placements.yml");
        loadAll();
    }

    public void loadAll() {
        allDeletedMaterials.clear();
        deletedMaterialsPerWorld.clear();
        if (deletedMaterialsFile.exists()) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(deletedMaterialsFile);
            if (config.contains("global")) {
                List<String> list = config.getStringList("global");
                for (String matName : list) {
                    try {
                        allDeletedMaterials.add(Material.valueOf(matName));
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Unknown material in global deleted list: " + matName);
                    }
                }
            }
            if (config.contains("worlds")) {
                for (String worldKeyStr : config.getConfigurationSection("worlds").getKeys(false)) {
                    try {
                        UUID worldId = UUID.fromString(worldKeyStr);
                        Set<Material> matSet = ConcurrentHashMap.newKeySet();
                        for (String matName : config.getStringList("worlds." + worldKeyStr)) {
                            try {
                                matSet.add(Material.valueOf(matName));
                            } catch (IllegalArgumentException e) {
                                // Invalid material
                            }
                        }
                        deletedMaterialsPerWorld.put(worldId, matSet);
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid world UUID in deleted_materials.yml: " + worldKeyStr);
                    }
                }
            }
        }

        protectedBlocks.clear();
        if (placementsFile.exists()) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(placementsFile);
            for (String worldKeyStr : config.getKeys(false)) {
                try {
                    UUID worldId = UUID.fromString(worldKeyStr);
                    Map<Long, Set<Integer>> chunkMap = new ConcurrentHashMap<>();
                    for (String chunkKeyStr : config.getConfigurationSection(worldKeyStr).getKeys(false)) {
                        long chunkKey = Long.parseLong(chunkKeyStr);
                        List<Integer> list = config.getIntegerList(worldKeyStr + "." + chunkKeyStr);
                        Set<Integer> set = ConcurrentHashMap.newKeySet();
                        set.addAll(list);
                        chunkMap.put(chunkKey, set);
                    }
                    protectedBlocks.put(worldId, chunkMap);
                } catch (IllegalArgumentException | NullPointerException e) {
                    plugin.getLogger().warning("Failed to load placement data for world: " + worldKeyStr);
                }
            }
        }
    }

    public void saveAll() {
        FileConfiguration matConfig = new YamlConfiguration();
        List<String> globalList = new ArrayList<>();
        for (Material mat : allDeletedMaterials) {
            globalList.add(mat.name());
        }
        matConfig.set("global", globalList);

        for (Map.Entry<UUID, Set<Material>> entry : deletedMaterialsPerWorld.entrySet()) {
            List<String> wList = new ArrayList<>();
            for (Material m : entry.getValue()) wList.add(m.name());
            matConfig.set("worlds." + entry.getKey().toString(), wList);
        }
        try {
            matConfig.save(deletedMaterialsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save deleted_materials.yml: " + e.getMessage());
        }

        FileConfiguration placeConfig = new YamlConfiguration();
        for (Map.Entry<UUID, Map<Long, Set<Integer>>> worldEntry : protectedBlocks.entrySet()) {
            String worldKey = worldEntry.getKey().toString();
            for (Map.Entry<Long, Set<Integer>> chunkEntry : worldEntry.getValue().entrySet()) {
                placeConfig.set(worldKey + "." + chunkEntry.getKey(), new ArrayList<>(chunkEntry.getValue()));
            }
        }
        try {
            placeConfig.save(placementsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save player_placements.yml: " + e.getMessage());
        }
    }

    public void resetAll() {
        allDeletedMaterials.clear();
        deletedMaterialsPerWorld.clear();
        protectedBlocks.clear();
        if (deletedMaterialsFile.exists()) deletedMaterialsFile.delete();
        if (placementsFile.exists()) placementsFile.delete();
        saveAll();
    }

    public void addDeletedMaterial(UUID worldId, Material material) {
        allDeletedMaterials.add(material);
        deletedMaterialsPerWorld.computeIfAbsent(worldId, k -> ConcurrentHashMap.newKeySet()).add(material);
        saveAll();
    }

    public boolean isMaterialDeletedGlobally(Material material) {
        return allDeletedMaterials.contains(material);
    }

    public boolean isMaterialDeletedInWorld(UUID worldId, Material material) {
        Set<Material> set = deletedMaterialsPerWorld.get(worldId);
        return set != null && set.contains(material);
    }

    public Set<Material> getDeletedMaterials() {
        return allDeletedMaterials;
    }

    public Set<Material> getDeletedMaterialsForWorld(UUID worldId) {
        return deletedMaterialsPerWorld.getOrDefault(worldId, Collections.emptySet());
    }

    public boolean isProtected(UUID worldId, int x, int y, int z) {
        Map<Long, Set<Integer>> chunkMap = protectedBlocks.get(worldId);
        if (chunkMap == null) return false;
        long chunkKey = ((long) (x >> 4) << 32) | ((z >> 4) & 0xFFFFFFFFL);
        Set<Integer> set = chunkMap.get(chunkKey);
        if (set == null) return false;
        int packed = packBlockPos(x, y, z);
        return set.contains(packed);
    }

    public void addProtectedBlock(UUID worldId, int x, int y, int z) {
        protectedBlocks.computeIfAbsent(worldId, k -> new ConcurrentHashMap<>())
                       .computeIfAbsent(((long) (x >> 4) << 32) | ((z >> 4) & 0xFFFFFFFFL), k -> ConcurrentHashMap.newKeySet())
                       .add(packBlockPos(x, y, z));
    }

    public void removeProtectedBlock(UUID worldId, int x, int y, int z) {
        Map<Long, Set<Integer>> chunkMap = protectedBlocks.get(worldId);
        if (chunkMap == null) return;
        long chunkKey = ((long) (x >> 4) << 32) | ((z >> 4) & 0xFFFFFFFFL);
        Set<Integer> set = chunkMap.get(chunkKey);
        if (set == null) return;
        set.remove(packBlockPos(x, y, z));
        if (set.isEmpty()) {
            chunkMap.remove(chunkKey);
        }
    }

    private static int packBlockPos(int x, int y, int z) {
        return ((y + 64) << 8) | ((x & 15) << 4) | (z & 15);
    }
}
