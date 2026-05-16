package com.blockdoom.gui;

import com.blockdoom.manager.ConfigManager;
import com.blockdoom.util.MessageUtil;
import com.blockdoom.util.SoundUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Listens to UI clicks in the in-game configuration and blacklist menus.
 */
public class GUIListener implements Listener {
    private final ConfigManager configManager;

    public GUIListener(ConfigManager configManager) {
        this.configManager = configManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (event.getInventory().getHolder() instanceof ConfigUIHolder) {
            event.setCancelled(true);
            if (event.getClickedInventory() == null || event.getRawSlot() >= event.getInventory().getSize()) {
                return;
            }

            ClickType click = event.getClick();
            int slot = event.getRawSlot();

            if (slot == 10) { // Timer Duration
                int current = configManager.getTimerDuration();
                if (click == ClickType.SHIFT_LEFT) current += 60;
                else if (click == ClickType.SHIFT_RIGHT) current -= 60;
                else if (click == ClickType.LEFT) current += 10;
                else if (click == ClickType.RIGHT) current -= 10;
                
                configManager.setTimerDuration(current);
                SoundUtil.playSound(player, "UI_BUTTON_CLICK", 0.5f, 1.2f);
                MessageUtil.sendMessage(player, "<green>Timer duration changed to <gold><bold>" + configManager.getTimerDuration() + "s</bold></gold></green>");
                ConfigGUI.openMainConfig(player, configManager);
            } else if (slot == 11) { // Auto Reload Toggle
                boolean current = configManager.isAutoReloadOnConfigChange();
                boolean next = !current;
                configManager.setAutoReloadOnConfigChange(next);
                SoundUtil.playSound(player, "UI_BUTTON_CLICK", 0.5f, 1.2f);
                MessageUtil.sendMessage(player, "<green>Auto-reload on config change updated to: <gold><bold>" + next + "</bold></gold></green>");
                ConfigGUI.openMainConfig(player, configManager);
            } else if (slot == 12) { // Scan Radius
                int current = configManager.getScanRadius();
                if (click == ClickType.LEFT || click == ClickType.SHIFT_LEFT) current += 1;
                else if (click == ClickType.RIGHT || click == ClickType.SHIFT_RIGHT) current -= 1;

                configManager.setScanRadius(current);
                SoundUtil.playSound(player, "UI_BUTTON_CLICK", 0.5f, 1.2f);
                MessageUtil.sendMessage(player, "<green>Scan radius changed to <gold><bold>" + configManager.getScanRadius() + " chunks</bold></gold></green>");
                ConfigGUI.openMainConfig(player, configManager);
            } else if (slot == 14) { // Show Next Block Toggle
                boolean current = configManager.isShowNextBlockDuringTimer();
                boolean next = !current;
                configManager.setShowNextBlockDuringTimer(next);
                SoundUtil.playSound(player, "UI_BUTTON_CLICK", 0.5f, 1.2f);
                MessageUtil.sendMessage(player, "<green>Show next block early updated to: <gold><bold>" + next + "</bold></gold></green>");
                ConfigGUI.openMainConfig(player, configManager);
            } else if (slot == 15) { // Protect Player Builds Toggle
                boolean current = configManager.isProtectPlayerBuilds();
                boolean next = !current;
                configManager.setProtectPlayerBuilds(next);
                SoundUtil.playSound(player, "UI_BUTTON_CLICK", 0.5f, 1.2f);
                MessageUtil.sendMessage(player, "<green>Protect player builds updated to: <gold><bold>" + next + "</bold></gold></green>");
                ConfigGUI.openMainConfig(player, configManager);
            } else if (slot == 16) { // Chunks per tick
                int current = configManager.getChunksPerTick();
                if (click == ClickType.LEFT || click == ClickType.SHIFT_LEFT) current += 5;
                else if (click == ClickType.RIGHT || click == ClickType.SHIFT_RIGHT) current -= 5;

                configManager.setChunksPerTick(current);
                SoundUtil.playSound(player, "UI_BUTTON_CLICK", 0.5f, 1.2f);
                MessageUtil.sendMessage(player, "<green>Deletion speed changed to <gold><bold>" + configManager.getChunksPerTick() + " chunks/tick</bold></gold></green>");
                ConfigGUI.openMainConfig(player, configManager);
            } else if (slot == 22) { // Manage Blacklist Button
                SoundUtil.playSound(player, "UI_BUTTON_CLICK", 0.5f, 1.0f);
                ConfigGUI.openBlacklistGUI(player, configManager, 0);
            }

        } else if (event.getInventory().getHolder() instanceof BlacklistUIHolder holder) {
            event.setCancelled(true);
            if (event.getClickedInventory() == null) return;

            int slot = event.getRawSlot();
            int page = holder.getPage();

            if (slot < 54) { // Top GUI Inventory
                if (slot == 45 && page > 0) {
                    SoundUtil.playSound(player, "UI_BUTTON_CLICK", 0.5f, 1.0f);
                    ConfigGUI.openBlacklistGUI(player, configManager, page - 1);
                } else if (slot == 49) {
                    SoundUtil.playSound(player, "UI_BUTTON_CLICK", 0.5f, 1.0f);
                    ConfigGUI.openMainConfig(player, configManager);
                } else if (slot == 53) {
                    SoundUtil.playSound(player, "UI_BUTTON_CLICK", 0.5f, 1.0f);
                    ConfigGUI.openBlacklistGUI(player, configManager, page + 1);
                } else if (slot < 45) {
                    ItemStack item = event.getInventory().getItem(slot);
                    if (item != null && item.getType() != Material.AIR) {
                        Material mat = item.getType();
                        configManager.removeBlacklistMaterial(mat);
                        SoundUtil.playSound(player, "ENTITY_ITEM_BREAK", 0.5f, 0.8f);
                        MessageUtil.sendMessage(player, "<green>Removed <gold>" + mat.name() + "</gold> from blacklist!</green>");
                        ConfigGUI.openBlacklistGUI(player, configManager, page);
                    }
                }
            } else { // Bottom Player Inventory
                ItemStack item = event.getCurrentItem();
                if (item != null && item.getType() != Material.AIR) {
                    Material mat = item.getType();
                    if (mat.isBlock()) {
                        configManager.addBlacklistMaterial(mat);
                        SoundUtil.playSound(player, "ENTITY_PLAYER_LEVELUP", 0.5f, 1.5f);
                        MessageUtil.sendMessage(player, "<green>Added <gold>" + mat.name() + "</gold> to blacklist!</green>");
                        ConfigGUI.openBlacklistGUI(player, configManager, page);
                    } else {
                        MessageUtil.sendMessage(player, "<red>Only block items can be blacklisted!</red>");
                    }
                }
            }
        }
    }
}
