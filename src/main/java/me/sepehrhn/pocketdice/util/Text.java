// File: src/main/java/me/sepehrhn/pocketdice/util/Text.java
package me.sepehrhn.pocketdice.util;

import me.sepehrhn.pocketdice.PocketDice;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.Map;

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

    /** Send a locale-backed message with optional placeholders. */
    public static void sendLocale(PocketDice plugin, CommandSender sender, String key) {
        sendLocale(plugin, sender, key, Collections.emptyMap());
    }

    /** Send a locale-backed message with optional placeholders. */
    public static void sendLocale(PocketDice plugin, CommandSender sender, String key, Map<String, String> placeholders) {
        String raw = plugin.getLocaleManager().get(sender, key, placeholders);
        sender.sendMessage(color(raw));
    }
}
