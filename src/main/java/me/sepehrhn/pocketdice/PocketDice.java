// File: D:\PocketDice\src\main\java\me\sepehrhn\pocketdice\PocketDice.java
package me.sepehrhn.pocketdice;

import me.sepehrhn.pocketdice.commands.PocketDiceAdminCommand;
import me.sepehrhn.pocketdice.commands.RollCommand;
import me.sepehrhn.pocketdice.config.ConfigUpdater;
import me.sepehrhn.pocketdice.locale.LocaleManager;
import me.sepehrhn.pocketdice.update.UpdateChecker;
import me.sepehrhn.pocketdice.update.UpdateNotifyListener;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;

public class PocketDice extends JavaPlugin {

    private UpdateChecker updateChecker;
    private LocaleManager localeManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        try {
            ConfigUpdater.updateConfig(this);
        } catch (IOException | IllegalStateException e) {
            getLogger().severe("Failed to update config.yml: " + e.getMessage());
        }
        reloadConfig();

        localeManager = new LocaleManager(this);
        localeManager.reload();

        initUpdateChecker();

        getServer().getPluginManager().registerEvents(new UpdateNotifyListener(this), this);

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
        if (updateChecker != null) {
            updateChecker.shutdown();
        }
        getLogger().info("PocketDice disabled.");
    }

    public UpdateChecker getUpdateChecker() {
        return updateChecker;
    }

    public LocaleManager getLocaleManager() {
        return localeManager;
    }

    public void restartUpdateChecker() {
        initUpdateChecker();
    }

    private void initUpdateChecker() {
        if (updateChecker == null) {
            updateChecker = new UpdateChecker(this);
        } else {
            updateChecker.shutdown();
        }
        updateChecker.initFromConfig();
        updateChecker.start();
    }
}
