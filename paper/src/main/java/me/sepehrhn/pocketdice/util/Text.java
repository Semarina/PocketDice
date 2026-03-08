// File: src/main/java/me/sepehrhn/pocketdice/util/Text.java
package me.sepehrhn.pocketdice.util;

import me.sepehrhn.pocketdice.PocketDice;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.Map;

/** Simple text utilities for placeholder replacement and coloring. */
public final class Text {
    private Text() {}

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

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

    /** Deserialize MiniMessage to a Component. */
    public static Component toComponent(String raw) {
        if (raw == null) return Component.empty();
        try {
            return MINI.deserialize(raw);
        } catch (Exception ex) {
            return Component.text(raw);
        }
    }

    /** Convert MiniMessage text to legacy string for console logging. */
    public static String toLegacy(String raw) {
        return LEGACY.serialize(toComponent(raw));
    }

    /** Send a locale-backed message with optional placeholders. */
    public static void sendLocale(PocketDice plugin, CommandSender sender, String key) {
        sendLocale(plugin, sender, key, Collections.emptyMap());
    }

    /** Send a locale-backed message with optional placeholders. */
    public static void sendLocale(PocketDice plugin, CommandSender sender, String key, Map<String, String> placeholders) {
        String raw = plugin.getLocaleManager().get(sender, key, placeholders);
        sender.sendMessage(toComponent(raw));
    }
}
