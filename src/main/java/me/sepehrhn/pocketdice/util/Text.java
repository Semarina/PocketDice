// File: src/main/java/me/sepehrhn/pocketdice/util/Text.java
package me.sepehrhn.pocketdice.util;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

/** Simple text utilities for placeholder replacement and coloring. */
public final class Text {
    private Text() {}

    /** Replace {placeholders} in the template with provided pairs. */
    public static String format(String template, String... kvPairs) {
        if (template == null) return "";
        String out = template;
        for (int i = 0; i + 1 < kvPairs.length; i += 2) {
            String key = kvPairs[i];
            String val = kvPairs[i + 1];
            out = out.replace("{" + key + "}", val);
        }
        return out;
    }

    /** Translate & color codes to Bukkit colors. */
    public static String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    /** Send a formatted message using error_format from config (used for status + errors). */
    public static void sendError(JavaPlugin plugin, CommandSender sender, String message) {
        String fmt = plugin.getConfig().getString("error_format", "[PocketDice] {message}");
        sender.sendMessage(color(format(fmt, "message", message)));
    }
}