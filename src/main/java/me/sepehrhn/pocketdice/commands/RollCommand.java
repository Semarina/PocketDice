// File: D:\PocketDice\src\main\java\me\sepehrhn\pocketdice\commands\RollCommand.java
package me.sepehrhn.pocketdice.commands;

import me.sepehrhn.pocketdice.PocketDice;
import me.sepehrhn.pocketdice.util.DiceParser;
import me.sepehrhn.pocketdice.util.Text;
import org.bukkit.Location;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.NamespacedKey;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class RollCommand implements CommandExecutor, TabCompleter {

    private final PocketDice plugin;

    public RollCommand(PocketDice plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            Text.sendLocale(plugin, sender, "messages.command.player_only");
            return true;
        }
        if (!sender.hasPermission("pocketdice.roll")) {
            Text.sendLocale(plugin, sender, "messages.command.no_permission");
            return true;
        }

        final var cfg = plugin.getConfig();
        final String defaultNotation = cfg.getString("default_notation", "1d100");
        final boolean allowShorthand = cfg.contains("allow_shorthand")
                ? cfg.getBoolean("allow_shorthand", true)
                : cfg.getBoolean("allow_shorthand_d", true);
        final int maxDice = cfg.getInt("max_dice", 50);
        final int maxFaces = cfg.getInt("max_faces", 1000);
        final int radius = cfg.getInt("radius", 16);

        final String notation = (args.length == 0 ? defaultNotation : args[0]).trim();

        final DiceParser.DiceSpec spec;
        try {
            spec = DiceParser.parse(notation, allowShorthand);
        } catch (DiceParser.DiceParseException ex) {
            handleParseError(sender, allowShorthand, ex);
            return true;
        }

        int dice = spec.dice();
        int faces = spec.faces();

        // Clamp to caps; inform only the roller
        if (spec.dice() > maxDice || spec.faces() > maxFaces) {
            Text.sendLocale(plugin, sender, "messages.roll.limits_exceeded", Map.of(
                    "notation", spec.dice() + "d" + spec.faces(),
                    "max_dice", Integer.toString(maxDice),
                    "max_faces", Integer.toString(maxFaces)
            ));
            return true;
        }

        // Roll
        final ThreadLocalRandom rng = ThreadLocalRandom.current();
        final List<Integer> rolls = new ArrayList<>(dice);
        int total = 0;
        for (int i = 0; i < dice; i++) {
            int value = rng.nextInt(1, faces + 1);
            rolls.add(value);
            total += value;
        }

        final String finalNotation = dice + "d" + faces;
        final String msg = plugin.getLocaleManager().get(player, "messages.roll.result", Map.of(
                "player", player.getName(),
                "notation", finalNotation,
                "results", rolls.toString(),
                "total", Integer.toString(total)
        ));
        Component msgComponent = Text.toComponent(msg);

        // Send to roller
        player.sendMessage(msgComponent);

        // Send to players within radius (same world). Use distanceSquared for performance.
        final Location origin = player.getLocation();
        final double r2 = (double) radius * (double) radius;

        for (Player p : player.getWorld().getPlayers()) {
            if (p.equals(player)) continue; // already sent
            if (p.getLocation().distanceSquared(origin) <= r2) {
                p.sendMessage(msgComponent);
            }
        }

        playRollSound(player);

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("1d100", "1d6", "2d6", "d20");
        }
        return List.of();
    }

    private void handleParseError(CommandSender sender, boolean allowShorthand, DiceParser.DiceParseException ex) {
        String key = switch (ex.getError()) {
            case MISSING_NOTATION -> "messages.roll.missing_notation";
            case INVALID_NOTATION -> "messages.roll.invalid_notation";
            case DICE_NAN -> "messages.roll.dice_nan";
            case FACES_NAN -> "messages.roll.faces_nan";
            case DICE_TOO_LOW -> "messages.roll.dice_too_low";
            case FACES_TOO_LOW -> "messages.roll.faces_too_low";
        };

        Map<String, String> placeholders = Map.of("or_d", ex.isShorthandAllowed() ? " or d8" : "");
        Text.sendLocale(plugin, sender, key, placeholders);
    }

    private String lastInvalidSoundKey = null;

    private void playRollSound(Player player) {
        ConfigurationSection soundCfg = plugin.getConfig().getConfigurationSection("sounds.roll");
        if (soundCfg == null || !soundCfg.getBoolean("enabled", true)) return;

        String soundKey = soundCfg.getString("sound_key", "minecraft:block.amethyst_block.chime");
        float volume = (float) soundCfg.getDouble("volume", 0.7D);
        float pitch = (float) soundCfg.getDouble("pitch", 1.2D);

        if (soundKey == null || soundKey.isBlank()) {
            warnInvalidSound("(empty)", "sound key missing");
            return;
        }

        NamespacedKey key = NamespacedKey.fromString(soundKey);
        if (key == null) {
            warnInvalidSound(soundKey, "invalid namespaced key");
            return;
        }

        try {
            player.playSound(player.getLocation(), soundKey, volume, pitch);
        } catch (Exception ex) {
            warnInvalidSound(soundKey, ex.getMessage());
        }
    }

    private void warnInvalidSound(String soundKey, String reason) {
        if (soundKey.equals(lastInvalidSoundKey)) return;
        lastInvalidSoundKey = soundKey;
        plugin.getLogger().warning("[PocketDice] Invalid roll sound '" + soundKey + "': " + reason + ". Sound disabled for this session.");
    }
}
