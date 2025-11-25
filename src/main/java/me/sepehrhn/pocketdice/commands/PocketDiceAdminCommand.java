// File: D:\PocketDice\src\main\java\me\sepehrhn\pocketdice\commands\PocketDiceAdminCommand.java
package me.sepehrhn.pocketdice.commands;

import me.sepehrhn.pocketdice.PocketDice;
import me.sepehrhn.pocketdice.config.ConfigUpdater;
import me.sepehrhn.pocketdice.util.Text;
import org.bukkit.command.*;

import java.io.IOException;
import java.util.List;

public class PocketDiceAdminCommand implements CommandExecutor, TabCompleter {

    private final PocketDice plugin;

    public PocketDiceAdminCommand(PocketDice plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("pocketdice.reload")) {
            Text.sendError(plugin, sender, "You don't have permission to do that.");
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            try {
                ConfigUpdater.updateConfig(plugin);
                plugin.reloadConfig();
                Text.sendError(plugin, sender, "Config reloaded and updated.");
            } catch (IOException | IllegalStateException e) {
                plugin.getLogger().severe("Failed to update config.yml on reload: " + e.getMessage());
                Text.sendError(plugin, sender, "Failed to update config.yml. Check console for details.");
            }
            return true;
        }

        Text.sendError(plugin, sender, "Usage: /pocketdice reload");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length == 1) return List.of("reload");
        return List.of();
    }
}
