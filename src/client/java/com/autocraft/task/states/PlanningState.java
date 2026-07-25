package com.autocraft.task.states;

import com.autocraft.AutoCraftMod;
import com.autocraft.task.CraftingTask;
import com.autocraft.task.MaterialPlanner;
import com.autocraft.task.TaskState;

public class PlanningState implements TaskState {
    @Override public void enter(CraftingTask ctx) {
        AutoCraftMod.LOGGER.debug("Planning materials for '{}'", ctx.getRecipe().getName());
    }
    @Override public StateResult tick(CraftingTask ctx) {
        if (ctx.isComplete()) { ctx.stop("Target reached"); return StateResult.STOPPED; }
        int remaining = ctx.getRemainingCount();
        var plan = MaterialPlanner.plan(ctx.getRecipe(), remaining);
        ctx.setCurrentMaterialPlan(plan);
        ctx.setCurrentInputIndex(0);
        ctx.resetAllRetryTrackers();
        AutoCraftMod.LOGGER.debug("Plan: {} types needed, {} crafts", plan.getUniqueItemTypesNeeded(), plan.batchSize);
        ctx.transitionTo(new NavigateToInputState());
        return StateResult.SUCCESS;
    }
    @Override public void exit(CraftingTask ctx) {}
    @Override public String getDescription() { return "Planning materials..."; }
}
