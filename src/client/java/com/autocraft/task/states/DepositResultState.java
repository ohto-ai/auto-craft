package com.autocraft.task.states;

import com.autocraft.AutoCraftMod;
import com.autocraft.config.ModConfig;
import com.autocraft.interaction.ContainerInteractor;
import com.autocraft.recipe.CraftingRecipe;
import com.autocraft.task.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.util.math.BlockPos;

/**
 * Deposits crafted items into the output container.
 * If output is full, retries with timeout.
 * Verification checks that items actually left the player's inventory.
 */
public class DepositResultState implements TaskState {

    private enum Phase { OPENING, FINDING_RESULT, DEPOSITING, VERIFY, DONE }
    private Phase phase;
    private int tickInPhase;
    private int depositSlot;
    private int depositRetryCount; // prevent infinite retry on same slot

    @Override public void enter(CraftingTask ctx) {
        phase = Phase.OPENING;
        tickInPhase = 0;
        depositRetryCount = 0;
        BlockPos out = ctx.getStation().getOutput();
        ContainerInteractor.openContainer(out);
    }

    @Override public StateResult tick(CraftingTask ctx) {
        tickInPhase++;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return StateResult.RETRY;
        ScreenHandler handler = client.player.currentScreenHandler;

        ModConfig config = AutoCraftMod.getInstance().getConfig();

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

                // Reset retry counter for this specific deposit
                depositRetryCount = 0;
                phase = Phase.DEPOSITING;
                tickInPhase = 0;
                return StateResult.RUNNING;
            }

            case DEPOSITING:
                if (tickInPhase == 1) {
                    ItemStack stack = handler.slots.get(depositSlot).getStack();
                    AutoCraftMod.LOGGER.info("Depositing {} x{} from slot {}",
                            net.minecraft.registry.Registries.ITEM.getId(stack.getItem()),
                            stack.getCount(), depositSlot);
                    ContainerInteractor.quickMove(handler.syncId, depositSlot);
                    return StateResult.RUNNING;
                }
                // Wait for click delay before verifying
                if (tickInPhase <= config.getClickDelayTicks() + 1) {
                    return StateResult.RUNNING;
                }
                phase = Phase.VERIFY;
                tickInPhase = 1;
                return StateResult.RUNNING;

            case VERIFY: {
                // Wait for server to sync inventory state before checking
                if (tickInPhase <= config.getVerifyWaitTicks()) {
                    tickInPhase++;
                    return StateResult.RUNNING;
                }

                BlockPos out = ctx.getStation().getOutput();
                // Check if item is still in player inventory (deposit failed)
                boolean stillInInventory = false;
                int stillCount = 0;
                for (int i = handler.slots.size() - 36; i < handler.slots.size(); i++) {
                    ItemStack stack = handler.slots.get(i).getStack();
                    if (!stack.isEmpty()) {
                        String id = net.minecraft.registry.Registries.ITEM.getId(stack.getItem()).toString();
                        if (id.equals(ctx.getRecipe().getResult().itemId)) {
                            stillInInventory = true;
                            stillCount += stack.getCount();
                        }
                    }
                }

                if (stillInInventory) {
                    depositRetryCount++;
                    AutoCraftMod.LOGGER.warn("Deposit verify failed: {} result items still in inventory (retry {}/{})",
                            stillCount, depositRetryCount, config.getMaxVerifyRetries());

                    // Check per-deposit retry limit
                    if (depositRetryCount >= config.getMaxVerifyRetries()) {
                        AutoCraftMod.LOGGER.warn("Deposit retry limit reached — checking if output is full");
                        // Fall through to timeout check
                        RetryTracker tracker = ctx.getOrCreateRetryTracker(out);
                        if (!tracker.isActive()) tracker.start();
                        tracker.recordCheck();
                        if (tracker.isTimedOut()) {
                            ContainerInteractor.closeContainer();
                            ctx.stop("Output full — timeout");
                            return StateResult.STOPPED;
                        }
                        // Skip this slot for now, try to find another or move on
                        phase = Phase.FINDING_RESULT;
                        tickInPhase = 0;
                        return StateResult.RUNNING;
                    }

                    // Retry deposit of this slot
                    phase = Phase.DEPOSITING;
                    tickInPhase = 0;
                    return StateResult.RUNNING;
                }

                // Success — item left inventory
                AutoCraftMod.LOGGER.info("Deposit verified: item moved to output container");
                ctx.getOrCreateRetryTracker(out).reset();
                phase = Phase.DONE;
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
