package com.blockdoom.listener;

import com.blockdoom.manager.ConfigManager;
import com.blockdoom.manager.GameManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.Map;

/**
 * Handles player joins, respawns, and dragon death victory checks directly in the server's natural worlds.
 */
public class PlayerListener implements Listener {
    private final GameManager gameManager;
    private final ConfigManager configManager;

    public PlayerListener(GameManager gameManager, ConfigManager configManager) {
        this.gameManager = gameManager;
        this.configManager = configManager;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Map<String, String> enabled = configManager.getEnabledDimensions();
        String overworldName = enabled.getOrDefault("overworld", "world");

        World targetWorld = Bukkit.getWorld(overworldName);
        if (targetWorld != null && !player.getWorld().getName().equals(overworldName)) {
            player.teleportAsync(targetWorld.getSpawnLocation());
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Map<String, String> enabled = configManager.getEnabledDimensions();
        String overworldName = enabled.getOrDefault("overworld", "world");

        World targetWorld = Bukkit.getWorld(overworldName);
        if (targetWorld != null) {
            event.setRespawnLocation(targetWorld.getSpawnLocation());
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof EnderDragon) {
            gameManager.getWinLossManager().handleDragonDeath();
        }
    }
}
