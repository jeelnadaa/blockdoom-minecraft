package com.blockdoom.manager;

import com.blockdoom.util.MessageUtil;
import com.blockdoom.util.SoundUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.time.Duration;

/**
 * Handles UI broadcasts including ActionBar countdowns, Titles, and Sound effects.
 */
public class UIManager {
    private final ConfigManager configManager;

    public UIManager(ConfigManager configManager) {
        this.configManager = configManager;
    }

    public void broadcastTimerTick(int remainingSeconds) {
        String timeFormatted = formatTime(remainingSeconds);
        String rawMsg = configManager.getActionbarTimer().replace("%time%", timeFormatted);
        Component comp = MessageUtil.format(rawMsg);

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendActionBar(comp);
        }

        // Play subtle timer tick sound when countdown is low (e.g., last 5 seconds)
        if (remainingSeconds > 0 && remainingSeconds <= 5) {
            SoundUtil.broadcastSound(configManager.getSoundTimerTick(), 0.8f, 1.2f);
        }
    }

    public void broadcastRevealingTick(Material material, int remainingRevealSeconds) {
        String matName = formatMaterialName(material);
        String rawMsg = configManager.getActionbarRevealing()
                .replace("%block%", matName)
                .replace("%time%", String.valueOf(remainingRevealSeconds));
        Component comp = MessageUtil.format(rawMsg);

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendActionBar(comp);
        }
    }

    public void broadcastPaused() {
        Component comp = MessageUtil.format(configManager.getActionbarPaused());
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendActionBar(comp);
        }
    }

    public void broadcastReveal(Material material) {
        String matName = formatMaterialName(material);
        Component titleComp = MessageUtil.format(configManager.getTitleReveal().replace("%block%", matName));
        Component subComp = MessageUtil.format(configManager.getSubtitleReveal().replace("%block%", matName));

        Title title = Title.title(titleComp, subComp, Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(4), Duration.ofMillis(500)));
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.showTitle(title);
        }
        SoundUtil.broadcastSound(configManager.getSoundReveal(), 1.0f, 0.9f);
        MessageUtil.broadcast("<red><bold>WARNING:</bold></red> Selected block <gold><bold>" + matName + "</bold></gold> will be permanently erased in 5 seconds!");
    }

    public void broadcastDeletionStart(Material material) {
        String matName = formatMaterialName(material);
        Component titleComp = MessageUtil.format(configManager.getTitleDeletionStart().replace("%block%", matName));
        Component subComp = MessageUtil.format(configManager.getSubtitleDeletionStart().replace("%block%", matName));

        Title title = Title.title(titleComp, subComp, Title.Times.times(Duration.ofMillis(250), Duration.ofSeconds(3), Duration.ofMillis(500)));
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.showTitle(title);
        }
        SoundUtil.broadcastSound(configManager.getSoundDeletionStart(), 1.0f, 0.7f);
        MessageUtil.broadcast("<dark_red><bold>DISINTEGRATION:</bold></dark_red> Natural instances of <gold><bold>" + matName + "</bold></gold> are now being erased from active dimensions!");
    }

    public void broadcastVictory() {
        Component titleComp = MessageUtil.format(configManager.getTitleVictory());
        Component subComp = MessageUtil.format(configManager.getSubtitleVictory());

        Title title = Title.title(titleComp, subComp, Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(5), Duration.ofSeconds(1)));
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.showTitle(title);
        }
        SoundUtil.broadcastSound(configManager.getSoundVictory(), 1.0f, 1.0f);
        MessageUtil.broadcast("<gold><bold>CONGRATULATIONS!</bold></gold> The Ender Dragon has been defeated! The chaos of BlockDoom has ended in VICTORY!");
    }

    public void broadcastDefeat() {
        Component titleComp = MessageUtil.format(configManager.getTitleDefeat());
        Component subComp = MessageUtil.format(configManager.getSubtitleDefeat());

        Title title = Title.title(titleComp, subComp, Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(5), Duration.ofSeconds(1)));
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.showTitle(title);
        }
        SoundUtil.broadcastSound(configManager.getSoundDefeat(), 1.0f, 0.5f);
        MessageUtil.broadcast("<dark_red><bold>GAME OVER:</bold></dark_red> Progression has become impossible. The chaos of BlockDoom has consumed the world!");
    }

    private static String formatTime(int totalSeconds) {
        int mins = totalSeconds / 60;
        int secs = totalSeconds % 60;
        return String.format("%02d:%02d", mins, secs);
    }

    private static String formatMaterialName(Material material) {
        String name = material.name().replace("_", " ").toLowerCase();
        StringBuilder capitalized = new StringBuilder();
        for (String word : name.split(" ")) {
            if (!word.isEmpty()) {
                capitalized.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(" ");
            }
        }
        return capitalized.toString().trim();
    }
}
