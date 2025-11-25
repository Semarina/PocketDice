package me.sepehrhn.pocketdice.update;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.sepehrhn.pocketdice.PocketDice;
import me.sepehrhn.pocketdice.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.scheduler.BukkitTask;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/** Performs Modrinth update checks for PocketDice. */
public class UpdateChecker {

    private static final String MODRINTH_API_BASE = "https://api.modrinth.com/v2/project/";
    private static final String MODRINTH_PROJECT_PAGE = "https://modrinth.com/plugin/";
    private static final String PROJECT_SLUG = "pocketdice";
    private static final long MIN_DELAY_TICKS = 20L; // 1 second
    private static final String UPDATE_NOTIFY_PERMISSION = "pocketdice.update.notify";

    private final PocketDice plugin;
    private final HttpClient httpClient;

    private boolean enabled;
    private boolean checkOnStartup;
    private long checkIntervalTicks;
    private boolean notifyConsole;
    private boolean notifyAdminsOnJoin;
    private String adminNotifyPermission;

    private volatile UpdateCheckResult lastResult;
    private BukkitTask intervalTask;

    public UpdateChecker(PocketDice plugin) {
        this.plugin = plugin;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /** Load settings from config. Does not start tasks. */
    public void initFromConfig() {
        ConfigurationSection updates = plugin.getConfig().getConfigurationSection("updates");
        if (updates == null) {
            enabled = false;
            checkOnStartup = false;
            checkIntervalTicks = -1;
            notifyConsole = false;
            notifyAdminsOnJoin = false;
            adminNotifyPermission = UPDATE_NOTIFY_PERMISSION;
            lastResult = null;
            return;
        }

        enabled = updates.getBoolean("enabled", true);
        checkOnStartup = updates.getBoolean("check_on_startup", true);
        double intervalHours = updates.getDouble("check_interval_hours", 24.0);
        if (intervalHours > 0) {
            long ticks = (long) (intervalHours * 60 * 60 * 20);
            checkIntervalTicks = Math.max(MIN_DELAY_TICKS, ticks);
        } else {
            checkIntervalTicks = -1;
        }
        notifyConsole = updates.getBoolean("notify_console", true);
        notifyAdminsOnJoin = updates.getBoolean("notify_admins_on_join", true);
        adminNotifyPermission = UPDATE_NOTIFY_PERMISSION;

        lastResult = null;
    }

    /** Start scheduled checks based on current settings. */
    public void start() {
        shutdown();
        if (!enabled) {
            return;
        }
        if (checkOnStartup) {
            checkForUpdatesAsync(MIN_DELAY_TICKS);
        }
        if (checkIntervalTicks > 0) {
            intervalTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin,
                    this::runCheckSilently, MIN_DELAY_TICKS, checkIntervalTicks);
        }
    }

    /** Cancel scheduled tasks. */
    public void shutdown() {
        if (intervalTask != null) {
            intervalTask.cancel();
            intervalTask = null;
        }
    }

    /** Trigger an async update check immediately. */
    public void checkForUpdatesAsync() {
        checkForUpdatesAsync(0L);
    }

    private void checkForUpdatesAsync(long delayTicks) {
        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, this::runCheckSilently, Math.max(0L, delayTicks));
    }

    private void runCheckSilently() {
        try {
            UpdateCheckResult result = performCheck();
            lastResult = result;
            logResult(result);
        } catch (Exception ex) {
            UpdateCheckResult result = new UpdateCheckResult(
                    UpdateCheckStatus.FAILED,
                    plugin.getDescription().getVersion(),
                    null,
                    projectPageUrl(),
                    Instant.now(),
                    ex.getMessage()
            );
            lastResult = result;
            plugin.getLogger().warning("[PocketDice] Failed to check for updates: " + ex.getMessage());
        }
    }

    private UpdateCheckResult performCheck() throws Exception {
        String currentVersion = plugin.getDescription().getVersion();
        URI uri = URI.create(MODRINTH_API_BASE + PROJECT_SLUG + "/version");

        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(uri)
                .timeout(Duration.ofSeconds(15))
                .header("User-Agent", "PocketDice/" + currentVersion)
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200) {
            throw new IllegalStateException("Unexpected HTTP status " + response.statusCode());
        }

        JsonElement element = JsonParser.parseString(response.body());
        if (!element.isJsonArray()) {
            throw new IllegalStateException("Invalid response from Modrinth.");
        }
        JsonArray versions = element.getAsJsonArray();
        if (versions.isEmpty()) {
            throw new IllegalStateException("No versions found on Modrinth.");
        }

        VersionCandidate best = null;
        for (JsonElement el : versions) {
            if (!el.isJsonObject()) continue;
            JsonObject obj = el.getAsJsonObject();
            String versionNumber = getAsString(obj, "version_number");
            if (versionNumber == null) continue;
            String versionType = getAsString(obj, "version_type");
            String publishedRaw = getAsString(obj, "date_published");
            Instant published = parseInstant(publishedRaw);
            String id = getAsString(obj, "id");
            String url = id != null ? projectPageUrl() + "/version/" + id : projectPageUrl();
            boolean release = "release".equalsIgnoreCase(versionType);
            VersionCandidate candidate = new VersionCandidate(versionNumber, url, published, release);
            if (best == null || isPreferred(candidate, best)) {
                best = candidate;
            }
        }

        if (best == null) {
            throw new IllegalStateException("No usable versions returned by Modrinth.");
        }

        String latestVersion = best.versionNumber();
        String url = best.url();
        int compare = compareVersions(currentVersion, latestVersion);
        UpdateCheckStatus status = compare < 0 ? UpdateCheckStatus.UPDATE_AVAILABLE : UpdateCheckStatus.UP_TO_DATE;

        return new UpdateCheckResult(
                status,
                currentVersion,
                latestVersion,
                url,
                Instant.now(),
                null
        );
    }

    private void logResult(UpdateCheckResult result) {
        if (result.getStatus() == UpdateCheckStatus.FAILED) {
            if (result.getErrorMessage() != null) {
                plugin.getLogger().warning("[PocketDice] Failed to check for updates: " + result.getErrorMessage());
            }
            return;
        }

        if (!notifyConsole) {
            return;
        }

        String key = switch (result.getStatus()) {
            case UPDATE_AVAILABLE -> "messages.update.available_console";
            case UP_TO_DATE -> "messages.update.up_to_date_console";
            default -> null;
        };
        if (key == null) return;
        String message = plugin.getLocaleManager().getDefault(key, placeholderMap(result));
        if (message != null && !message.isBlank()) {
            plugin.getLogger().info(Text.toLegacy(message));
        }
    }

    public UpdateCheckResult getLastResult() {
        return lastResult;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isNotifyAdminsOnJoin() {
        return notifyAdminsOnJoin;
    }

    public String getAdminNotifyPermission() {
        return adminNotifyPermission != null && !adminNotifyPermission.isBlank()
                ? adminNotifyPermission
                : UPDATE_NOTIFY_PERMISSION;
    }

    private boolean isPreferred(VersionCandidate candidate, VersionCandidate current) {
        if (candidate.release() && !current.release()) {
            return true;
        }
        if (candidate.release() == current.release()) {
            return candidate.published().isAfter(current.published());
        }
        return false;
    }

    private String getAsString(JsonObject obj, String key) {
        JsonElement el = obj.get(key);
        return el != null && !el.isJsonNull() ? el.getAsString() : null;
    }

    private Instant parseInstant(String value) {
        if (value == null || value.isBlank()) return Instant.EPOCH;
        try {
            return Instant.parse(value);
        } catch (Exception ignored) {
            return Instant.EPOCH;
        }
    }

    private int compareVersions(String current, String latest) {
        if (current == null || latest == null) return 0;
        String[] cParts = current.split("[.-]");
        String[] lParts = latest.split("[.-]");
        int len = Math.max(cParts.length, lParts.length);
        for (int i = 0; i < len; i++) {
            String c = i < cParts.length ? cParts[i] : "0";
            String l = i < lParts.length ? lParts[i] : "0";
            Integer ci = parseInt(c);
            Integer li = parseInt(l);
            if (ci != null && li != null) {
                int cmp = ci.compareTo(li);
                if (cmp != 0) return cmp < 0 ? -1 : 1;
            } else {
                int cmp = c.compareToIgnoreCase(l);
                if (cmp != 0) return cmp < 0 ? -1 : 1;
            }
        }
        return 0;
    }

    private Integer parseInt(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Map<String, String> placeholderMap(UpdateCheckResult res) {
        String url = res.getUrl() != null && !res.getUrl().isBlank() ? res.getUrl() : projectPageUrl();
        return Map.of(
                "current", safe(res.getCurrentVersion()),
                "latest", safe(res.getLatestVersion()),
                "url", url
        );
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String projectPageUrl() {
        return MODRINTH_PROJECT_PAGE + PROJECT_SLUG;
    }

    private record VersionCandidate(String versionNumber, String url, Instant published, boolean release) {
    }
}
