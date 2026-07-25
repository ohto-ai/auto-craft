package com.autocraft.task.states;

import com.autocraft.AutoCraftMod;
import com.autocraft.interaction.ContainerInteractor;
import com.autocraft.interaction.InventoryAnalyzer;
import com.autocraft.recipe.CraftingRecipe;
import com.autocraft.task.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.math.BlockPos;
import java.util.*;

/**
 * Deposits crafted items into the output container.
 * If output is full, retries with timeout.
 */
public class DepositResultState implements TaskState {

    private enum Phase { OPENING, FINDING_RESULT, DEPOSITING, VERIFY, DONE }
    private Phase phase;
    private int tickInPhase;
    private int depositSlot;
    private int depositedCount;

    @Override public void enter(CraftingTask ctx) {
        phase = Phase.OPENING;
        tickInPhase = 0;
        BlockPos out = ctx.getStation().getOutput();
        ContainerInteractor.openContainer(out);
    }

    @Override public StateResult tick(CraftingTask ctx) {
        tickInPhase++;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return StateResult.RETRY;
        ScreenHandler handler = client.player.currentScreenHandler;

        switch (phase) {
            case OPENING:
                if (ContainerInteractor.isContainerOpen()) {
                    phase = Phase.FINDING_RESULT;
                    tickInPhase = 0;
                    return StateResult.RUNNING;
                }
                if (tickInPhase > 20) {
                    ContainerInteractor.openContainer(ctx.getStation().getOutput());
                    tickInPhase = 0;
                }
                return StateResult.RUNNING;

            case FINDING_RESULT: {
                // Find the crafted result item in player inventory
                CraftingRecipe recipe = ctx.getRecipe();
                String resultId = recipe.getResult().itemId;
                int syncId = handler.syncId;
                depositSlot = -1;

                for (int i = handler.slots.size() - 36; i < handler.slots.size(); i++) {
                    ItemStack stack = handler.slots.get(i).getStack();
                    if (!stack.isEmpty()) {
                        String id = net.minecraft.registry.Registries.ITEM.getId(stack.getItem()).toString();
                        if (id.equals(resultId)) {
                            depositSlot = i;
                            break;
                        }
                    }
                }

                if (depositSlot < 0) {
                    // No result items in inventory — maybe already deposited or different
                    AutoCraftMod.LOGGER.debug("No result items found in player inventory");
                    phase = Phase.DONE;
                    return StateResult.RUNNING;
                }

                phase = Phase.DEPOSITING;
                tickInPhase = 0;
                return StateResult.RUNNING;
            }

            case DEPOSITING:
                if (tickInPhase == 1) {
                    ContainerInteractor.quickMove(handler.syncId, depositSlot);
                    return StateResult.RUNNING;
                }
                depositedCount++;
                phase = Phase.VERIFY;
                tickInPhase = 1;
                return StateResult.RUNNING;

            case VERIFY: {
                // Check if output container is full by trying to find if the item is still in player inventory
                if (tickInPhase > 3) {
                    BlockPos out = ctx.getStation().getOutput();
                    // Check if item is still there (output full?)
                    boolean stillInInventory = false;
                    for (int i = handler.slots.size() - 36; i < handler.slots.size(); i++) {
                        ItemStack stack = handler.slots.get(i).getStack();
                        if (!stack.isEmpty()) {
                            String id = net.minecraft.registry.Registries.ITEM.getId(stack.getItem()).toString();
                            if (id.equals(ctx.getRecipe().getResult().itemId)) {
                                stillInInventory = true;
                                break;
                            }
                        }
                    }

                    if (stillInInventory) {
                        // Output might be full
                        RetryTracker tracker = ctx.getOrCreateRetryTracker(out);
                        if (!tracker.isActive()) tracker.start();
                        tracker.recordCheck();
                        if (tracker.isTimedOut()) {
                            ContainerInteractor.closeContainer();
                            ctx.stop("Output full — timeout");
                            return StateResult.STOPPED;
                        }
                        // Retry deposit
                        phase = Phase.FINDING_RESULT;
                        tickInPhase = 0;
                        return StateResult.RUNNING;
                    }

                    // Success — reset tracker
                    ctx.getOrCreateRetryTracker(out).reset();
                    phase = Phase.DONE;
                    return StateResult.RUNNING;
                }
                tickInPhase++;
                return StateResult.RUNNING;
            }

            case DONE:
                ContainerInteractor.closeContainer();
                // Check goal
                if (ctx.isComplete()) {
                    ctx.stop("Target reached — complete");
                    return StateResult.STOPPED;
                }
                // Loop for next batch
                ctx.transitionTo(new PlanningState());
                return StateResult.SUCCESS;
        }
        return StateResult.RUNNING;
    }

    @Override public void exit(CraftingTask ctx) {}
    @Override public String getDescription() { return "Depositing items..."; }
}
