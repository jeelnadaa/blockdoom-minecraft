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
 * Handles persistence of deleted materials and player-placed protected blocks.
 */
public class StorageManager {
    private final BlockDoomPlugin plugin;
    private final File deletedMaterialsFile;
    private final File placementsFile;

    private final Set<Material> deletedMaterials = ConcurrentHashMap.newKeySet();
    // worldUUID -> (chunkKey -> set of packed block coords)
    private final Map<UUID, Map<Long, Set<Integer>>> protectedBlocks = new ConcurrentHashMap<>();

    public StorageManager(BlockDoomPlugin plugin) {
        this.plugin = plugin;
        this.deletedMaterialsFile = new File(plugin.getDataFolder(), "deleted_materials.yml");
        this.placementsFile = new File(plugin.getDataFolder(), "player_placements.yml");
        loadAll();
    }

    public void loadAll() {
        deletedMaterials.clear();
        if (deletedMaterialsFile.exists()) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(deletedMaterialsFile);
            List<String> list = config.getStringList("deleted");
            for (String matName : list) {
                try {
                    deletedMaterials.add(Material.valueOf(matName));
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Unknown material in deleted_materials.yml: " + matName);
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
        List<String> matList = new ArrayList<>();
        for (Material mat : deletedMaterials) {
            matList.add(mat.name());
        }
        matConfig.set("deleted", matList);
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
        deletedMaterials.clear();
        protectedBlocks.clear();
        if (deletedMaterialsFile.exists()) deletedMaterialsFile.delete();
        if (placementsFile.exists()) placementsFile.delete();
        saveAll();
    }

    public void addDeletedMaterial(Material material) {
        deletedMaterials.add(material);
        saveAll();
    }

    public boolean isMaterialDeleted(Material material) {
        return deletedMaterials.contains(material);
    }

    public Set<Material> getDeletedMaterials() {
        return deletedMaterials;
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
