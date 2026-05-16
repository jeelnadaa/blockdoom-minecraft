package com.blockdoom.manager;

import com.blockdoom.BlockDoomPlugin;
import com.blockdoom.util.MessageUtil;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Handles /blockdoom command execution and tab completion.
 */
public class CommandManager implements CommandExecutor, TabCompleter {
    private final BlockDoomPlugin plugin;
    private final GameManager gameManager;
    private final ConfigManager configManager;
    private final StorageManager storageManager;

    public CommandManager(BlockDoomPlugin plugin, GameManager gameManager, ConfigManager configManager, StorageManager storageManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
        this.configManager = configManager;
        this.storageManager = storageManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("blockdoom.admin")) {
            MessageUtil.sendMessage(sender, "<red>You do not have permission to execute BlockDoom commands.</red>");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "start" -> {
                gameManager.start();
                MessageUtil.sendMessage(sender, "<green>BlockDoom gameplay started.</green>");
            }
            case "pause" -> {
                gameManager.pause();
                MessageUtil.sendMessage(sender, "<yellow>BlockDoom gameplay paused.</yellow>");
            }
            case "skip" -> {
                gameManager.skip();
                MessageUtil.sendMessage(sender, "<gold>Skipping to next cycle.</gold>");
            }
            case "regenerate" -> {
                gameManager.regenerate();
            }
            case "status" -> sendStatus(sender);
            case "reload" -> {
                configManager.loadConfig();
                storageManager.loadAll();
                MessageUtil.sendMessage(sender, "<green>BlockDoom configuration and storage reloaded!</green>");
            }
            case "forcestart" -> {
                gameManager.forceStart();
                MessageUtil.sendMessage(sender, "<green>BlockDoom forcefully started.</green>");
            }
            case "forcedelete" -> {
                if (args.length < 2) {
                    MessageUtil.sendMessage(sender, "<red>Usage: /blockdoom forcedelete <material></red>");
                    return true;
                }
                try {
                    Material mat = Material.valueOf(args[1].toUpperCase());
                    gameManager.forceDelete(mat);
                } catch (IllegalArgumentException e) {
                    MessageUtil.sendMessage(sender, "<red>Unknown block material: " + args[1] + "</red>");
                }
            }
            case "ui" -> {
                if (sender instanceof org.bukkit.entity.Player player) {
                    com.blockdoom.gui.ConfigGUI.openMainConfig(player, configManager);
                } else {
                    MessageUtil.sendMessage(sender, "<red>Only in-game players can open the UI.</red>");
                }
            }
            case "blacklist" -> {
                if (args.length < 3) {
                    MessageUtil.sendMessage(sender, "<red>Usage: /blockdoom blacklist <add|remove> <material></red>");
                    return true;
                }
                String action = args[1].toLowerCase();
                try {
                    Material mat = Material.valueOf(args[2].toUpperCase());
                    if (!mat.isBlock()) {
                        MessageUtil.sendMessage(sender, "<red>Material " + mat.name() + " is not a block.</red>");
                        return true;
                    }
                    if (action.equals("add")) {
                        configManager.addBlacklistMaterial(mat);
                        MessageUtil.sendMessage(sender, "<green>Added " + mat.name() + " to blacklist.</green>");
                    } else if (action.equals("remove")) {
                        configManager.removeBlacklistMaterial(mat);
                        MessageUtil.sendMessage(sender, "<green>Removed " + mat.name() + " from blacklist.</green>");
                    } else {
                        MessageUtil.sendMessage(sender, "<red>Unknown action: " + action + "</red>");
                    }
                } catch (IllegalArgumentException e) {
                    MessageUtil.sendMessage(sender, "<red>Unknown material: " + args[2] + "</red>");
                }
            }
            case "config" -> {
                if (args.length < 3) {
                    MessageUtil.sendMessage(sender, "<red>Usage: /blockdoom config <timer|shownext|speed|protect|autoreload> <value></red>");
                    return true;
                }
                String setting = args[1].toLowerCase();
                try {
                    if (setting.equals("shownext")) {
                        boolean val = Boolean.parseBoolean(args[2]);
                        configManager.setShowNextBlockDuringTimer(val);
                        MessageUtil.sendMessage(sender, "<green>Show next block during timer updated to: " + val + "</green>");
                        return true;
                    } else if (setting.equals("protect")) {
                        boolean val = Boolean.parseBoolean(args[2]);
                        configManager.setProtectPlayerBuilds(val);
                        MessageUtil.sendMessage(sender, "<green>Protect player builds updated to: " + val + "</green>");
                        return true;
                    } else if (setting.equals("autoreload")) {
                        boolean val = Boolean.parseBoolean(args[2]);
                        configManager.setAutoReloadOnConfigChange(val);
                        MessageUtil.sendMessage(sender, "<green>Auto reload on config change updated to: " + val + "</green>");
                        return true;
                    }
                    int val = Integer.parseInt(args[2]);
                    if (setting.equals("timer")) {
                        configManager.setTimerDuration(val);
                        MessageUtil.sendMessage(sender, "<green>Timer duration updated to " + val + " seconds.</green>");
                    } else if (setting.equals("speed")) {
                        configManager.setChunksPerTick(val);
                        MessageUtil.sendMessage(sender, "<green>Deletion speed updated to " + val + " chunks/tick.</green>");
                    } else {
                        MessageUtil.sendMessage(sender, "<red>Unknown config setting: " + setting + "</red>");
                    }
                } catch (NumberFormatException e) {
                    MessageUtil.sendMessage(sender, "<red>Value must be a valid integer or boolean.</red>");
                }
            }
            default -> sendHelp(sender);
        }

        return true;
    }

    private void sendHelp(CommandSender sender) {
        MessageUtil.sendMessage(sender, "<gold><bold>--- BlockDoom Commands ---</bold></gold>");
        MessageUtil.sendRawMessage(sender, "<yellow>/blockdoom ui</yellow> <gray>- Opens the in-game configuration GUI menu</gray>");
        MessageUtil.sendRawMessage(sender, "<yellow>/blockdoom start</yellow> <gray>- Starts the game loop</gray>");
        MessageUtil.sendRawMessage(sender, "<yellow>/blockdoom pause</yellow> <gray>- Pauses the game loop</gray>");
        MessageUtil.sendRawMessage(sender, "<yellow>/blockdoom skip</yellow> <gray>- Skips current countdown to reveal</gray>");
        MessageUtil.sendRawMessage(sender, "<yellow>/blockdoom regenerate</yellow> <gray>- Safely wipes and regenerates gameplay worlds</gray>");
        MessageUtil.sendRawMessage(sender, "<yellow>/blockdoom status</yellow> <gray>- Shows active game state and stats</gray>");
        MessageUtil.sendRawMessage(sender, "<yellow>/blockdoom reload</yellow> <gray>- Reloads config.yml and storage</gray>");
        MessageUtil.sendRawMessage(sender, "<yellow>/blockdoom forcestart</yellow> <gray>- Forcefully starts the game</gray>");
        MessageUtil.sendRawMessage(sender, "<yellow>/blockdoom forcedelete <block></yellow> <gray>- Forces immediate deletion of a block</gray>");
        MessageUtil.sendRawMessage(sender, "<yellow>/blockdoom config <timer|shownext|speed|protect|autoreload> <val></yellow> <gray>- Updates config on the fly</gray>");
        MessageUtil.sendRawMessage(sender, "<yellow>/blockdoom blacklist <add|remove> <block></yellow> <gray>- Manages the block blacklist</gray>");
    }

    private void sendStatus(CommandSender sender) {
        MessageUtil.sendMessage(sender, "<gold><bold>--- BlockDoom Status ---</bold></gold>");
        MessageUtil.sendRawMessage(sender, "<yellow>Game State:</yellow> <white>" + gameManager.getState() + "</white>");
        if (gameManager.getActiveDimension() != null) {
            MessageUtil.sendRawMessage(sender, "<yellow>Active Dimension:</yellow> <white>" + gameManager.getActiveDimension().getName() + "</white>");
        }
        if (gameManager.getSelectedMaterial() != null) {
            MessageUtil.sendRawMessage(sender, "<yellow>Target Material:</yellow> <red>" + gameManager.getSelectedMaterial().name() + "</red>");
        }
        MessageUtil.sendRawMessage(sender, "<yellow>Deleted Materials Count:</yellow> <white>" + storageManager.getDeletedMaterials().size() + "</white>");
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("blockdoom.admin")) return null;

        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            List<String> subs = Arrays.asList("ui", "start", "pause", "skip", "regenerate", "status", "reload", "config", "blacklist", "forcestart", "forcedelete");
            for (String s : subs) {
                if (s.startsWith(args[0].toLowerCase())) completions.add(s);
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("config")) {
            List<String> subs = Arrays.asList("timer", "shownext", "speed", "protect", "autoreload");
            for (String s : subs) {
                if (s.startsWith(args[1].toLowerCase())) completions.add(s);
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("config") && 
                  (args[1].equalsIgnoreCase("shownext") || args[1].equalsIgnoreCase("protect") || args[1].equalsIgnoreCase("autoreload"))) {
            List<String> subs = Arrays.asList("true", "false");
            for (String s : subs) {
                if (s.startsWith(args[2].toLowerCase())) completions.add(s);
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("blacklist")) {
            List<String> subs = Arrays.asList("add", "remove");
            for (String s : subs) {
                if (s.startsWith(args[1].toLowerCase())) completions.add(s);
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("blacklist")) {
            for (Material mat : Material.values()) {
                if (mat.isBlock() && mat.name().toLowerCase().startsWith(args[2].toLowerCase())) {
                    completions.add(mat.name());
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("forcedelete")) {
            for (Material mat : Material.values()) {
                if (mat.isBlock() && mat.name().toLowerCase().startsWith(args[1].toLowerCase())) {
                    completions.add(mat.name());
                }
            }
        }
        return completions;
    }
}
