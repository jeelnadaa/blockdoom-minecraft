package com.blockdoom.gui;

import com.blockdoom.manager.ConfigManager;
import com.blockdoom.util.MessageUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Helper class to construct and open in-game configuration and blacklist GUIs.
 */
public final class ConfigGUI {

    private ConfigGUI() {}

    public static void openMainConfig(Player player, ConfigManager configManager) {
        ConfigUIHolder holder = new ConfigUIHolder();
        Inventory inv = Bukkit.createInventory(holder, 27, MessageUtil.format("<dark_red><bold>BlockDoom Configuration</bold></dark_red>"));
        holder.setInventory(inv);

        // Border Glass
        ItemStack glass = createItem(Material.BLACK_STAINED_GLASS_PANE, "<gray> </gray>");
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, glass);

        // Slot 11: Timer Duration
        List<String> timerLore = List.of(
                "<gray>Current: <gold><bold>" + configManager.getTimerDuration() + "s</bold></gold></gray>",
                "",
                "<yellow>Left-Click: <green>+10s</green></yellow>",
                "<yellow>Right-Click: <red>-10s</red></yellow>",
                "<yellow>Shift-Left: <green>+60s</green></yellow>",
                "<yellow>Shift-Right: <red>-60s</red></yellow>"
        );
        inv.setItem(11, createItem(Material.CLOCK, "<gold><bold>Timer Duration</bold></gold>", timerLore));

        // Slot 12: Show Next Block
        boolean showNext = configManager.isShowNextBlockDuringTimer();
        String showNextStr = showNext ? "<green><bold>TRUE</bold></green>" : "<red><bold>FALSE</bold></red>";
        List<String> showLore = List.of(
                "<gray>Current: " + showNextStr + "</gray>",
                "",
                "<yellow>Click to toggle setting</yellow>"
        );
        Material showMat = showNext ? Material.REDSTONE_TORCH : Material.LEVER;
        inv.setItem(12, createItem(showMat, "<yellow><bold>Show Next Block Early</bold></yellow>", showLore));

        // Slot 13: Protect Player Builds
        boolean protect = configManager.isProtectPlayerBuilds();
        String protectStr = protect ? "<green><bold>TRUE</bold></green>" : "<red><bold>FALSE</bold></red>";
        List<String> protectLore = List.of(
                "<gray>Current: " + protectStr + "</gray>",
                "",
                "<yellow>Click to toggle protection of player placed blocks</yellow>"
        );
        inv.setItem(13, createItem(Material.SHIELD, "<yellow><bold>Protect Player Builds</bold></yellow>", protectLore));

        // Slot 14: Chunks Per Tick
        List<String> chunksLore = List.of(
                "<gray>Current: <light_purple><bold>" + configManager.getChunksPerTick() + " chunks/tick</bold></light_purple></gray>",
                "",
                "<yellow>Left-Click: <green>+5</green></yellow>",
                "<yellow>Right-Click: <red>-5</red></yellow>"
        );
        inv.setItem(14, createItem(Material.MINECART, "<light_purple><bold>Deletion Speed (Chunks/Tick)</bold></light_purple>", chunksLore));

        // Slot 15: Auto Reload Config
        boolean autoReload = configManager.isAutoReloadOnConfigChange();
        String autoStr = autoReload ? "<green><bold>TRUE</bold></green>" : "<red><bold>FALSE</bold></red>";
        List<String> autoLore = List.of(
                "<gray>Current: " + autoStr + "</gray>",
                "",
                "<yellow>Click to toggle auto-reload on config changes</yellow>"
        );
        inv.setItem(15, createItem(Material.REPEATER, "<yellow><bold>Auto Reload Config</bold></yellow>", autoLore));

        // Slot 16: Player Selection Range
        int range = configManager.getSelectionRange();
        String rangeStr = range == 0 ? "<aqua><bold>Unlimited</bold></aqua>" : "<aqua><bold>" + range + " chunks</bold></aqua>";
        List<String> rangeLore = List.of(
                "<gray>Current: " + rangeStr + "</gray>",
                "<gray>If unlimited, scans all loaded chunks.</gray>",
                "",
                "<yellow>Left-Click: <green>+1 chunk</green></yellow>",
                "<yellow>Right-Click: <red>-1 chunk</red></yellow>",
                "<yellow>Shift-Left: <green>+4 chunks</green></yellow>",
                "<yellow>Shift-Right: <red>-4 chunks</red></yellow>"
        );
        inv.setItem(16, createItem(Material.COMPASS, "<aqua><bold>Player Selection Range (Chunks)</bold></aqua>", rangeLore));

        // Slot 22: Manage Blacklist Button
        List<String> blLore = List.of(
                "<gray>Total Blacklisted: <red><bold>" + configManager.getBlacklist().size() + " blocks</bold></red></gray>",
                "",
                "<yellow>Click to view and edit blacklisted blocks</yellow>"
        );
        inv.setItem(22, createItem(Material.BARRIER, "<red><bold>Manage Blacklisted Blocks</bold></red>", blLore));

        player.openInventory(inv);
    }

    public static void openBlacklistGUI(Player player, ConfigManager configManager, int page) {
        BlacklistUIHolder holder = new BlacklistUIHolder(page);
        Inventory inv = Bukkit.createInventory(holder, 54, MessageUtil.format("<dark_red><bold>Blacklist (Page " + (page + 1) + ")</bold></dark_red>"));
        holder.setInventory(inv);

        List<Material> blacklisted = new ArrayList<>(configManager.getBlacklist());
        blacklisted.sort(Comparator.comparing(Material::name));

        int itemsPerPage = 45;
        int startIndex = page * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, blacklisted.size());

        for (int i = startIndex; i < endIndex; i++) {
            Material mat = blacklisted.get(i);
            int slot = i - startIndex;
            List<String> lore = List.of(
                    "<red>Click to REMOVE from blacklist</red>"
            );
            inv.setItem(slot, createItem(mat, "<gold><bold>" + mat.name() + "</bold></gold>", lore));
        }

        // Bottom border
        ItemStack glass = createItem(Material.BLACK_STAINED_GLASS_PANE, "<gray> </gray>");
        for (int i = 45; i < 54; i++) inv.setItem(i, glass);

        if (page > 0) {
            inv.setItem(45, createItem(Material.ARROW, "<yellow><bold>Previous Page</bold></yellow>"));
        }
        inv.setItem(49, createItem(Material.OAK_DOOR, "<green><bold>Back to Config Menu</bold></green>"));
        if (endIndex < blacklisted.size()) {
            inv.setItem(53, createItem(Material.ARROW, "<yellow><bold>Next Page</bold></yellow>"));
        }

        player.openInventory(inv);
    }

    private static ItemStack createItem(Material material, String name) {
        return createItem(material, name, List.of());
    }

    private static ItemStack createItem(Material material, String name, List<String> loreStrings) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MessageUtil.format(name));
            List<Component> lore = new ArrayList<>();
            for (String s : loreStrings) {
                lore.add(MessageUtil.format(s));
            }
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
}
