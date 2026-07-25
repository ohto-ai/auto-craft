package com.autocraft.interaction;

import net.minecraft.screen.slot.SlotActionType;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a sequence of click operations to achieve a transfer goal.
 * Computes the optimal clicks to move an exact quantity of items while
 * minimizing the number of packets sent.
 */
public class SlotClickPlan {

    /**
     * A single planned click action.
     */
    public record ClickStep(
            int slotIndex,
            int button,         // 0=left, 1=right
            SlotActionType action,
            String description
    ) {}

    private final List<ClickStep> steps = new ArrayList<>();

    public void addStep(ClickStep step) {
        steps.add(step);
    }

    /**
     * Add a quick-move step (shift-click).
     */
    public void addQuickMove(int slot, String desc) {
        steps.add(new ClickStep(slot, 1, SlotActionType.QUICK_MOVE, desc));
    }

    /**
     * Add pickup steps for taking a stack.
     */
    public void addPickup(int slot, String desc) {
        steps.add(new ClickStep(slot, 0, SlotActionType.PICKUP, desc));
    }

    /**
     * Place one item from cursor into a slot.
     */
    public void addPlaceOne(int slot, String desc) {
        steps.add(new ClickStep(slot, 1, SlotActionType.PICKUP, desc));
    }

    /**
     * Place all items from cursor into a slot.
     */
    public void addPlaceAll(int slot, String desc) {
        steps.add(new ClickStep(slot, 0, SlotActionType.PICKUP, desc));
    }

    public List<ClickStep> getSteps() {
        return steps;
    }

    public int getStepCount() {
        return steps.size();
    }

    /**
     * Build a plan to move exactly {@code amount} items from a source slot
     * to a destination slot using quick-move or precise clicks.
     *
     * For full-stack moves: use one quick-move (shift-click).
     * For partial moves: pick up source, right-click dest N times for exact count.
     */
    public static SlotClickPlan transferItems(
            int sourceSlot, int destSlot, int amount, int sourceStackSize) {

        SlotClickPlan plan = new SlotClickPlan();

        if (amount >= sourceStackSize) {
            // Move the entire stack at once
            plan.addQuickMove(sourceSlot,
                    "Shift-click " + amount + " items from slot " + sourceSlot);
        } else {
            // Pick up the source stack
            plan.addPickup(sourceSlot, "Pick up stack from slot " + sourceSlot);
            // Place N items into dest by right-clicking N times
            for (int i = 0; i < amount; i++) {
                plan.addPlaceOne(destSlot, "Place 1 item into slot " + destSlot +
                        " (" + (i + 1) + "/" + amount + ")");
            }
            // Put remaining items back in source
            plan.addPickup(sourceSlot, "Return remaining items to slot " + sourceSlot);
        }

        return plan;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("SlotClickPlan[" + steps.size() + " steps]:");
        for (var step : steps) {
            sb.append("\n  ").append(step.description);
        }
        return sb.toString();
    }
}
