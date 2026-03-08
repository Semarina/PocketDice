package me.sepehrhn.pocketdice.fabric.update;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.sepehrhn.pocketdice.fabric.FabricConfig;
import me.sepehrhn.pocketdice.update.UpdateCheckResult;
import me.sepehrhn.pocketdice.update.UpdateCheckStatus;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class FabricUpdateChecker {

    private static final Logger LOGGER = LoggerFactory.getLogger("PocketDice");
    private static final String MODRINTH_API_BASE = "https://api.modrinth.com/v2/project/";
    private static final String MODRINTH_PROJECT_PAGE = "https://modrinth.com/plugin/";
    private static final String PROJECT_SLUG = "pocketdice";

    private final HttpClient httpClient;
    private ScheduledExecutorService scheduler;
    private volatile UpdateCheckResult lastResult;

    public FabricUpdateChecker() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    private synchronized void ensureScheduler() {
        if (scheduler == null || scheduler.isShutdown()) {
            this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "PocketDice-UpdateChecker");
                t.setDaemon(true);
                return t;
            });
        }
    }

    public void start() {
        if (!FabricConfig.DATA.updates.enabled) return;

        ensureScheduler();

        if (FabricConfig.DATA.updates.check_on_startup) {
            scheduler.execute(this::runCheck);
        }

        double intervalHours = FabricConfig.DATA.updates.check_interval_hours;
        if (intervalHours > 0) {
            long delay = (long) (intervalHours * 60 * 60);
            scheduler.scheduleAtFixedRate(this::runCheck, delay, delay, TimeUnit.SECONDS);
        }
    }

    public void shutdown() {
        scheduler.shutdownNow();
    }

    private void runCheck() {
        try {
            UpdateCheckResult result = performCheck();
            lastResult = result;
            logResult(result);
        } catch (Exception ex) {
            LOGGER.warn("[PocketDice] Failed to check for updates: {}", ex.getMessage());
        }
    }

    private UpdateCheckResult performCheck() throws Exception {
        String currentVersion = FabricLoader.getInstance()
                .getModContainer("pocketdice")
                .map(m -> m.getMetadata().getVersion().getFriendlyString())
                .orElse("0.0.0");

        URI uri = URI.create(MODRINTH_API_BASE + PROJECT_SLUG + "/version");
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(uri)
                .timeout(Duration.ofSeconds(15))
                .header("User-Agent", "PocketDice/" + currentVersion)
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200) {
            throw new IllegalStateException("HTTP " + response.statusCode());
        }

        JsonElement element = JsonParser.parseString(response.body());
        JsonArray versions = element.getAsJsonArray();
        if (versions.isEmpty()) throw new IllegalStateException("No versions found");

        VersionCandidate best = null;
        for (JsonElement el : versions) {
            if (!el.isJsonObject()) continue;
            JsonObject obj = el.getAsJsonObject();
            
            // Check loader compatibility (optional but good practice)
            JsonArray loaders = obj.getAsJsonArray("loaders");
            boolean fabricCompatible = false;
            if (loaders != null) {
                for (JsonElement l : loaders) {
                    if (l.getAsString().equalsIgnoreCase("fabric") || l.getAsString().equalsIgnoreCase("quilt")) {
                        fabricCompatible = true;
                        break;
                    }
                }
            }
            if (!fabricCompatible) continue;

            String vNum = obj.get("version_number").getAsString();
            String vType = obj.get("version_type").getAsString();
            String date = obj.get("date_published").getAsString();
            String id = obj.get("id").getAsString();
            String url = MODRINTH_PROJECT_PAGE + PROJECT_SLUG + "/version/" + id;
            
            VersionCandidate cand = new VersionCandidate(vNum, url, Instant.parse(date), "release".equalsIgnoreCase(vType));
            
            if (best == null || isPreferred(cand, best)) {
                best = cand;
            }
        }

        if (best == null) throw new IllegalStateException("No compatible versions found");

        UpdateCheckStatus status = compareVersions(currentVersion, best.versionNumber) < 0 
                ? UpdateCheckStatus.UPDATE_AVAILABLE : UpdateCheckStatus.UP_TO_DATE;

        return new UpdateCheckResult(status, currentVersion, best.versionNumber, best.url, Instant.now(), null);
    }

    private void logResult(UpdateCheckResult result) {
        if (!FabricConfig.DATA.updates.notify_console) return;
        if (result.getStatus() == UpdateCheckStatus.UPDATE_AVAILABLE) {
            LOGGER.info("[PocketDice] A new update is available: {} -> {}", result.getCurrentVersion(), result.getLatestVersion());
            LOGGER.info("[PocketDice] Download: {}", result.getUrl());
        }
    }

    public UpdateCheckResult getLastResult() {
        return lastResult;
    }

    // Helper methods (compareVersions, isPreferred) mostly identical to Paper version
    private boolean isPreferred(VersionCandidate c, VersionCandidate cur) {
        if (c.release && !cur.release) return true;
        if (c.release == cur.release) return c.published.isAfter(cur.published);
        return false;
    }

    private int compareVersions(String current, String latest) {
        if (current == null || latest == null) return 0;
        String[] cParts = current.split("[.-]");
        String[] lParts = latest.split("[.-]");
        int len = Math.max(cParts.length, lParts.length);
        for (int i = 0; i < len; i++) {
            String c = i < cParts.length ? cParts[i] : "0";
            String l = i < lParts.length ? lParts[i] : "0";
            try {
                int ci = Integer.parseInt(c);
                int li = Integer.parseInt(l);
                int cmp = Integer.compare(ci, li);
                if (cmp != 0) return cmp < 0 ? -1 : 1;
            } catch (NumberFormatException e) {
                int cmp = c.compareToIgnoreCase(l);
                if (cmp != 0) return cmp < 0 ? -1 : 1;
            }
        }
        return 0;
    }

    private record VersionCandidate(String versionNumber, String url, Instant published, boolean release) {}
}
