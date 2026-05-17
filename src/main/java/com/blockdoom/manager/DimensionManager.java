package com.blockdoom.manager;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Manages probabilistic dimension selection based on active player distribution.
 */
public class DimensionManager {
    private final ConfigManager configManager;
    private final Random random = new Random();

    public DimensionManager(ConfigManager configManager) {
        this.configManager = configManager;
    }

    public World selectDimension() {
        Map<String, String> enabled = configManager.getEnabledDimensions();
        Map<World, Integer> counts = new HashMap<>();
        int totalPlayers = 0;

        for (Player player : Bukkit.getOnlinePlayers()) {
            World world = player.getWorld();
            for (String worldName : enabled.values()) {
                if (world.getName().equalsIgnoreCase(worldName)) {
                    counts.put(world, counts.getOrDefault(world, 0) + 1);
                    totalPlayers++;
                    break;
                }
            }
        }

        if (totalPlayers == 0 || counts.isEmpty()) {
            String overworldName = enabled.getOrDefault("overworld", "world");
            World overworld = Bukkit.getWorld(overworldName);
            if (overworld != null) {
                return overworld;
            }
            return Bukkit.getWorlds().get(0);
        }

        int target = random.nextInt(totalPlayers);
        int current = 0;
        for (Map.Entry<World, Integer> entry : counts.entrySet()) {
            current += entry.getValue();
            if (target < current) {
                return entry.getKey();
            }
        }

        return counts.keySet().iterator().next();
    }
}
