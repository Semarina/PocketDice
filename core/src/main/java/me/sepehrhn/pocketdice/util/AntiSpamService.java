package me.sepehrhn.pocketdice.util;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Platform-agnostic, thread-safe service that enforces per-player
 * cooldowns and sliding-window rate limits for dice rolls.
 *
 * <p>All time comparisons use {@link System#nanoTime()} (monotonic clock)
 * to avoid wall-clock drift or daylight-saving jumps.
 */
public final class AntiSpamService {

    /** Result returned by check methods. */
    public static final class CheckResult {
        private final boolean allowed;
        /** Remaining seconds (ceiling), only meaningful when {@code !allowed}. */
        private final int remainingSeconds;

        private CheckResult(boolean allowed, int remainingSeconds) {
            this.allowed = allowed;
            this.remainingSeconds = remainingSeconds;
        }

        public boolean isAllowed() { return allowed; }
        public int getRemainingSeconds() { return remainingSeconds; }

        public static final CheckResult ALLOWED = new CheckResult(true, 0);
        public static CheckResult blocked(int remaining) { return new CheckResult(false, remaining); }
    }

    // --- Cooldown state ---
    private final ConcurrentHashMap<UUID, Long> lastRollNs = new ConcurrentHashMap<>();

    // --- Rate-limit state ---
    private final ConcurrentHashMap<UUID, ArrayDeque<Long>> rollWindows = new ConcurrentHashMap<>();

    /**
     * Check whether the player passes the cooldown.
     *
     * @param playerId        the player's UUID
     * @param enabled         whether the cooldown is active
     * @param cooldownSeconds minimum seconds between rolls
     * @return {@link CheckResult#ALLOWED} or a blocked result with remaining seconds
     */
    public CheckResult checkCooldown(UUID playerId, boolean enabled, int cooldownSeconds) {
        if (!enabled || cooldownSeconds <= 0) return CheckResult.ALLOWED;

        long cooldownNs = (long) cooldownSeconds * 1_000_000_000L;
        Long last = lastRollNs.get(playerId);
        if (last == null) return CheckResult.ALLOWED;

        long elapsed = System.nanoTime() - last;
        if (elapsed >= cooldownNs) return CheckResult.ALLOWED;

        long remainingNs = cooldownNs - elapsed;
        int remainingSecs = (int) Math.ceil(remainingNs / 1_000_000_000.0);
        return CheckResult.blocked(Math.max(1, remainingSecs));
    }

    /**
     * Check whether the player passes the sliding-window rate limit.
     *
     * @param playerId      the player's UUID
     * @param enabled       whether the rate limit is active
     * @param windowSeconds rolling time window size in seconds
     * @param maxRolls      maximum rolls allowed within the window
     * @return {@link CheckResult#ALLOWED} or a blocked result with remaining seconds
     */
    public CheckResult checkRateLimit(UUID playerId, boolean enabled, int windowSeconds, int maxRolls) {
        if (!enabled || windowSeconds <= 0 || maxRolls <= 0) return CheckResult.ALLOWED;

        long windowNs = (long) windowSeconds * 1_000_000_000L;
        long now = System.nanoTime();

        ArrayDeque<Long> deque = rollWindows.computeIfAbsent(playerId, k -> new ArrayDeque<>());

        synchronized (deque) {
            // Prune entries older than the window
            Iterator<Long> it = deque.iterator();
            while (it.hasNext()) {
                if (now - it.next() > windowNs) it.remove();
                else break; // deque is insertion-ordered; once one is fresh, all following are too
            }

            if (deque.size() < maxRolls) return CheckResult.ALLOWED;

            // Blocked: remaining = window - (now - oldest)
            long oldest = deque.peekFirst();
            long remainingNs = windowNs - (now - oldest);
            int remainingSecs = (int) Math.ceil(remainingNs / 1_000_000_000.0);
            return CheckResult.blocked(Math.max(1, remainingSecs));
        }
    }

    /**
     * Record a successful roll. Must be called <em>after</em> both checks pass.
     *
     * @param playerId the player's UUID
     */
    public void recordRoll(UUID playerId) {
        long now = System.nanoTime();
        lastRollNs.put(playerId, now);

        ArrayDeque<Long> deque = rollWindows.computeIfAbsent(playerId, k -> new ArrayDeque<>());
        synchronized (deque) {
            deque.addLast(now);
        }
    }

    /**
     * Clear all stored state for a specific player (e.g., on disconnect).
     *
     * @param playerId the player's UUID
     */
    public void clearPlayer(UUID playerId) {
        lastRollNs.remove(playerId);
        rollWindows.remove(playerId);
    }
}
