package com.blockdoom.manager;

import com.blockdoom.BlockDoomPlugin;
import com.blockdoom.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Handles custom world creation, unloads, deletion, and fresh regeneration.
 */
public class WorldRegenerationManager {
    private final BlockDoomPlugin plugin;
    private final ConfigManager configManager;
    private final StorageManager storageManager;

    private boolean isRegenerating = false;

    public WorldRegenerationManager(BlockDoomPlugin plugin, ConfigManager configManager, StorageManager storageManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.storageManager = storageManager;
    }

    public void initializeWorlds() {
        Map<String, String> enabled = configManager.getEnabledDimensions();
        createWorldIfNotExists(enabled.getOrDefault("overworld", "blockdoom_overworld"), World.Environment.NORMAL);
        createWorldIfNotExists(enabled.getOrDefault("nether", "blockdoom_nether"), World.Environment.NETHER);
        createWorldIfNotExists(enabled.getOrDefault("end", "blockdoom_end"), World.Environment.THE_END);
    }

    public boolean isRegenerating() {
        return isRegenerating;
    }

    public void regenerateWorlds(Runnable onComplete) {
        if (isRegenerating) {
            return;
        }
        isRegenerating = true;
        MessageUtil.broadcast("<yellow><bold>WORLD REGENERATION:</bold></yellow> Commencing safe wipe and creation of fresh gameplay worlds...");

        Map<String, String> enabled = configManager.getEnabledDimensions();
        String overworldName = enabled.getOrDefault("overworld", "blockdoom_overworld");
        String netherName = enabled.getOrDefault("nether", "blockdoom_nether");
        String endName = enabled.getOrDefault("end", "blockdoom_end");

        World rootWorld = Bukkit.getWorlds().get(0);
        if (rootWorld == null) {
            isRegenerating = false;
            plugin.getLogger().severe("Cannot find root server world for fallback!");
            return;
        }

        // 1. Teleport all players to root world spawn safely before unloading
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.teleportAsync(rootWorld.getSpawnLocation());
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // 2. Unload custom worlds
            unloadAndDeleteWorld(overworldName);
            unloadAndDeleteWorld(netherName);
            unloadAndDeleteWorld(endName);

            // 3. Reset persistent storage
            storageManager.resetAll();

            // 4. Create fresh worlds
            World overworld = createWorldIfNotExists(overworldName, World.Environment.NORMAL);
            createWorldIfNotExists(netherName, World.Environment.NETHER);
            createWorldIfNotExists(endName, World.Environment.THE_END);

            // 5. Teleport players to new overworld spawn
            if (overworld != null) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.teleportAsync(overworld.getSpawnLocation());
                }
            }

            isRegenerating = false;
            MessageUtil.broadcast("<green><bold>SUCCESS:</bold></green> Worlds regenerated successfully! All data wiped. Run <gold>/blockdoom start</gold> to begin!");
            if (onComplete != null) {
                onComplete.run();
            }
        }, 40L); // Wait 2 seconds for teleports to settle
    }

    private World createWorldIfNotExists(String name, World.Environment environment) {
        World world = Bukkit.getWorld(name);
        if (world == null) {
            plugin.getLogger().info("Generating world: " + name + " (" + environment + ")...");
            world = Bukkit.createWorld(new WorldCreator(name).environment(environment));
        }
        return world;
    }

    private void unloadAndDeleteWorld(String name) {
        World world = Bukkit.getWorld(name);
        if (world != null) {
            Bukkit.unloadWorld(world, false);
        }
        File folder = new File(Bukkit.getWorldContainer(), name);
        if (folder.exists()) {
            deleteFolderRecursively(folder);
        }
    }

    private void deleteFolderRecursively(File folder) {
        try (Stream<Path> walk = Files.walk(folder.toPath())) {
            walk.sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to delete world folder: " + folder.getName());
        }
    }
}
