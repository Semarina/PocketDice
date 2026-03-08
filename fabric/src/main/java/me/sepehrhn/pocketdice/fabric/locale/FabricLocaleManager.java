package me.sepehrhn.pocketdice.fabric.locale;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class FabricLocaleManager {

    private static final Logger LOGGER = LoggerFactory.getLogger("PocketDice");
    private static final String DEFAULT_LOCALE = "en_us";
    private static final Map<String, Map<String, Object>> locales = new HashMap<>();
    private static Map<String, Object> jarDefaults = new HashMap<>();
    private static final Path LOCALE_DIR = FabricLoader.getInstance().getConfigDir().resolve("pocketdice/locale");

    private static final Map<String, String> HARDCODED_FALLBACKS = new HashMap<>();
    static {
        HARDCODED_FALLBACKS.put("messages.prefix", "<aqua>[PocketDice]</aqua> ");
        HARDCODED_FALLBACKS.put("messages.command.pocketdice_help", "{prefix}<yellow>PocketDice Commands:</yellow><newline><gray> - </gray><aqua>/pd reload</aqua><gray> : Reloads config and locales.</gray><newline><gray> - </gray><aqua>/pd version</aqua><gray> : Displays current version.</gray>");
        HARDCODED_FALLBACKS.put("messages.command.roll_help", "{prefix}<yellow>Rolling Dice:</yellow><newline><gray>Use format NdM (e.g. 2d6). Leave blank for 1d100.</gray><newline><gray> - </gray><aqua>/roll 2d20</aqua><gray> : Rolls two 20-sided dice.</gray>");
        HARDCODED_FALLBACKS.put("messages.command.no_permission", "{prefix}<red>You don't have permission to do that.</red>");
        HARDCODED_FALLBACKS.put("messages.command.reload_success", "{prefix}<green>Config and locales reloaded.</green>");
        HARDCODED_FALLBACKS.put("messages.command.reload_failure", "{prefix}<red>Failed to reload config or locales. Check console for details.</red>");
        HARDCODED_FALLBACKS.put("messages.command.pocketdice_version", "{prefix}<gray>Running PocketDice version </gray><green>{version}</green>");
    }

    public static void reload() {
        locales.clear();
        jarDefaults.clear();
        
        System.out.println("[PocketDice] Starting locale reload sequence...");
        
        // Load defaults from JAR (internal backup)
        String[] possiblePaths = {"/locale/en_US.yml", "locale/en_US.yml", "/locale/en_us.yml", "locale/en_us.yml"};
        boolean found = false;
        for (String path : possiblePaths) {
            try (InputStream in = FabricLocaleManager.class.getResourceAsStream(path)) {
                if (in != null) {
                    Yaml yaml = new Yaml();
                    Map<String, Object> loaded = yaml.load(new InputStreamReader(in, StandardCharsets.UTF_8));
                    if (loaded != null) {
                        jarDefaults = loaded;
                        System.out.println("[PocketDice] SUCCESS: Loaded internal JAR defaults from " + path);
                        found = true;
                        break;
                    }
                } else {
                    System.out.println("[PocketDice] Resource not found in JAR: " + path);
                }
            } catch (Exception e) {
                System.out.println("[PocketDice] Error reading resource " + path + ": " + e.getMessage());
            }
        }
        if (!found) {
            System.out.println("[PocketDice] CRITICAL: Could not find any locale files in JAR!");
        }

        try {
            if (LOCALE_DIR.getParent() != null && !Files.exists(LOCALE_DIR.getParent())) {
                Files.createDirectories(LOCALE_DIR.getParent());
            }
            Files.createDirectories(LOCALE_DIR);

            File[] files = LOCALE_DIR.toFile().listFiles((dir, name) -> name.toLowerCase().endsWith(".yml"));
            if (files != null) {
                Yaml yaml = new Yaml();
                for (File file : files) {
                    try (InputStreamReader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
                        Map<String, Object> map = yaml.load(reader);
                        if (map != null) {
                            String code = file.getName().substring(0, file.getName().length() - 4).toLowerCase();
                            locales.put(code, map);
                            System.out.println("[PocketDice] SUCCESS: Loaded disk locale: " + code);
                        }
                    } catch (Exception e) {
                        LOGGER.error("Failed to load locale file: " + file.getName(), e);
                    }
                }
            }

        } catch (Exception e) {
            LOGGER.error("Failed to reload locales", e);
        }
    }

    public static String get(String localeCode, String path) {
        return get(localeCode, path, Collections.emptyMap());
    }

    public static String getDefault(String path) {
        return get(DEFAULT_LOCALE, path, Collections.emptyMap());
    }

    @SuppressWarnings("unchecked")
    private static String getRawString(Map<String, Object> map, String path) {
        if (map == null || map.isEmpty()) return null;
        String[] keys = path.split("\\.");
        Object current = map;
        for (String key : keys) {
            if (!(current instanceof Map)) return null;
            current = ((Map<String, Object>) current).get(key);
            if (current == null) return null;
        }
        return current.toString();
    }

    public static String get(String localeCode, String path, Map<String, String> placeholders) {
        String code = (localeCode == null ? DEFAULT_LOCALE : localeCode).toLowerCase();
        
        // 1. Try requested locale from disk
        String message = getRawString(locales.get(code), path);
        
        // 2. Try default locale from disk (en_us)
        if (message == null && !code.equals(DEFAULT_LOCALE)) {
            message = getRawString(locales.get(DEFAULT_LOCALE), path);
        }
        
        // 3. Try internal JAR defaults
        if (message == null) {
            message = getRawString(jarDefaults, path);
        }
        
        // 4. Ultimate hardcoded fallback for help messages
        if (message == null) {
            message = HARDCODED_FALLBACKS.get(path);
        }
        
        if (message == null) {
            System.out.println("[PocketDice] WARNING: Still missing localization key: " + path);
            return path;
        }

        Map<String, String> mutable = new HashMap<>(placeholders);
        if (!mutable.containsKey("prefix")) {
            String prefix = getRawString(locales.get(code), "messages.prefix");
            if (prefix == null) prefix = getRawString(locales.get(DEFAULT_LOCALE), "messages.prefix");
            if (prefix == null) prefix = getRawString(jarDefaults, "messages.prefix");
            if (prefix == null) prefix = HARDCODED_FALLBACKS.get("messages.prefix");
            if (prefix == null) prefix = "";
            mutable.put("prefix", prefix);
        }

        for (Map.Entry<String, String> entry : mutable.entrySet()) {
            String val = entry.getValue() == null ? "" : entry.getValue();
            message = message.replace("{" + entry.getKey() + "}", val);
        }
        return message;
    }

    public static net.minecraft.text.Text getText(net.minecraft.registry.RegistryWrapper.WrapperLookup registryManager, String localeCode, String path, Map<String, String> placeholders) {
        String msg = get(localeCode, path, placeholders);
        if (msg.equals(path)) return net.minecraft.text.Text.literal(msg);

        try {
            net.kyori.adventure.text.Component comp = net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(msg);
            String json = net.kyori.adventure.text.serializer.gson.GsonComponentSerializer.gson().serialize(comp);
            return net.minecraft.text.TextCodecs.CODEC.parse(registryManager.getOps(com.mojang.serialization.JsonOps.INSTANCE), com.google.gson.JsonParser.parseString(json))
                    .result()
                    .orElse(net.minecraft.text.Text.literal(msg));
        } catch (Exception e) {
            // Fallback to literal if MiniMessage fails
            return net.minecraft.text.Text.literal(msg);
        }
    }

    public static net.minecraft.text.Text getText(net.minecraft.registry.RegistryWrapper.WrapperLookup registryManager, String localeCode, String path) {
        return getText(registryManager, localeCode, path, Collections.emptyMap());
    }
}
