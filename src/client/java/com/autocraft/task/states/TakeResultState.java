package com.autocraft.task.states;

import com.autocraft.AutoCraftMod;
import com.autocraft.config.ModConfig;
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
 *
 * Includes rate-limited clicks and verification that the result
 * was actually taken before checking for more.
 */
public class TakeResultState implements TaskState {

    private enum Phase { WAIT_FIRST, TAKE, VERIFY_TAKE, CHECK_MORE, CLOSE, DONE }
    private Phase phase;
    private int tickInPhase;
    private int batchSize;
    private int takeRetryCount; // retry counter for failed takes

    @Override public void enter(CraftingTask ctx) {
        phase = Phase.WAIT_FIRST;
        tickInPhase = 0;
        takeRetryCount = 0;
        var plan = ctx.getCurrentMaterialPlan();
        batchSize = plan != null ? plan.batchSize : 1;
    }

    @Override public StateResult tick(CraftingTask ctx) {
        tickInPhase++;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return StateResult.RETRY;
        ScreenHandler handler = client.player.currentScreenHandler;
        int syncId = handler.syncId;

        ModConfig config = AutoCraftMod.getInstance().getConfig();

        switch (phase) {
            case WAIT_FIRST:
                if (!handler.slots.get(0).getStack().isEmpty()) {
                    AutoCraftMod.LOGGER.debug("Result appeared in output slot");
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
                // Wait for click delay before sending quickMove
                if (tickInPhase <= config.getClickDelayTicks()) {
                    return StateResult.RUNNING;
                }
                ItemStack resultStack = handler.slots.get(0).getStack();
                if (resultStack.isEmpty()) {
                    // Result disappeared — grid may be exhausted
                    phase = Phase.CHECK_MORE;
                    tickInPhase = 0;
                    return StateResult.RUNNING;
                }
                AutoCraftMod.LOGGER.debug("Taking result: {} x{}",
                        net.minecraft.registry.Registries.ITEM.getId(resultStack.getItem()),
                        resultStack.getCount());
                ContainerInteractor.quickMove(syncId, 0);
                takeRetryCount = 0;
                phase = Phase.VERIFY_TAKE;
                tickInPhase = 0;
                return StateResult.RUNNING;

            case VERIFY_TAKE:
                // Wait for server to sync before checking if result was taken
                if (tickInPhase <= config.getVerifyWaitTicks()) {
                    return StateResult.RUNNING;
                }
                // Check if result slot is now empty (item was taken)
                if (!handler.slots.get(0).getStack().isEmpty()) {
                    takeRetryCount++;
                    if (takeRetryCount < config.getMaxVerifyRetries()) {
                        AutoCraftMod.LOGGER.warn("Result still in slot after take — retry {}/{}",
                                takeRetryCount, config.getMaxVerifyRetries());
                        phase = Phase.TAKE;
                        tickInPhase = 0;
                        return StateResult.RUNNING;
                    }
                    AutoCraftMod.LOGGER.warn("Result still in slot after {} retries — proceeding anyway",
                            takeRetryCount);
                } else {
                    AutoCraftMod.LOGGER.debug("Result taken successfully");
                }
                phase = Phase.CHECK_MORE;
                tickInPhase = 0;
                return StateResult.RUNNING;

            case CHECK_MORE:
                // Wait for server to update, then check if result slot has more
                if (tickInPhase < config.getVerifyWaitTicks()) return StateResult.RUNNING;
                if (!handler.slots.get(0).getStack().isEmpty()) {
                    AutoCraftMod.LOGGER.debug("More results available — taking again");
                    phase = Phase.TAKE;
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
