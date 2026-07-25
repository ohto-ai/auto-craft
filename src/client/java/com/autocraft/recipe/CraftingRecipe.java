package com.autocraft.recipe;

import com.google.gson.annotations.Expose;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.*;

/**
 * Represents a saved crafting recipe: a 3×3 grid pattern with an expected result.
 * Grid slots are indexed 0-8 (left-to-right, top-to-bottom).
 * A null grid entry means an empty slot.
 */
public class CraftingRecipe {

    @Expose private String name;
    @Expose private final GridEntry[] grid = new GridEntry[9];
    @Expose private ResultEntry result = new ResultEntry();

    public CraftingRecipe() {}

    public CraftingRecipe(String name) {
        this.name = name;
    }

    // --- Getters / Setters ---

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get the recipe entry for a grid slot (0-8). Returns null if the slot is empty.
     */
    public GridEntry getSlot(int index) {
        if (index < 0 || index >= 9) return null;
        return grid[index];
    }

    public void setSlot(int index, String itemId, int count) {
        if (index < 0 || index >= 9) return;
        grid[index] = new GridEntry(itemId, count);
    }

    public void clearSlot(int index) {
        if (index < 0 || index >= 9) return;
        grid[index] = null;
    }

    public ResultEntry getResult() {
        return result;
    }

    public void setResult(String itemId, int count) {
        this.result = new ResultEntry(itemId, count);
    }

    /**
     * Returns the set of unique item IDs used in this recipe (empty slots excluded).
     */
    public Set<String> getIngredientItemIds() {
        Set<String> ids = new LinkedHashSet<>();
        for (GridEntry entry : grid) {
            if (entry != null && entry.itemId != null && !entry.itemId.isEmpty()) {
                ids.add(entry.itemId);
            }
        }
        return ids;
    }

    /**
     * Returns the total count of a specific ingredient across all grid slots.
     */
    public int getTotalIngredientCount(String itemId) {
        int total = 0;
        for (GridEntry entry : grid) {
            if (entry != null && itemId.equals(entry.itemId)) {
                total += entry.count;
            }
        }
        return total;
    }

    /**
     * Returns all non-empty grid slots as a map of slotIndex -> GridEntry.
     */
    public Map<Integer, GridEntry> getFilledSlots() {
        Map<Integer, GridEntry> filled = new LinkedHashMap<>();
        for (int i = 0; i < 9; i++) {
            if (grid[i] != null && grid[i].itemId != null && !grid[i].itemId.isEmpty()) {
                filled.put(i, grid[i]);
            }
        }
        return filled;
    }

    /**
     * Check whether this recipe has any meaningful content.
     */
    public boolean isEmpty() {
        for (GridEntry entry : grid) {
            if (entry != null && entry.itemId != null && !entry.itemId.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Create an ItemStack for the result, or ItemStack.EMPTY if invalid.
     */
    public ItemStack createResultStack() {
        if (result.itemId == null || result.itemId.isEmpty()) return ItemStack.EMPTY;
        var item = Registries.ITEM.get(Identifier.of(result.itemId));
        if (item == Items.AIR) return ItemStack.EMPTY;
        return new ItemStack(item, Math.max(1, result.count));
    }

    /**
     * Create an ItemStack for a grid slot entry.
     */
    public static ItemStack createItemStack(GridEntry entry) {
        if (entry == null || entry.itemId == null || entry.itemId.isEmpty()) return ItemStack.EMPTY;
        var item = Registries.ITEM.get(Identifier.of(entry.itemId));
        if (item == Items.AIR) return ItemStack.EMPTY;
        return new ItemStack(item, Math.max(1, entry.count));
    }

    // --- Inner data classes ---

    public static class GridEntry {
        @Expose public String itemId;
        @Expose public int count = 1;

        public GridEntry() {}

        public GridEntry(String itemId, int count) {
            this.itemId = itemId;
            this.count = count;
        }

        public ItemStack toStack() {
            return createItemStack(this);
        }
    }

    public static class ResultEntry {
        @Expose public String itemId = "";
        @Expose public int count = 1;

        public ResultEntry() {}

        public ResultEntry(String itemId, int count) {
            this.itemId = itemId;
            this.count = count;
        }
    }

    @Override
    public String toString() {
        return "CraftingRecipe{name='" + name + "', result=" + result.itemId + " x" + result.count + "}";
    }
}
