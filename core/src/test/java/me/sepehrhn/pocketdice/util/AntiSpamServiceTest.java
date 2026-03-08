package me.sepehrhn.pocketdice.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class AntiSpamServiceTest {

    private AntiSpamService service;
    private final UUID PLAYER = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new AntiSpamService();
    }

    // ---- Cooldown tests ----

    @Test
    void cooldownAllowsFirstRoll() {
        AntiSpamService.CheckResult result = service.checkCooldown(PLAYER, true, 3);
        assertTrue(result.isAllowed(), "First roll should always be allowed");
    }

    @Test
    void cooldownBlocksImmediateSecondRoll() {
        service.recordRoll(PLAYER);
        AntiSpamService.CheckResult result = service.checkCooldown(PLAYER, true, 3);
        assertFalse(result.isAllowed(), "Immediate second roll should be blocked");
        assertTrue(result.getRemainingSeconds() >= 1, "Remaining seconds should be >= 1");
        assertTrue(result.getRemainingSeconds() <= 3, "Remaining seconds should be <= cooldown");
    }

    @Test
    void cooldownAllowsWhenDisabled() {
        service.recordRoll(PLAYER);
        AntiSpamService.CheckResult result = service.checkCooldown(PLAYER, false, 3);
        assertTrue(result.isAllowed(), "Disabled cooldown should always allow");
    }

    @Test
    void cooldownAllowsAfterExpiry() throws InterruptedException {
        service.recordRoll(PLAYER);
        // Use very short cooldown (1 second) and wait 1.1 seconds
        Thread.sleep(1100);
        AntiSpamService.CheckResult result = service.checkCooldown(PLAYER, true, 1);
        assertTrue(result.isAllowed(), "Roll after cooldown expiry should be allowed");
    }

    // ---- Rate limit tests ----

    @Test
    void rateLimitAllowsFirstRolls() {
        for (int i = 0; i < 3; i++) {
            AntiSpamService.CheckResult r = service.checkRateLimit(PLAYER, true, 60, 5);
            assertTrue(r.isAllowed(), "Roll " + (i + 1) + " should be allowed");
            service.recordRoll(PLAYER);
        }
    }

    @Test
    void rateLimitBlocksAfterMaxRolls() {
        int maxRolls = 5;
        for (int i = 0; i < maxRolls; i++) {
            service.recordRoll(PLAYER);
        }
        AntiSpamService.CheckResult result = service.checkRateLimit(PLAYER, true, 60, maxRolls);
        assertFalse(result.isAllowed(), "Roll after max exceeded should be blocked");
        assertTrue(result.getRemainingSeconds() >= 1, "Remaining should be >= 1");
    }

    @Test
    void rateLimitAllowsWhenDisabled() {
        int maxRolls = 3;
        for (int i = 0; i < maxRolls + 2; i++) {
            service.recordRoll(PLAYER);
        }
        AntiSpamService.CheckResult result = service.checkRateLimit(PLAYER, false, 60, maxRolls);
        assertTrue(result.isAllowed(), "Disabled rate limit should always allow");
    }

    @Test
    void rateLimitAllowsAfterWindowExpiry() throws InterruptedException {
        // Use 1-second window, record max rolls, wait for window to pass
        int maxRolls = 3;
        for (int i = 0; i < maxRolls; i++) {
            service.recordRoll(PLAYER);
        }
        AntiSpamService.CheckResult blocked = service.checkRateLimit(PLAYER, true, 1, maxRolls);
        assertFalse(blocked.isAllowed(), "Should be blocked before window expires");

        Thread.sleep(1100); // wait for 1-second window to expire

        AntiSpamService.CheckResult allowed = service.checkRateLimit(PLAYER, true, 1, maxRolls);
        assertTrue(allowed.isAllowed(), "Should be allowed after window expires");
    }

    // ---- clearPlayer ----

    @Test
    void clearPlayerResetsState() {
        service.recordRoll(PLAYER);
        service.clearPlayer(PLAYER);
        AntiSpamService.CheckResult result = service.checkCooldown(PLAYER, true, 3);
        assertTrue(result.isAllowed(), "After clearPlayer, cooldown should be reset");
    }
}
