package com.autocraft.task.states;

import com.autocraft.task.CraftingTask;
import com.autocraft.task.TaskState;

/**
 * Stopped state — a stop condition was triggered.
 * The task is halted and awaits manual intervention.
 * The player must use /autocraft task stop to fully reset,
 * or resume after fixing the issue.
 */
public class StoppedState implements TaskState {

    @Override
    public void enter(CraftingTask ctx) {
        // Task has stopped, overlay shows stop info + reason
    }

    @Override
    public StateResult tick(CraftingTask ctx) {
        // Stay stopped until the player manually handles it
        return StateResult.RUNNING;
    }

    @Override
    public void exit(CraftingTask ctx) {
        // Reset when transitioning to idle
    }

    @Override
    public String getDescription() {
        return "Stopped — awaiting intervention";
    }
}
