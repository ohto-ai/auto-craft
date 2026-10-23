package com.autocraft.task.states;

import com.autocraft.AutoCraftMod;
import com.autocraft.config.ModConfig;
import com.autocraft.interaction.ContainerInteractor;
import com.autocraft.task.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.util.math.BlockPos;
import java.util.*;

/**
 * Opens the current input container, takes needed materials, moves to next.
 *
 * Phase order: ENTER → OPENING → GRAB → VERIFY → NEXT or DONE
 *
 * ENTER:    Determine if we need anything. If not → workbench.
 * OPENING:  Right-click container, wait for HandledScreen.
 * GRAB:     For each needed item, shift-click ONE slot per tick (rate-limited).
 * VERIFY:   Wait for server sync, re-scan container to confirm items were taken.
 * NEXT:     Close, advance index, loop to next container or done.
 */
public class TakeMaterialsState implements TaskState {

    private enum Phase { ENTER, OPENING, GRAB, VERIFY, NEXT, DONE, WAIT_RETRY }
    private Phase phase;
    private int tickInPhase;
    private final List<String> needList = new ArrayList<>();
    private int retryWaitTicks;

    // GRAB phase — rate-limited, one quickMove per tick
    private int grabItemIdx;        // index into needList
    private int grabDelayCounter;   // ticks to wait between quickMoves

    // VERIFY phase
    private int verifyWaitCounter;  // ticks to wait for server sync
    private int verifyRetryCount;   // how many times we've retried

    @Override public void enter(CraftingTask ctx) {
        phase = Phase.ENTER;
        tickInPhase = 0;
        needList.clear();
    }

    @Override public StateResult tick(CraftingTask ctx) {
        tickInPhase++;
        var plan = ctx.getCurrentMaterialPlan();
        var inputs = ctx.getStation().getInputs();
        int idx = ctx.getCurrentInputIndex();
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return StateResult.RETRY;

        ModConfig config = AutoCraftMod.getInstance().getConfig();

        switch (phase) {

            case ENTER:
                if (plan == null || plan.needFromContainers.isEmpty()) {
                    AutoCraftMod.LOGGER.info("No materials needed from containers — going to workbench");
                    ctx.transitionTo(new NavigateToWorkbenchState());
                    return StateResult.SUCCESS;
                }
                if (idx >= inputs.size()) {
                    return handleExhausted(ctx);
                }
                needList.clear();
                needList.addAll(plan.needFromContainers.keySet());
                AutoCraftMod.LOGGER.info("Need {} types from containers: {}", needList.size(), needList);
                phase = Phase.OPENING;
                tickInPhase = 0;
                retryWaitTicks = 0;
                ContainerInteractor.openContainer(inputs.get(idx));
                return StateResult.RUNNING;

            case OPENING:
                if (ContainerInteractor.isContainerOpen()) {
                    AutoCraftMod.LOGGER.info("Container #{} opened, scanning...", idx);
                    phase = Phase.GRAB;
                    tickInPhase = 0;
                    grabItemIdx = 0;
                    grabDelayCounter = 0;
                    verifyRetryCount = 0;
                    return StateResult.RUNNING;
                }
                if (tickInPhase > 40) {
                    if (retryWaitTicks++ >= 3) {
                        AutoCraftMod.LOGGER.warn("Container #{} won't open after {} retries — skipping", idx, retryWaitTicks);
                        retryWaitTicks = 0;
                        phase = Phase.NEXT;
                        tickInPhase = 0;
                        return StateResult.RUNNING;
                    }
                    AutoCraftMod.LOGGER.warn("Container #{} not opening, retry {}/3...", idx, retryWaitTicks);
                    ContainerInteractor.openContainer(inputs.get(idx));
                    tickInPhase = 0;
                }
                return StateResult.RUNNING;

            case GRAB: {
                // Wait for click delay between quickMoves
                if (grabDelayCounter > 0) {
                    grabDelayCounter--;
                    return StateResult.RUNNING;
                }

                ScreenHandler handler = client.player.currentScreenHandler;
                int syncId = handler.syncId;
                int containerSize = handler.slots.size() - 36;
                if (containerSize <= 0) {
                    ContainerInteractor.closeContainer();
                    phase = Phase.NEXT;
                    tickInPhase = 0;
                    return StateResult.RUNNING;
                }

                // Skip items no longer needed
                while (grabItemIdx < needList.size()) {
                    String itemId = needList.get(grabItemIdx);
                    int need = plan.needFromContainers.getOrDefault(itemId, 0);
                    if (need <= 0) {
                        grabItemIdx++;
                        continue;
                    }
                    break;
                }

                if (grabItemIdx >= needList.size()) {
                    // All item types processed — move to verification
                    AutoCraftMod.LOGGER.info("GRAB pass complete — verifying...");
                    phase = Phase.VERIFY;
                    tickInPhase = 0;
                    verifyWaitCounter = config.getVerifyWaitTicks();
                    return StateResult.RUNNING;
                }

                // Process ONE quickMove for the current needed item
                String itemId = needList.get(grabItemIdx);
                int need = plan.needFromContainers.getOrDefault(itemId, 0);
                boolean clicked = false;

                // Scan container from the beginning each tick (handles item shifting after quickMove)
                for (int si = 0; si < containerSize && !clicked; si++) {
                    ItemStack stack = handler.slots.get(si).getStack();
                    if (stack.isEmpty()) continue;
                    String sid = Registries.ITEM.getId(stack.getItem()).toString();
                    if (sid.equals(itemId)) {
                        int count = stack.getCount();
                        AutoCraftMod.LOGGER.info("Shift-click container slot {} → {} x{} (need {})",
                                si, itemId, count, need);
                        ContainerInteractor.quickMove(syncId, si);
                        clicked = true;

                        int remaining = need - count;
                        if (remaining <= 0) {
                            plan.needFromContainers.remove(itemId);
                            grabItemIdx++; // move to next item type
                        } else {
                            plan.needFromContainers.put(itemId, remaining);
                            // stay on same item for next tick (more stacks needed)
                        }
                    }
                }

                if (!clicked) {
                    // No more stacks of this item in the container
                    AutoCraftMod.LOGGER.debug("No more {} in container #{}", itemId, idx);
                    grabItemIdx++;
                }

                // Set delay before next quickMove
                grabDelayCounter = config.getClickDelayTicks();
                return StateResult.RUNNING;
            }

            case VERIFY: {
                // Wait for server to sync inventory state
                if (verifyWaitCounter > 0) {
                    verifyWaitCounter--;
                    return StateResult.RUNNING;
                }

                // Rebuild needList from plan
                needList.clear();
                needList.addAll(plan.needFromContainers.keySet());

                // Re-scan container to see if items we still need are actually present
                ScreenHandler handler = client.player.currentScreenHandler;
                int containerSize = handler.slots.size() - 36;
                int stillAvailable = 0;

                for (String itemId : needList) {
                    int need = plan.needFromContainers.getOrDefault(itemId, 0);
                    if (need <= 0) continue;
                    // Count how many of this item are still in the container
                    int available = 0;
                    for (int si = 0; si < containerSize; si++) {
                        ItemStack stack = handler.slots.get(si).getStack();
                        if (!stack.isEmpty()) {
                            String sid = Registries.ITEM.getId(stack.getItem()).toString();
                            if (sid.equals(itemId)) {
                                available += stack.getCount();
                            }
                        }
                    }
                    if (available > 0) {
                        stillAvailable += available;
                        AutoCraftMod.LOGGER.info("Verify: {} still has {} x{} in container", itemId, available, itemId);
                    }
                }

                if (stillAvailable > 0 && verifyRetryCount < config.getMaxVerifyRetries()) {
                    verifyRetryCount++;
                    AutoCraftMod.LOGGER.info("Verify: {} items still in container — retry {}/{}",
                            stillAvailable, verifyRetryCount, config.getMaxVerifyRetries());
                    phase = Phase.GRAB;
                    tickInPhase = 0;
                    grabItemIdx = 0;
                    grabDelayCounter = config.getClickDelayTicks();
                    return StateResult.RUNNING;
                }

                if (stillAvailable > 0) {
                    AutoCraftMod.LOGGER.warn("Verify: {} items still in container after {} retries — giving up",
                            stillAvailable, verifyRetryCount);
                } else {
                    AutoCraftMod.LOGGER.info("Verify: all items taken successfully");
                }

                int stillNeeded = 0;
                for (var entry : plan.needFromContainers.entrySet()) {
                    stillNeeded += entry.getValue();
                }
                AutoCraftMod.LOGGER.info("After grab: {} items still needed from containers", stillNeeded);

                ContainerInteractor.closeContainer();
                phase = Phase.NEXT;
                tickInPhase = 0;
                return StateResult.RUNNING;
            }

            case NEXT:
                if (tickInPhase > 3) {
                    if (needList.isEmpty() || plan.needFromContainers.isEmpty()) {
                        AutoCraftMod.LOGGER.info("All materials taken!");
                        ctx.transitionTo(new NavigateToWorkbenchState());
                        return StateResult.SUCCESS;
                    }
                    ctx.setCurrentInputIndex(idx + 1);
                    ctx.transitionTo(new NavigateToInputState());
                    return StateResult.SUCCESS;
                }
                return StateResult.RUNNING;

            case WAIT_RETRY:
                if (tickInPhase > 40) {
                    ctx.setCurrentInputIndex(0);
                    ctx.transitionTo(new NavigateToInputState());
                    return StateResult.SUCCESS;
                }
                return StateResult.RUNNING;

            case DONE:
                return StateResult.SUCCESS;
        }
        return StateResult.RUNNING;
    }

    private StateResult handleExhausted(CraftingTask ctx) {
        var inputs = ctx.getStation().getInputs();
        BlockPos last = inputs.isEmpty() ? null : inputs.get(inputs.size() - 1);
        if (last != null) {
            RetryTracker tracker = ctx.getOrCreateRetryTracker(last);
            if (!tracker.isActive()) tracker.start();
            tracker.recordCheck();
            if (tracker.isTimedOut()) {
                ctx.stop("Materials exhausted after 30s timeout");
                return StateResult.STOPPED;
            }
        }
        AutoCraftMod.LOGGER.info("All containers checked — waiting 2s before retry (check {})",
                last != null ? ctx.getOrCreateRetryTracker(last).getCheckCount() : 0);
        phase = Phase.WAIT_RETRY;
        tickInPhase = 0;
        return StateResult.RUNNING;
    }

    @Override public void exit(CraftingTask ctx) {
        if (ContainerInteractor.isContainerOpen()) ContainerInteractor.closeContainer();
    }
    @Override public String getDescription() { return "Taking materials..."; }
}
