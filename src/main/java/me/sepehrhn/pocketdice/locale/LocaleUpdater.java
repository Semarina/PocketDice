package me.sepehrhn.pocketdice.locale;

import me.sepehrhn.pocketdice.PocketDice;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

/** Updates locale files by merging defaults while preserving user changes. */
public final class LocaleUpdater {

    private LocaleUpdater() {
    }

    public static void updateLocales(PocketDice plugin) throws IOException {
        Logger logger = plugin.getLogger();
        File localeDir = new File(plugin.getDataFolder(), "locale");
        if (!localeDir.exists() && !localeDir.mkdirs()) {
            logger.warning("Could not create locale directory: " + localeDir.getAbsolutePath());
        }

        // Ensure default en_US exists
        File defaultFile = new File(localeDir, "en_US.yml");
        if (!defaultFile.exists()) {
            plugin.saveResource("locale/en_US.yml", false);
        }

        YamlConfiguration defaultLocale = loadDefault(plugin);
        if (defaultLocale == null) {
            logger.warning("Default locale en_US.yml missing from JAR; skipping locale update.");
            return;
        }

        File[] files = localeDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".yml"));
        if (files == null) return;

        for (File file : files) {
            YamlConfiguration existing = YamlConfiguration.loadConfiguration(file);
            boolean changed = mergeDefaults(existing, defaultLocale);
            if (changed) {
                existing.save(file);
            }
        }
    }

    private static YamlConfiguration loadDefault(PocketDice plugin) {
        InputStream in = plugin.getResource("locale/en_US.yml");
        if (in == null) {
            return null;
        }
        try (InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            return YamlConfiguration.loadConfiguration(reader);
        } catch (IOException e) {
            return null;
        }
    }

    /** Deep-merge missing keys from defaults without overwriting existing values. */
    private static boolean mergeDefaults(ConfigurationSection existing, ConfigurationSection defaults) {
        boolean changed = false;
        for (String key : defaults.getKeys(false)) {
            Object defVal = defaults.get(key);
            if (defVal instanceof ConfigurationSection defSection) {
                ConfigurationSection existingSection = existing.getConfigurationSection(key);
                if (existingSection == null) {
                    existingSection = existing.createSection(key);
                    changed = true;
                }
                if (mergeDefaults(existingSection, defSection)) {
                    changed = true;
                }
            } else {
                if (!existing.contains(key)) {
                    existing.set(key, defVal);
                    changed = true;
                }
            }
        }
        return changed;
    }
}
