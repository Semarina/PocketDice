// File: D:\PocketDice\src\main\java\me\sepehrhn\pocketdice\commands\RollCommand.java
package me.sepehrhn.pocketdice.commands;

import me.sepehrhn.pocketdice.PocketDice;
import me.sepehrhn.pocketdice.util.DiceParser;
import me.sepehrhn.pocketdice.util.Text;
import org.bukkit.Location;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class RollCommand implements CommandExecutor, TabCompleter {

    private final PocketDice plugin;

    public RollCommand(PocketDice plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            Text.sendError(plugin, sender, "Only players can use /roll.");
            return true;
        }
        if (!sender.hasPermission("pocketdice.roll")) {
            Text.sendError(plugin, sender, "You don't have permission to use this command.");
            return true;
        }

        final var cfg = plugin.getConfig();
        final String defaultNotation = cfg.getString("default_notation", "1d100");
        final boolean allowShorthand = cfg.getBoolean("allow_shorthand_d", true);
        final int maxDice = cfg.getInt("max_dice", 50);
        final int maxFaces = cfg.getInt("max_faces", 1000);
        final int radius = cfg.getInt("radius", 16);

        final String notation = (args.length == 0 ? defaultNotation : args[0]).trim();

        final DiceParser.DiceSpec spec;
        try {
            spec = DiceParser.parse(notation, allowShorthand);
        } catch (IllegalArgumentException ex) {
            Text.sendError(plugin, sender, ex.getMessage());
            return true;
        }

        int dice = spec.dice();
        int faces = spec.faces();

        // Clamp to caps; inform only the roller
        if (spec.dice() > maxDice || spec.faces() > maxFaces) {
            Text.sendError(plugin, sender,
                "Requested " + spec.dice() + "d" + spec.faces()
                + " exceeds limits (max_dice=" + maxDice + ", max_faces=" + maxFaces + ").");
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
        final String msg = Text.color(Text.format(
                plugin.getConfig().getString("message_format",
                        "[PocketDice] {player} rolled {notation}: {results} (total {total})"),
                "player", player.getName(),
                "notation", finalNotation,
                "results", rolls.toString(),
                "total", Integer.toString(total)
        ));

        // Send to roller
        player.sendMessage(msg);

        // Send to players within radius (same world). Use distanceSquared for performance.
        final Location origin = player.getLocation();
        final double r2 = (double) radius * (double) radius;

        for (Player p : player.getWorld().getPlayers()) {
            if (p.equals(player)) continue; // already sent
            if (p.getLocation().distanceSquared(origin) <= r2) {
                p.sendMessage(msg);
            }
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("1d100", "1d6", "2d6", "d20");
        }
        return List.of();
    }
}