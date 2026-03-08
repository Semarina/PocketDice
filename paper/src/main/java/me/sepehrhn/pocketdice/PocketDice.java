// File: D:\PocketDice\src\main\java\me\sepehrhn\pocketdice\PocketDice.java
package me.sepehrhn.pocketdice;

import me.sepehrhn.pocketdice.commands.PocketDiceAdminCommand;
import me.sepehrhn.pocketdice.commands.RollCommand;
import me.sepehrhn.pocketdice.config.ConfigUpdater;
import me.sepehrhn.pocketdice.locale.LocaleManager;
import me.sepehrhn.pocketdice.util.AntiSpamService;
import me.sepehrhn.pocketdice.update.UpdateChecker;
import me.sepehrhn.pocketdice.update.UpdateNotifyListener;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;

public class PocketDice extends JavaPlugin {

    private UpdateChecker updateChecker;
    private LocaleManager localeManager;
    private AntiSpamService antiSpamService;

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

        antiSpamService = new AntiSpamService();

        initUpdateChecker();

        getServer().getPluginManager().registerEvents(new UpdateNotifyListener(this), this);

        // /roll command
        var rollCmd = new RollCommand(this, antiSpamService);
        
        var roll = getCommand("roll");
        if (roll != null) {
            roll.setExecutor(rollCmd);
            roll.setTabCompleter(rollCmd);
        } else {
            getLogger().warning("Command 'roll' not found in plugin.yml!");
        }

        var groll = getCommand("groll");
        if (groll != null) {
            groll.setExecutor(rollCmd);
            groll.setTabCompleter(rollCmd);
        } else {
            getLogger().warning("Command 'groll' not found in plugin.yml!");
        }

        var proll = getCommand("proll");
        if (proll != null) {
            proll.setExecutor(rollCmd);
            proll.setTabCompleter(rollCmd);
        } else {
            getLogger().warning("Command 'proll' not found in plugin.yml!");
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
                "PocketDice enabled. radius=%d default=%s caps=%dd%d anti-spam=[CD=%ss, RL=%d/%ss]",
                getConfig().getInt("radius", 16),
                getConfig().getString("default_notation", "1d100"),
                getConfig().getInt("max_dice", 50),
                getConfig().getInt("max_faces", 1000),
                getConfig().getBoolean("cooldowns.enabled", true) ? getConfig().getInt("cooldowns.seconds", 3) : "off",
                getConfig().getInt("rate_limit.max_rolls", 12),
                getConfig().getBoolean("rate_limit.enabled", true) ? getConfig().getInt("rate_limit.window_seconds", 60) : "off"
        ));
    }

    public void debug(String message) {
        if (getConfig().getBoolean("debug", false)) {
            getLogger().info("[DEBUG] " + message);
        }
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
