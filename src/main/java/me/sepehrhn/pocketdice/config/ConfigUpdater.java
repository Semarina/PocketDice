package me.sepehrhn.pocketdice.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Handles config.yml migrations and default merging across plugin versions. */
public final class ConfigUpdater {

    private static final boolean REMOVE_OBSOLETE_KEYS = false;

    private ConfigUpdater() {
    }

    public static YamlConfiguration loadDefaultConfig(JavaPlugin plugin) {
        InputStream in = plugin.getResource("config.yml");
        if (in == null) {
            throw new IllegalStateException("Default config.yml not found inside JAR.");
        }
        try (InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            return YamlConfiguration.loadConfiguration(reader);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read default config.yml from JAR.", e);
        }
    }

    /**
     * Update the runtime config.yml by running versioned migrations and merging in new defaults
     * without overwriting user-defined values.
     */
    public static void updateConfig(JavaPlugin plugin) throws IOException {
        plugin.getDataFolder().mkdirs();
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            plugin.saveDefaultConfig();
            return;
        }

        YamlConfiguration existingConfig = YamlConfiguration.loadConfiguration(configFile);
        YamlConfiguration defaultConfig = loadDefaultConfig(plugin);

        int currentVersion = existingConfig.getInt("config-version", 0);
        int targetVersion = defaultConfig.getInt("config-version", 1);

        boolean changed = false;

        if (currentVersion < targetVersion) {
            migrateConfig(existingConfig, currentVersion, targetVersion);
            changed = true;
        }

        if (mergeDefaults(existingConfig, defaultConfig)) {
            changed = true;
        }

        if (REMOVE_OBSOLETE_KEYS && removeObsoleteKeys(existingConfig, defaultConfig)) {
            changed = true;
        }

        if (existingConfig.getInt("config-version", 0) != targetVersion) {
            existingConfig.set("config-version", targetVersion);
            changed = true;
        }

        if (changed) {
            existingConfig.save(configFile);
        }
    }

    private static void migrateConfig(YamlConfiguration config, int currentVersion, int targetVersion) {
        for (int version = currentVersion + 1; version <= targetVersion; version++) {
            switch (version) {
                case 1 -> migrateToV1(config);
                case 2 -> migrateToV2(config);
                case 3 -> migrateToV3(config);
                case 4 -> migrateToV4(config);
                case 5 -> migrateToV5(config);
                default -> {
                }
            }
        }
    }

    private static void migrateToV1(YamlConfiguration config) {
        if (!config.contains("config-version")) {
            config.set("config-version", 1);
        }
    }

    private static void migrateToV2(YamlConfiguration config) {
        // Version 2 adds the configurable Modrinth update checker; defaults are merged below.
    }

    private static void migrateToV3(YamlConfiguration config) {
        // Version 3 moves user-facing strings to locale files; defaults are merged below.
    }

    private static void migrateToV4(YamlConfiguration config) {
        // Rename allow_shorthand_d to allow_shorthand if present
        if (!config.contains("allow_shorthand") && config.contains("allow_shorthand_d")) {
            config.set("allow_shorthand", config.get("allow_shorthand_d"));
        }
        config.set("allow_shorthand_d", null);

        // Remove obsolete admin_notify_permission customization
        config.set("updates.admin_notify_permission", null);
    }

    private static void migrateToV5(YamlConfiguration config) {
        // Remove modrinth_project_slug customization; slug is fixed in code
        config.set("updates.modrinth_project_slug", null);
    }

    private static boolean mergeDefaults(ConfigurationSection existing, ConfigurationSection defaults) {
        boolean changed = false;
        for (String key : defaults.getKeys(false)) {
            Object defaultValue = defaults.get(key);
            if (defaultValue instanceof ConfigurationSection defaultSection) {
                ConfigurationSection existingSection = existing.getConfigurationSection(key);
                if (existingSection == null) {
                    existingSection = existing.createSection(key);
                    changed = true;
                }
                if (mergeDefaults(existingSection, defaultSection)) {
                    changed = true;
                }
            } else {
                if (!existing.contains(key)) {
                    existing.set(key, defaultValue);
                    changed = true;
                }
            }
        }
        return changed;
    }

    private static boolean removeObsoleteKeys(ConfigurationSection existing, ConfigurationSection defaults) {
        boolean changed = false;
        for (String key : existing.getKeys(false)) {
            Object existingValue = existing.get(key);
            if (!defaults.contains(key)) {
                existing.set(key, null);
                changed = true;
                continue;
            }
            Object defaultValue = defaults.get(key);
            if (existingValue instanceof ConfigurationSection existingSection
                    && defaultValue instanceof ConfigurationSection defaultSection) {
                if (removeObsoleteKeys(existingSection, defaultSection)) {
                    changed = true;
                }
            }
        }
        return changed;
    }

    public static void moveKey(YamlConfiguration config, String oldPath, String newPath) {
        if (config.contains(oldPath) && !config.contains(newPath)) {
            Object value = config.get(oldPath);
            config.set(newPath, value);
            config.set(oldPath, null);
        }
    }
}
