package com.autocraft.task.states;

import com.autocraft.AutoCraftMod;
import com.autocraft.interaction.ContainerInteractor;
import com.autocraft.recipe.CraftingRecipe;
import com.autocraft.task.CraftingTask;
import com.autocraft.task.MaterialPlanner;
import com.autocraft.task.TaskState;
import java.util.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;

/**
 * Opens workbench and places recipe items in the 3x3 grid.
 * Places enough items for an entire batch (up to 64 per slot).
 *
 * CraftingScreenHandler slots: result=0, grid=1-9, player inv=10+
 *
 * For each grid slot: pickup from inv → place N items in grid → return remainder.
 */
public class PlaceRecipeState implements TaskState {

    private static final int MAX_PER_SLOT = 64;

    private enum Phase { OPENING, PREPARE, PLACING, DONE }
    private Phase phase;
    private int tickInPhase;
    private int gridIdx;        // 0-8, which grid position we're on
    private int subPhase;       // 0=pickup, 1=placing items, 2=return
    private int itemsToPlace;   // total items needed in current grid slot
    private int itemsPlaced;    // how many placed so far
    private int invSlot;        // inventory slot we're using
    private int[] needed;       // items needed per grid slot (0-8)

    @Override public void enter(CraftingTask ctx) {
        phase = Phase.OPENING;
        tickInPhase = 0;
        ContainerInteractor.openContainer(ctx.getStation().getWorkbench());
    }

    @Override public StateResult tick(CraftingTask ctx) {
        tickInPhase++;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return StateResult.RETRY;

        switch (phase) {
            case OPENING:
                if (ContainerInteractor.isContainerOpen()) {
                    phase = Phase.PREPARE;
                    tickInPhase = 0;
                    return StateResult.RUNNING;
                }
                if (tickInPhase > 30) {
                    ContainerInteractor.openContainer(ctx.getStation().getWorkbench());
                    tickInPhase = 0;
                }
                return StateResult.RUNNING;

            case PREPARE: {
                MaterialPlanner.MaterialPlan plan = ctx.getCurrentMaterialPlan();
                int batch = plan != null ? plan.batchSize : 1;
                needed = new int[9];
                for (int i = 0; i < 9; i++) {
                    CraftingRecipe.GridEntry e = ctx.getRecipe().getSlot(i);
                    if (e != null) needed[i] = Math.min(MAX_PER_SLOT, e.count * batch);
                }
                AutoCraftMod.LOGGER.info("Placing batch of {} crafts", batch);
                phase = Phase.PLACING;
                gridIdx = 0;
                subPhase = 0;
                tickInPhase = 0;
                return StateResult.RUNNING;
            }

            case PLACING: {
                if (gridIdx >= 9) {
                    AutoCraftMod.LOGGER.debug("Grid placed: {} items in batch", summarize());
                    ctx.transitionTo(new TakeResultState());
                    return StateResult.SUCCESS;
                }

                if (needed[gridIdx] == 0) {
                    gridIdx++;
                    subPhase = 0;
                    return StateResult.RUNNING;
                }

                CraftingRecipe.GridEntry entry = ctx.getRecipe().getSlot(gridIdx);
                ScreenHandler handler = client.player.currentScreenHandler;
                int syncId = handler.syncId;
                int gridSlot = gridIdx + 1;
                int need = needed[gridIdx];

                switch (subPhase) {
                    case 0: // PICKUP from inventory
                        invSlot = findItemSlot(handler, entry.itemId);
                        if (invSlot < 0) {
                            AutoCraftMod.LOGGER.warn("No {} in inventory for grid[{}]", entry.itemId, gridIdx);
                            gridIdx++;
                            subPhase = 0;
                            return StateResult.RUNNING;
                        }
                        ContainerInteractor.leftClick(syncId, invSlot);
                        subPhase = 1;
                        tickInPhase = 0;
                        return StateResult.RUNNING;

                    case 1: // PLACE items into grid
                        // Check cursor: if it has exactly what we need, left-click to place all at once
                        ItemStack cursor = handler.getCursorStack();
                        int onCursor = cursor.getCount();

                        if (onCursor == need) {
                            // Exact match — one left-click places everything
                            ContainerInteractor.leftClick(syncId, gridSlot);
                            gridIdx++;
                            subPhase = 0;
                            return StateResult.RUNNING;
                        } else if (onCursor > need) {
                            // Too many — right-click N times to place exactly N, then return remainder
                            if (tickInPhase == 1) {
                                itemsPlaced = 0;
                            }
                            ContainerInteractor.rightClick(syncId, gridSlot);
                            itemsPlaced++;
                            if (itemsPlaced < need) return StateResult.RUNNING;
                            // Return remainder
                            ContainerInteractor.leftClick(syncId, invSlot);
                            gridIdx++;
                            subPhase = 0;
                            return StateResult.RUNNING;
                        } else {
                            // Not enough on cursor — place what we have, get more
                            ContainerInteractor.leftClick(syncId, gridSlot); // place all
                            itemsPlaced += onCursor;
                            if (itemsPlaced >= need) {
                                gridIdx++;
                                subPhase = 0;
                                return StateResult.RUNNING;
                            }
                            // Get more from another slot
                            subPhase = 0; // back to pickup
                            return StateResult.RUNNING;
                        }
                }
            }
            case DONE:
                return StateResult.SUCCESS;
        }
        return StateResult.RUNNING;
    }

    /** Find inventory slot with the MOST items of this type (best candidate for pickup). */
    private int findItemSlot(ScreenHandler handler, String itemId) {
        int bestSlot = -1;
        int bestCount = 0;
        for (int i = 10; i < handler.slots.size(); i++) {
            ItemStack s = handler.slots.get(i).getStack();
            if (!s.isEmpty() && Registries.ITEM.getId(s.getItem()).toString().equals(itemId)) {
                if (s.getCount() > bestCount) {
                    bestCount = s.getCount();
                    bestSlot = i;
                }
            }
        }
        return bestSlot;
    }

    private int summarize() {
        int total = 0;
        for (int n : needed) total += n;
        return total;
    }

    @Override public void exit(CraftingTask ctx) {}
    @Override public String getDescription() { return "Placing recipe..."; }
}
