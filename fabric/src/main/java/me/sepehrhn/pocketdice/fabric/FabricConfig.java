package me.sepehrhn.pocketdice.fabric;

import net.fabricmc.loader.api.FabricLoader;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class FabricConfig {

    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("pocketdice/pocketdice.yml");
    public static ConfigData DATA = new ConfigData();

    public static void load() {
        try {
            if (CONFIG_PATH.getParent() != null && !Files.exists(CONFIG_PATH.getParent())) {
                Files.createDirectories(CONFIG_PATH.getParent());
            }
        } catch (Exception e) {}

        if (Files.exists(CONFIG_PATH)) {
            try (InputStreamReader reader = new InputStreamReader(new FileInputStream(CONFIG_PATH.toFile()), StandardCharsets.UTF_8)) {
                Yaml yaml = new Yaml();
                Map<String, Object> map = yaml.load(reader);
                if (map != null) {
                    if (map.containsKey("config-version")) DATA.config_version = (int) map.get("config-version");
                    if (map.containsKey("radius")) DATA.radius = (int) map.get("radius");
                    if (map.containsKey("default_notation")) DATA.default_notation = (String) map.get("default_notation");
                    if (map.containsKey("allow_shorthand")) DATA.allow_shorthand = (boolean) map.get("allow_shorthand");
                    if (map.containsKey("max_dice")) DATA.max_dice = (int) map.get("max_dice");
                    if (map.containsKey("max_faces")) DATA.max_faces = (int) map.get("max_faces");
                    if (map.containsKey("debug")) DATA.debug = (boolean) map.get("debug");

                    if (map.containsKey("cooldowns")) {
                        Map<String, Object> cooldowns = (Map<String, Object>) map.get("cooldowns");
                        if (cooldowns.containsKey("enabled")) DATA.cooldowns.enabled = (boolean) cooldowns.get("enabled");
                        if (cooldowns.containsKey("seconds")) DATA.cooldowns.seconds = (int) cooldowns.get("seconds");
                        if (cooldowns.containsKey("bypass_permission")) DATA.cooldowns.bypass_permission = (String) cooldowns.get("bypass_permission");
                    }

                    if (map.containsKey("rate_limit")) {
                        Map<String, Object> rate_limit = (Map<String, Object>) map.get("rate_limit");
                        if (rate_limit.containsKey("enabled")) DATA.rate_limit.enabled = (boolean) rate_limit.get("enabled");
                        if (rate_limit.containsKey("window_seconds")) DATA.rate_limit.window_seconds = (int) rate_limit.get("window_seconds");
                        if (rate_limit.containsKey("max_rolls")) DATA.rate_limit.max_rolls = (int) rate_limit.get("max_rolls");
                        if (rate_limit.containsKey("bypass_permission")) DATA.rate_limit.bypass_permission = (String) rate_limit.get("bypass_permission");
                    }

                    if (map.containsKey("sounds")) {
                        Map<String, Object> sounds = (Map<String, Object>) map.get("sounds");
                        if (sounds.containsKey("roll")) {
                            Map<String, Object> roll = (Map<String, Object>) sounds.get("roll");
                            if (roll.containsKey("enabled")) DATA.sounds.roll.enabled = (boolean) roll.get("enabled");
                            if (roll.containsKey("sound_key")) DATA.sounds.roll.sound_key = (String) roll.get("sound_key");
                            if (roll.containsKey("volume")) DATA.sounds.roll.volume = getDouble(roll.get("volume"));
                            if (roll.containsKey("pitch")) DATA.sounds.roll.pitch = getDouble(roll.get("pitch"));
                        }
                    }

                    if (map.containsKey("updates")) {
                        Map<String, Object> updates = (Map<String, Object>) map.get("updates");
                        if (updates.containsKey("enabled")) DATA.updates.enabled = (boolean) updates.get("enabled");
                        if (updates.containsKey("check_on_startup")) DATA.updates.check_on_startup = (boolean) updates.get("check_on_startup");
                        if (updates.containsKey("check_interval_hours")) DATA.updates.check_interval_hours = getDouble(updates.get("check_interval_hours"));
                        if (updates.containsKey("notify_console")) DATA.updates.notify_console = (boolean) updates.get("notify_console");
                        if (updates.containsKey("notify_admins_on_join")) DATA.updates.notify_admins_on_join = (boolean) updates.get("notify_admins_on_join");
                    }
                    
                    // Auto-migrate old default sound to new default
                    if ("minecraft:block.amethyst_block.chime".equals(DATA.sounds.roll.sound_key) || 
                        "minecraft:item.lodestone_compass.lock".equals(DATA.sounds.roll.sound_key) ||
                        ("minecraft:block.lodestone.place".equals(DATA.sounds.roll.sound_key) && DATA.sounds.roll.volume < 2.0)) {
                        DATA.sounds.roll.sound_key = "minecraft:block.lodestone.place";
                        DATA.sounds.roll.volume = 2.0;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // Always save back natively guaranteeing comments inject perfectly over new elements or formatting defects
        save();
    }

    private static double getDouble(Object obj) {
        if (obj instanceof Number) {
            return ((Number) obj).doubleValue();
        }
        try {
            return Double.parseDouble(obj.toString());
        } catch (Exception e) {
            return 1.0;
        }
    }

    public static void save() {
        StringBuilder sb = new StringBuilder();

        sb.append("###########################\n");
        sb.append("#       PocketDice        #\n");
        sb.append("###########################\n\n");

        sb.append("config-version: ").append(DATA.config_version).append("\n\n");

        sb.append("# Developed and maintained by Semarina.\n\n");

        sb.append("###########################\n");
        sb.append("#         General         #\n");
        sb.append("###########################\n\n");

        sb.append("# The maximum radius in blocks that dice rolls can be seen/heard by other players.\n");
        sb.append("radius: ").append(DATA.radius).append("\n\n");

        sb.append("# The default dice notation (e.g. 1d100, 2d6) used when rolling without arguments.\n");
        sb.append("default_notation: \"").append(DATA.default_notation).append("\"\n\n");

        sb.append("# Allow players to use shorthand 'd' prefixes for simple faces (e.g. 'd20' instead of '1d20').\n");
        sb.append("allow_shorthand: ").append(DATA.allow_shorthand).append("\n\n");

        sb.append("# Hard limit for the maximum number of dice a player can roll at once to prevent server lag.\n");
        sb.append("max_dice: ").append(DATA.max_dice).append("\n\n");

        sb.append("# Hard limit for the maximum number of faces on a single die.\n");
        sb.append("max_faces: ").append(DATA.max_faces).append("\n\n");

        sb.append("# Diagnostic switch printing robust execution markers natively to Console when enabled\n");
        sb.append("debug: ").append(DATA.debug).append("\n\n");

        sb.append("###########################\n");
        sb.append("#       Anti-Spam         #\n");
        sb.append("###########################\n\n");

        sb.append("# Cooldown between rolls per player.\n");
        sb.append("cooldowns:\n");
        sb.append("  enabled: ").append(DATA.cooldowns.enabled).append("\n");
        sb.append("  # Minimum seconds between rolls per player.\n");
        sb.append("  seconds: ").append(DATA.cooldowns.seconds).append("\n");
        sb.append("  # Permission to bypass the cooldown entirely.\n");
        sb.append("  bypass_permission: \"").append(DATA.cooldowns.bypass_permission).append("\"\n\n");

        sb.append("# Rate limit: max rolls within a sliding time window.\n");
        sb.append("rate_limit:\n");
        sb.append("  enabled: ").append(DATA.rate_limit.enabled).append("\n");
        sb.append("  # Time window in seconds.\n");
        sb.append("  window_seconds: ").append(DATA.rate_limit.window_seconds).append("\n");
        sb.append("  # Maximum rolls per player within the window.\n");
        sb.append("  max_rolls: ").append(DATA.rate_limit.max_rolls).append("\n");
        sb.append("  # Permission to bypass the rate limit entirely.\n");
        sb.append("  bypass_permission: \"").append(DATA.rate_limit.bypass_permission).append("\"\n\n");

        sb.append("###########################\n");
        sb.append("#         Sounds          #\n");
        sb.append("###########################\n\n");

        sb.append("# Optional sound effect played to players in the radius when a roll occurs.\n");
        sb.append("sounds:\n");
        sb.append("  roll:\n");
        sb.append("    enabled: ").append(DATA.sounds.roll.enabled).append("\n");
        sb.append("    # See here: https://hub.spigotmc.org/javadocs/spigot/org/bukkit/Sound.html\n");
        sb.append("    sound_key: \"").append(DATA.sounds.roll.sound_key).append("\"\n");
        sb.append("    volume: ").append(DATA.sounds.roll.volume).append("\n");
        sb.append("    pitch: ").append(DATA.sounds.roll.pitch).append("\n\n");

        sb.append("###########################\n");
        sb.append("#         Updates         #\n");
        sb.append("###########################\n\n");

        sb.append("# Optional Modrinth update checks for PocketDice.\n");
        sb.append("# Set enabled to false to disable HTTP calls entirely.\n");
        sb.append("updates:\n");
        sb.append("  enabled: ").append(DATA.updates.enabled).append("\n");
        sb.append("  check_on_startup: ").append(DATA.updates.check_on_startup).append("\n");
        sb.append("  # Interval in hours to check for new updates (0 or negative = startup-only)\n");
        sb.append("  check_interval_hours: ").append(DATA.updates.check_interval_hours).append("\n");
        sb.append("  notify_console: ").append(DATA.updates.notify_console).append("\n");
        sb.append("  # Notifies administrators (OPs) when they log into the server\n");
        sb.append("  notify_admins_on_join: ").append(DATA.updates.notify_admins_on_join).append("\n");

        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(CONFIG_PATH.toFile()), StandardCharsets.UTF_8))) {
            writer.write(sb.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static class ConfigData {
        public int config_version = 5;
        public int radius = 16;
        public String default_notation = "1d100";
        public boolean allow_shorthand = true;
        public int max_dice = 50;
        public int max_faces = 1000;
        public boolean debug = false;
        public CooldownConfig cooldowns = new CooldownConfig();
        public RateLimitConfig rate_limit = new RateLimitConfig();
        public SoundConfig sounds = new SoundConfig();
        public UpdateConfig updates = new UpdateConfig();

        public static class CooldownConfig {
            public boolean enabled = true;
            public int seconds = 3;
            public String bypass_permission = "pocketdice.cooldown.bypass";
        }

        public static class RateLimitConfig {
            public boolean enabled = true;
            public int window_seconds = 60;
            public int max_rolls = 12;
            public String bypass_permission = "pocketdice.ratelimit.bypass";
        }

        public static class SoundConfig {
            public RollSound roll = new RollSound();

            public static class RollSound {
                public boolean enabled = true;
                public String sound_key = "minecraft:block.lodestone.place";
                public double volume = 2.0;
                public double pitch = 1.2;
            }
        }

        public static class UpdateConfig {
            public boolean enabled = true;
            public boolean check_on_startup = true;
            public double check_interval_hours = 24.0;
            public boolean notify_console = true;
            public boolean notify_admins_on_join = true;
        }
    }
}
