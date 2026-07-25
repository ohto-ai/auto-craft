package com.autocraft.task.states;

import com.autocraft.AutoCraftMod;
import com.autocraft.recipe.CraftingRecipe;
import com.autocraft.task.CraftingTask;
import com.autocraft.task.MaterialPlanner;
import com.autocraft.task.TaskState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;
import java.util.*;

public class PlanningState implements TaskState {
    @Override public void enter(CraftingTask ctx) {
        AutoCraftMod.LOGGER.debug("Planning for '{}'", ctx.getRecipe().getName());
    }
    @Override public StateResult tick(CraftingTask ctx) {
        if (ctx.isComplete()) { ctx.stop("Target reached"); return StateResult.STOPPED; }
        int remaining = ctx.getRemainingCount();
        var plan = MaterialPlanner.plan(ctx.getRecipe(), remaining);
        ctx.setCurrentMaterialPlan(plan);

        // If nothing needed from containers, verify player actually has enough in inventory
        if (plan.needFromContainers.isEmpty()) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) {
                ScreenHandler handler = client.player.currentScreenHandler;
                if (handler != null) {
                    Map<String, Integer> playerHas = new HashMap<>();
                    for (int i = 0; i < handler.slots.size(); i++) {
                        ItemStack s = handler.slots.get(i).getStack();
                        if (!s.isEmpty()) {
                            String id = Registries.ITEM.getId(s.getItem()).toString();
                            playerHas.merge(id, s.getCount(), Integer::sum);
                        }
                    }
                    for (var e : plan.totalNeeded.entrySet()) {
                        int need = e.getValue();
                        int have = playerHas.getOrDefault(e.getKey(), 0);
                        if (have < need) {
                            AutoCraftMod.LOGGER.info("Need {} x{} but only have {} — fetching more",
                                    need, e.getKey(), have);
                            plan.needFromContainers.put(e.getKey(), need - have);
                        }
                    }
                }
            }
        }

        ctx.setCurrentInputIndex(0);
        AutoCraftMod.LOGGER.info("Plan: {} types needed from containers, {} crafts batch",
                plan.getUniqueItemTypesNeeded(), plan.batchSize);

        if (plan.needFromContainers.isEmpty()) {
            ctx.transitionTo(new NavigateToWorkbenchState());
        } else {
            ctx.transitionTo(new NavigateToInputState());
        }
        return StateResult.SUCCESS;
    }
    @Override public void exit(CraftingTask ctx) {}
    @Override public String getDescription() { return "Planning materials..."; }
}
