package com.autocraft.task;

import com.autocraft.util.TickTimer;

/**
 * Tracks retry attempts and timeout for a single container or operation.
 * Implements the rule: must check at least minChecks times AND exceed timeout before declaring failure.
 */
public class RetryTracker {

    private final TickTimer timer = new TickTimer();
    private final int timeoutMs;
    private final int minChecks;

    private int checkCount;
    private boolean active;

    public RetryTracker(int timeoutMs, int minChecks) {
        this.timeoutMs = timeoutMs;
        this.minChecks = minChecks;
        this.active = false;
        this.checkCount = 0;
    }

    /**
     * Start tracking a new retry cycle. Call when first failure is detected.
     */
    public void start() {
        timer.reset();
        checkCount = 0;
        active = true;
    }

    /**
     * Record that a check was performed (container was opened and scanned).
     */
    public void recordCheck() {
        checkCount++;
    }

    /**
     * Reset tracking — the issue has been resolved.
     */
    public void reset() {
        timer.reset();
        checkCount = 0;
        active = false;
    }

    /**
     * Has the retry timeout period been exceeded with the minimum required checks?
     */
    public boolean isTimedOut() {
        if (!active) return false;
        return timer.hasElapsed(timeoutMs) && checkCount >= minChecks;
    }

    /**
     * Seconds remaining before timeout triggers (given minChecks satisfied),
     * or 0 if timeout has already occurred.
     */
    public int getRemainingSeconds() {
        if (!active) return 0;
        return timer.getRemainingSeconds(timeoutMs);
    }

    /**
     * Whether a check is still needed (haven't reached minChecks yet).
     */
    public boolean needsMoreChecks() {
        return active && checkCount < minChecks;
    }

    public int getCheckCount() {
        return checkCount;
    }

    public int getMinChecks() {
        return minChecks;
    }

    public boolean isActive() {
        return active;
    }

    public long getElapsedMs() {
        return timer.getElapsedMs();
    }
}
