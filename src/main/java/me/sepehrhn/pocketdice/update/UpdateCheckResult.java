package me.sepehrhn.pocketdice.update;

import java.time.Instant;

public class UpdateCheckResult {
    private final UpdateCheckStatus status;
    private final String currentVersion;
    private final String latestVersion;
    private final String url;
    private final Instant checkedAt;
    private final String errorMessage;

    public UpdateCheckResult(UpdateCheckStatus status, String currentVersion, String latestVersion, String url, Instant checkedAt, String errorMessage) {
        this.status = status;
        this.currentVersion = currentVersion;
        this.latestVersion = latestVersion;
        this.url = url;
        this.checkedAt = checkedAt;
        this.errorMessage = errorMessage;
    }

    public UpdateCheckStatus getStatus() {
        return status;
    }

    public String getCurrentVersion() {
        return currentVersion;
    }

    public String getLatestVersion() {
        return latestVersion;
    }

    public String getUrl() {
        return url;
    }

    public Instant getCheckedAt() {
        return checkedAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
