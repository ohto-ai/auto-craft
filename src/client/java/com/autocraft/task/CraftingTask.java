package com.autocraft.task;

import com.autocraft.AutoCraftMod;
import com.autocraft.recipe.CraftingRecipe;
import com.autocraft.station.StationConfig;
import com.autocraft.task.MaterialPlanner.MaterialPlan;
import net.minecraft.util.math.BlockPos;
import net.minecraft.item.ItemStack;

import java.util.*;

/**
 * Runtime context for a single crafting task execution.
 * Holds all mutable state: progress, current state, material plans, retry status.
 */
public class CraftingTask {

    // --- Immutable task identity ---
    private final StationConfig station;
    private final CraftingRecipe recipe;
    private final int targetCount;      // -1 = infinite
    private final long startTimeMs;

    // --- Mutable progress ---
    private int completedCount;
    private TaskState currentState;
    private TaskState previousState;    // for pause resume
    private boolean paused;
    private boolean stopped;
    private String stopReason;

    // --- Per-cycle material plan ---
    private MaterialPlan currentMaterialPlan;
    private int currentInputIndex;

    // --- Retry tracking ---
    private final Map<BlockPos, RetryTracker> retryTrackers = new HashMap<>();
    private final int retryTimeoutMs;
    private final int minRetryChecks;

    // --- Statistics ---
    private int totalCraftsCompleted;
    private long totalTimeSpentCrafting; // accumulated ms (excludes pause time)

    public CraftingTask(StationConfig station, CraftingRecipe recipe, int targetCount) {
        this.station = station;
        this.recipe = recipe;
        this.targetCount = targetCount;
        this.startTimeMs = System.currentTimeMillis();
        this.completedCount = 0;
        this.paused = false;
        this.stopped = false;

        var config = AutoCraftMod.getInstance().getConfig();
        this.retryTimeoutMs = config.getRetryTimeoutMs();
        this.minRetryChecks = config.getMinRetryChecks();
    }

    // --- Convenience queries ---

    public boolean isInfinite() {
        return targetCount < 0;
    }

    public boolean isComplete() {
        return !isInfinite() && completedCount >= targetCount;
    }

    public int getRemainingCount() {
        if (isInfinite()) return Integer.MAX_VALUE;
        return Math.max(0, targetCount - completedCount);
    }

    public double getProgressFraction() {
        if (isInfinite()) return -1;
        if (targetCount == 0) return 1.0;
        return Math.min(1.0, (double) completedCount / targetCount);
    }

    // --- Retry tracking ---

    public RetryTracker getOrCreateRetryTracker(BlockPos containerPos) {
        return retryTrackers.computeIfAbsent(containerPos,
                k -> new RetryTracker(retryTimeoutMs, minRetryChecks));
    }

    public void resetAllRetryTrackers() {
        retryTrackers.clear();
    }

    public Map<BlockPos, RetryTracker> getRetryTrackers() {
        return Collections.unmodifiableMap(retryTrackers);
    }

    // --- State management ---

    public void transitionTo(TaskState newState) {
        if (currentState != null) {
            currentState.exit(this);
        }
        this.previousState = this.currentState;
        this.currentState = newState;
        if (newState != null) {
            newState.enter(this);
        }
    }

    public void pause() {
        if (!paused) {
            this.paused = true;
            this.previousState = this.currentState;
        }
    }

    public void resume() {
        if (paused) {
            this.paused = false;
        }
    }

    public void stop(String reason) {
        this.stopped = true;
        this.stopReason = reason;
        this.paused = false;
    }

    // --- Simple getters/setters ---

    public StationConfig getStation() { return station; }
    public CraftingRecipe getRecipe() { return recipe; }
    public int getTargetCount() { return targetCount; }
    public int getCompletedCount() { return completedCount; }
    public void addCompletedCount(int count) { this.completedCount += count; this.totalCraftsCompleted += count; }
    public TaskState getCurrentState() { return currentState; }
    public TaskState getPreviousState() { return previousState; }
    public boolean isPaused() { return paused; }
    public boolean isStopped() { return stopped; }
    public String getStopReason() { return stopReason; }
    public long getStartTimeMs() { return startTimeMs; }
    public MaterialPlan getCurrentMaterialPlan() { return currentMaterialPlan; }
    public void setCurrentMaterialPlan(MaterialPlan plan) { this.currentMaterialPlan = plan; }
    public int getCurrentInputIndex() { return currentInputIndex; }
    public void setCurrentInputIndex(int index) { this.currentInputIndex = index; }
    public int getTotalCraftsCompleted() { return totalCraftsCompleted; }
    public long getTotalTimeSpentCrafting() { return totalTimeSpentCrafting; }
    public void addTimeSpentCrafting(long ms) { this.totalTimeSpentCrafting += ms; }
}
