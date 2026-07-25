package com.autocraft.task;

import com.autocraft.AutoCraftMod;
import com.autocraft.recipe.CraftingRecipe;
import com.autocraft.recipe.RecipeManager;
import com.autocraft.station.StationConfig;
import com.autocraft.station.StationManager;
import com.autocraft.task.states.IdleState;
import com.autocraft.task.states.PausedState;
import com.autocraft.task.states.StoppedState;

/**
 * Central task execution engine.
 * Owns the active CraftingTask, drives the state machine on each client tick,
 * and coordinates all subsystems (navigation, interaction, overlay).
 */
public class TaskExecutor {

    private final RecipeManager recipeManager;
    private final StationManager stationManager;

    private CraftingTask activeTask;
    private boolean reported;

    // Flag to track if space was pressed for pause toggle
    private boolean spacePressedLastTick;

    public TaskExecutor(RecipeManager recipeManager, StationManager stationManager) {
        this.recipeManager = recipeManager;
        this.stationManager = stationManager;
    }

    // --- Public API (called by commands) ---

    /**
     * Start a crafting task. Returns error message or null on success.
     */
    public String start(String stationName, int targetCount) {
        if (activeTask != null) {
            if (activeTask.isStopped()) {
                activeTask = null; // clear old stopped task
            } else {
                return "A task is already active. Use /autocraft task stop first.";
            }
        }

        StationConfig station = stationManager.getByName(stationName);
        if (station == null) {
            return "Station '" + stationName + "' not found.";
        }

        String validationError = station.getValidationError();
        if (validationError != null) {
            return "Station incomplete: " + validationError;
        }

        CraftingRecipe recipe = recipeManager.getByName(station.getBoundRecipeName());
        if (recipe == null) {
            return "Bound recipe '" + station.getBoundRecipeName() + "' not found.";
        }

        activeTask = new CraftingTask(station, recipe, targetCount);
        activeTask.transitionTo(new IdleState());
        reported = false;

        AutoCraftMod.LOGGER.info("Task started: station={}, recipe={}, target={}",
                stationName, recipe.getName(), targetCount < 0 ? "infinite" : targetCount);
        return null; // success
    }

    public String pause() {
        if (activeTask == null) return "No active task.";
        if (activeTask.isPaused()) return "Task is already paused.";
        activeTask.pause();
        AutoCraftMod.LOGGER.info("Task paused");
        return null;
    }

    public String resume() {
        if (activeTask == null) return "No active task.";
        if (!activeTask.isPaused()) return "Task is not paused.";
        activeTask.resume();
        AutoCraftMod.LOGGER.info("Task resumed");
        return null;
    }

    public String stop() {
        if (activeTask == null) return "No active task.";
        String reason = "Manually stopped by player";
        activeTask.stop(reason);
        AutoCraftMod.LOGGER.info("Task stopped: {}", reason);
        return null;
    }

    public boolean hasActiveTask() {
        return activeTask != null && !activeTask.isStopped();
    }

    public boolean isRunning() {
        return activeTask != null && !activeTask.isPaused() && !activeTask.isStopped();
    }

    public CraftingTask getActiveTask() {
        return activeTask;
    }

    /**
     * Reset the executor to allow a new task (after a task has stopped).
     */
    public void reset() {
        activeTask = null;
    }

    // --- Tick driver (called from ClientTickEvents.START_CLIENT_TICK) ---

    public void tick() {
        if (activeTask == null) return;

        // Handle space key pause toggle
        handleSpaceKey();

        if (activeTask.isStopped()) {
            if (!reported) {
                TaskReport.send(activeTask);
                reported = true;
            }
            return;
        }

        if (activeTask.isPaused()) {
            return; // paused, do nothing
        }

        // Delegate to current state
        TaskState currentState = activeTask.getCurrentState();
        if (currentState == null) return;

        TaskState.StateResult result = currentState.tick(activeTask);

        switch (result) {
            case RUNNING:
                // Continue ticking this state
                break;
            case SUCCESS:
                handleStateSuccess(currentState);
                break;
            case RETRY:
                // Stay in current state, it handles its own retry logic
                break;
            case STOPPED:
                // The state should have already set the stop reason
                if (!activeTask.isStopped()) {
                    activeTask.stop("Unknown error in state: " + currentState.getDescription());
                }
                break;
        }
    }

    private void handleStateSuccess(TaskState completedState) {
        // State machine transitions will be handled within each state class
        // by calling activeTask.transitionTo(nextState)
        // The states themselves know the transition graph
    }

    /**
     * Check for space key press to toggle pause.
     */
    private void handleSpaceKey() {
        // TODO: This will be refined when the overlay input handler is implemented.
        // For now, the overlay/input system manages space key detection.
        // This is a placeholder hook.
    }

    /**
     * Called from the overlay input handler when space is pressed.
     */
    public void onSpacePressed() {
        if (activeTask == null) return;

        if (activeTask.isStopped()) {
            // Can't unpause a stopped task via space
            return;
        }

        if (activeTask.isPaused()) {
            activeTask.resume();
            AutoCraftMod.LOGGER.info("Task resumed via space key");
        } else {
            activeTask.pause();
            AutoCraftMod.LOGGER.info("Task paused via space key");
        }
    }
}
