package com.autocraft.task.states;

import com.autocraft.task.CraftingTask;
import com.autocraft.task.TaskState;

public class IdleState implements TaskState {
    @Override public void enter(CraftingTask ctx) {}
    @Override public StateResult tick(CraftingTask ctx) {
        ctx.transitionTo(new PlanningState());
        return StateResult.SUCCESS;
    }
    @Override public void exit(CraftingTask ctx) {}
    @Override public String getDescription() { return "Initializing..."; }
}
