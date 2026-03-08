package me.sepehrhn.pocketdice.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Handles config.yml migrations seamlessly preserving comments and adding new defaults via native Bukkit mapping. */
public final class ConfigUpdater {

    private ConfigUpdater() {
    }

    public static void updateConfig(JavaPlugin plugin) throws IOException {
        plugin.getDataFolder().mkdirs();
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
            return;
        }

        YamlConfiguration currentConfig = YamlConfiguration.loadConfiguration(configFile);
        
        InputStream in = plugin.getResource("config.yml");
        if (in == null) return;
        
        YamlConfiguration defaultConfig;
        try (InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            defaultConfig = YamlConfiguration.loadConfiguration(reader);
        }

        int currentVersion = currentConfig.getInt("config-version", 0);
        int targetVersion = defaultConfig.getInt("config-version", 1);
        boolean changed = false;

        if (currentVersion < targetVersion) {
            migrate(currentConfig, currentVersion, targetVersion);
            changed = true;
        }

        // Apply native Bukkit configurations checking for missing elements securely 
        // retaining custom user nodes inside identically tagged paths
        for (String key : defaultConfig.getKeys(true)) {
            if (!currentConfig.contains(key)) {
                currentConfig.set(key, defaultConfig.get(key));
                changed = true;
            }
        }

        if (changed) {
            // Because older Bukkit versions destroy comments on .save(), we use standard copyDefaults
            // to safely inherit values. (Mimicking DoorsReloaded logic strictly)
            currentConfig.options().copyDefaults(true);
            currentConfig.setDefaults(defaultConfig);
            currentConfig.save(configFile);
        }
    }

    private static void migrate(YamlConfiguration config, int currentVersion, int targetVersion) {
        for (int version = currentVersion + 1; version <= targetVersion; version++) {
            switch (version) {
                case 4 -> migrateToV4(config);
                case 5 -> migrateToV5(config);
                case 6 -> migrateToV6(config);
            }
        }
        config.set("config-version", targetVersion);
    }
    
    private static void migrateToV4(YamlConfiguration config) {
        if (!config.contains("allow_shorthand") && config.contains("allow_shorthand_d")) {
            config.set("allow_shorthand", config.get("allow_shorthand_d"));
        }
        config.set("allow_shorthand_d", null);
        config.set("updates.admin_notify_permission", null);
    }

    private static void migrateToV5(YamlConfiguration config) {
        config.set("updates.modrinth_project_slug", null);
    }

    private static void migrateToV6(YamlConfiguration config) {
        // Version 6 adds cooldowns and rate_limit, handled by copyDefaults.
    }
}
