package com.autocraft.task.states;

import com.autocraft.task.CraftingTask;
import com.autocraft.task.TaskState;

/**
 * Paused state — player pressed space or issued pause command.
 * Idle ticks until resumed. Does not advance the state machine.
 */
public class PausedState implements TaskState {

    @Override
    public void enter(CraftingTask ctx) {
        // Task is paused, overlay shows pause info
    }

    @Override
    public StateResult tick(CraftingTask ctx) {
        if (!ctx.isPaused()) {
            // Resume to previous state
            TaskState previous = ctx.getPreviousState();
            if (previous != null) {
                ctx.transitionTo(previous);
            }
            return StateResult.SUCCESS;
        }
        return StateResult.RUNNING;
    }

    @Override
    public void exit(CraftingTask ctx) {
        // No cleanup
    }

    @Override
    public String getDescription() {
        return "Paused";
    }
}
