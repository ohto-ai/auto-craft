package com.autocraft.task.states;

import com.autocraft.AutoCraftMod;
import com.autocraft.interaction.ContainerInteractor;
import com.autocraft.task.CraftingTask;
import com.autocraft.task.TaskState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;

/**
 * Takes crafting results repeatedly until the grid is exhausted.
 * Shift-clicks the result slot, then checks if more results appear
 * (more materials remain in grid). Loops until grid is empty.
 */
public class TakeResultState implements TaskState {

    private enum Phase { WAIT_FIRST, TAKE, CHECK_MORE, CLOSE, DONE }
    private Phase phase;
    private int tickInPhase;
    private int timesTaken;
    private int batchSize;

    @Override public void enter(CraftingTask ctx) {
        phase = Phase.WAIT_FIRST;
        tickInPhase = 0;
        timesTaken = 0;
        var plan = ctx.getCurrentMaterialPlan();
        batchSize = plan != null ? plan.batchSize : 1;
    }

    @Override public StateResult tick(CraftingTask ctx) {
        tickInPhase++;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return StateResult.RETRY;
        ScreenHandler handler = client.player.currentScreenHandler;
        int syncId = handler.syncId;

        switch (phase) {
            case WAIT_FIRST:
                if (!handler.slots.get(0).getStack().isEmpty()) {
                    phase = Phase.TAKE;
                    tickInPhase = 0;
                    return StateResult.RUNNING;
                }
                if (tickInPhase > 40) {
                    AutoCraftMod.LOGGER.warn("No result after 2s");
                    ctx.transitionTo(new PlaceRecipeState());
                    return StateResult.SUCCESS;
                }
                return StateResult.RUNNING;

            case TAKE:
                if (tickInPhase == 1) {
                    ContainerInteractor.quickMove(syncId, 0);
                    return StateResult.RUNNING;
                }
                timesTaken++;
                phase = Phase.CHECK_MORE;
                tickInPhase = 0;
                return StateResult.RUNNING;

            case CHECK_MORE:
                // Wait 2 ticks for server to update, then check if result slot has more
                if (tickInPhase < 3) return StateResult.RUNNING;
                if (!handler.slots.get(0).getStack().isEmpty()) {
                    phase = Phase.TAKE; // more results to take
                    tickInPhase = 0;
                    return StateResult.RUNNING;
                }
                // Grid exhausted — done
                phase = Phase.CLOSE;
                tickInPhase = 0;
                return StateResult.RUNNING;

            case CLOSE:
                ContainerInteractor.closeContainer();
                // All crafts from this grid fill are done
                int yield = ctx.getRecipe().getResult().count;
                ctx.addCompletedCount(yield * batchSize);
                AutoCraftMod.LOGGER.debug("Batch done: {}x{}={} (total: {}/{})",
                        batchSize, yield, yield * batchSize,
                        ctx.getCompletedCount(),
                        ctx.isInfinite() ? "∞" : ctx.getTargetCount());
                phase = Phase.DONE;
                return StateResult.RUNNING;

            case DONE:
                ctx.transitionTo(new NavigateToOutputState());
                return StateResult.SUCCESS;
        }
        return StateResult.RUNNING;
    }

    @Override public void exit(CraftingTask ctx) {}
    @Override public String getDescription() { return "Taking results..."; }
}
