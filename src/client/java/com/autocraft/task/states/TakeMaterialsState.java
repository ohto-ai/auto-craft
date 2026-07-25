package com.autocraft.task.states;

import com.autocraft.AutoCraftMod;
import com.autocraft.interaction.ContainerInteractor;
import com.autocraft.task.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.math.BlockPos;
import java.util.*;

/**
 * Opens the current input container, takes needed materials, moves to next.
 *
 * Phase order: ENTER → OPENING → GRAB → CHECK → NEXT or DONE
 *
 * ENTER:    Determine if we need anything. If not → workbench.
 * OPENING:  Right-click container, wait for HandledScreen.
 * GRAB:     For each needed item, shift-click its slot(s).
 * CHECK:    Verify items are in player inventory.
 * NEXT:     Close, advance index, loop to next container or done.
 */
public class TakeMaterialsState implements TaskState {

    private enum Phase { ENTER, OPENING, GRAB, CHECK, NEXT, DONE, WAIT_RETRY }
    private Phase phase;
    private int tickInPhase;
    private final List<String> needList = new ArrayList<>();
    private int retryWaitTicks; // how many ticks to wait before retrying

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
                    return StateResult.RUNNING;
                }
                if (tickInPhase > 40) {
                    // Retry up to 3 times (6s total), then skip this container
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
                ScreenHandler handler = client.player.currentScreenHandler;
                int syncId = handler.syncId;
                int containerSize = handler.slots.size() - 36;
                if (containerSize <= 0) {
                    ContainerInteractor.closeContainer();
                    phase = Phase.NEXT;
                    tickInPhase = 0;
                    return StateResult.RUNNING;
                }

                // For each needed item: find ALL matching stacks and shift-click them
                // until the plan's need is met (or container runs out)
                for (Iterator<String> it = needList.iterator(); it.hasNext(); ) {
                    String itemId = it.next();
                    int need = plan.needFromContainers.getOrDefault(itemId, 0);
                    if (need <= 0) { it.remove(); continue; }

                    int taken = 0;
                    for (int si = 0; si < containerSize && taken < need; si++) {
                        ItemStack stack = handler.slots.get(si).getStack();
                        if (stack.isEmpty()) continue;
                        String sid = Registries.ITEM.getId(stack.getItem()).toString();
                        if (sid.equals(itemId)) {
                            AutoCraftMod.LOGGER.info("Shift-click container slot {} → {} x{} (need {})",
                                    si, itemId, stack.getCount(), need - taken);
                            ContainerInteractor.quickMove(syncId, si);
                            taken += stack.getCount();
                        }
                    }
                    int remaining = need - taken;
                    if (remaining <= 0) {
                        plan.needFromContainers.remove(itemId);
                        it.remove();
                    } else {
                        plan.needFromContainers.put(itemId, remaining);
                    }
                }

                phase = Phase.CHECK;
                tickInPhase = 0;
                return StateResult.RUNNING;
            }

            case CHECK: {
                // Verify items in player inventory
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
                // Wait ~2 seconds between retry cycles
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
        var plan = ctx.getCurrentMaterialPlan();
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
        // Wait before retrying
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
