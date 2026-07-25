package com.autocraft.task;

/**
 * Interface for all task states in the crafting state machine.
 * Each state represents one phase of the automated crafting process.
 */
public interface TaskState {

    /**
     * Called once when entering this state.
     */
    void enter(CraftingTask ctx);

    /**
     * Called every client tick while this state is active.
     * Returns the action the executor should take.
     */
    StateResult tick(CraftingTask ctx);

    /**
     * Called once when leaving this state.
     */
    void exit(CraftingTask ctx);

    /**
     * Human-readable description of what this state is doing (for overlay display).
     */
    String getDescription();

    /**
     * Result of a state tick, indicating what the TaskExecutor should do next.
     */
    enum StateResult {
        /** Stay in this state, continue ticking. */
        RUNNING,
        /** State completed successfully, advance to next state. */
        SUCCESS,
        /** State failed with a retryable error, retry from current state. */
        RETRY,
        /** State failed with an unrecoverable error, transition to STOPPED. */
        STOPPED
    }
}
