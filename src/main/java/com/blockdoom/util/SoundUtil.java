package com.blockdoom.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/**
 * Utility for playing configured sounds to players.
 */
public final class SoundUtil {

    private SoundUtil() {}

    public static void playSound(Player player, String soundName, float volume, float pitch) {
        if (soundName == null || soundName.equalsIgnoreCase("none") || soundName.isEmpty()) {
            return;
        }
        try {
            Sound sound = Sound.valueOf(soundName.toUpperCase());
            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (IllegalArgumentException e) {
            // Sound name is invalid or custom
        }
    }

    public static void broadcastSound(String soundName, float volume, float pitch) {
        if (soundName == null || soundName.equalsIgnoreCase("none") || soundName.isEmpty()) {
            return;
        }
        try {
            Sound sound = Sound.valueOf(soundName.toUpperCase());
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.playSound(player.getLocation(), sound, volume, pitch);
            }
        } catch (IllegalArgumentException e) {
            // Sound name is invalid
        }
    }

    public static void playSoundAt(Location location, String soundName, float volume, float pitch) {
        if (soundName == null || soundName.equalsIgnoreCase("none") || soundName.isEmpty() || location.getWorld() == null) {
            return;
        }
        try {
            Sound sound = Sound.valueOf(soundName.toUpperCase());
            location.getWorld().playSound(location, sound, volume, pitch);
        } catch (IllegalArgumentException e) {
            // Sound name is invalid
        }
    }
}
