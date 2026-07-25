package com.autocraft.util;

/**
 * A simple stopwatch-style timer for tracking elapsed ticks / milliseconds.
 * Used by state machine states and retry trackers.
 */
public class TickTimer {

    private long startTimeMs;
    private boolean running;

    public TickTimer() {
        this.startTimeMs = 0;
        this.running = false;
    }

    /**
     * Start or restart the timer.
     */
    public void reset() {
        this.startTimeMs = System.currentTimeMillis();
        this.running = true;
    }

    /**
     * Stop the timer without resetting the accumulated time.
     */
    public void pause() {
        this.running = false;
    }

    /**
     * Resume from the current elapsed point.
     */
    public void resume() {
        if (!running) {
            // Adjust start time so elapsed stays continuous
            long elapsed = getElapsedMs();
            this.startTimeMs = System.currentTimeMillis() - elapsed;
            this.running = true;
        }
    }

    /**
     * Milliseconds since the timer was last reset.
     */
    public long getElapsedMs() {
        if (!running) return 0;
        return System.currentTimeMillis() - startTimeMs;
    }

    /**
     * Check whether the timer has exceeded a given timeout.
     */
    public boolean hasElapsed(long timeoutMs) {
        return running && getElapsedMs() >= timeoutMs;
    }

    /**
     * Seconds remaining until the timeout, or 0 if already elapsed.
     */
    public int getRemainingSeconds(long timeoutMs) {
        long remaining = timeoutMs - getElapsedMs();
        if (remaining <= 0) return 0;
        return (int) (remaining / 1000);
    }

    public boolean isRunning() {
        return running;
    }
}
