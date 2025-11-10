// File: D:\PocketDice\src\main\java\me\sepehrhn\pocketdice\PocketDice.java
package me.sepehrhn.pocketdice;

import me.sepehrhn.pocketdice.commands.PocketDiceAdminCommand;
import me.sepehrhn.pocketdice.commands.RollCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class PocketDice extends JavaPlugin {

    @Override
    public void onEnable() {
        // Create default config.yml on first run
        saveDefaultConfig();

        // /roll command
        var roll = getCommand("roll");
        if (roll != null) {
            var rollCmd = new RollCommand(this);
            roll.setExecutor(rollCmd);
            roll.setTabCompleter(rollCmd);
        } else {
            getLogger().warning("Command 'roll' not found in plugin.yml!");
        }

        // /pocketdice admin command
        var admin = getCommand("pocketdice");
        if (admin != null) {
            var adminCmd = new PocketDiceAdminCommand(this);
            admin.setExecutor(adminCmd);
            admin.setTabCompleter(adminCmd);
        } else {
            getLogger().warning("Command 'pocketdice' not found in plugin.yml!");
        }

        getLogger().info(() -> String.format(
                "PocketDice enabled. radius=%d default=%s caps=%dd%d",
                getConfig().getInt("radius", 16),
                getConfig().getString("default_notation", "1d100"),
                getConfig().getInt("max_dice", 50),
                getConfig().getInt("max_faces", 1000)
        ));
    }

    @Override
    public void onDisable() {
        getLogger().info("PocketDice disabled.");
    }
}