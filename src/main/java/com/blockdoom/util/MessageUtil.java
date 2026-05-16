package com.blockdoom.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Utility for formatting and sending rich Adventure messages.
 */
public final class MessageUtil {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static String prefix = "<dark_gray>[<red><bold>BlockDoom</bold></red>]</dark_gray> ";

    private MessageUtil() {}

    public static void setPrefix(String newPrefix) {
        if (newPrefix != null) {
            prefix = newPrefix;
        }
    }

    public static Component format(String message) {
        return MINI_MESSAGE.deserialize(message);
    }

    public static Component formatWithPrefix(String message) {
        return MINI_MESSAGE.deserialize(prefix + message);
    }

    public static void sendMessage(CommandSender sender, String message) {
        sender.sendMessage(formatWithPrefix(message));
    }

    public static void sendRawMessage(CommandSender sender, String message) {
        sender.sendMessage(format(message));
    }

    public static void broadcast(String message) {
        Component comp = formatWithPrefix(message);
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(comp);
        }
        Bukkit.getConsoleSender().sendMessage(comp);
    }
}
