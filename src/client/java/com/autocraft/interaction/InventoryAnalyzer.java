package com.autocraft.interaction;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;

import java.util.*;

/**
 * Scans player and container inventories to count items and find slot positions.
 */
public final class InventoryAnalyzer {

    private InventoryAnalyzer() {}

    /**
     * Result of scanning an inventory area.
     */
    public static class InventorySnapshot {
        /** itemId → total count found */
        public final Map<String, Integer> itemCounts = new LinkedHashMap<>();
        /** itemId → list of slot indices where this item appears */
        public final Map<String, List<Integer>> itemSlots = new LinkedHashMap<>();
        public final int totalSlots;
        public final int emptySlots;

        public InventorySnapshot(Map<String, Integer> counts, Map<String, List<Integer>> slots,
                                  int totalSlots, int emptySlots) {
            this.itemCounts.putAll(counts);
            this.itemSlots.putAll(slots);
            this.totalSlots = totalSlots;
            this.emptySlots = emptySlots;
        }

        public int getCount(String itemId) {
            return itemCounts.getOrDefault(itemId, 0);
        }

        public boolean hasEnough(String itemId, int required) {
            return getCount(itemId) >= required;
        }
    }

    /**
     * Scan the player's main inventory (36 slots: hotbar + main).
     */
    public static InventorySnapshot scanPlayerInventory() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return new InventorySnapshot(Map.of(), Map.of(), 0, 0);
        }

        PlayerInventory inv = client.player.getInventory();
        return scanSlots(inv.main, 0, 36);
    }

    /**
     * Scan an arbitrary list of ItemStacks (e.g., container slots from a ScreenHandler).
     */
    public static InventorySnapshot scanSlots(List<ItemStack> stacks, int startIndex, int count) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        Map<String, List<Integer>> slots = new LinkedHashMap<>();
        int emptySlots = 0;

        for (int i = startIndex; i < startIndex + count && i < stacks.size(); i++) {
            ItemStack stack = stacks.get(i);
            if (stack.isEmpty()) {
                emptySlots++;
                continue;
            }
            String itemId = Registries.ITEM.getId(stack.getItem()).toString();
            counts.merge(itemId, stack.getCount(), Integer::sum);
            slots.computeIfAbsent(itemId, k -> new ArrayList<>()).add(i);
        }

        return new InventorySnapshot(counts, slots, count, emptySlots);
    }

    /**
     * Find the first slot index containing the given item in a list of stacks.
     * Returns -1 if not found.
     */
    public static int findItemInSlots(List<ItemStack> stacks, String itemId, int startIndex, int endIndex) {
        for (int i = startIndex; i < endIndex && i < stacks.size(); i++) {
            ItemStack stack = stacks.get(i);
            if (!stack.isEmpty()) {
                String id = Registries.ITEM.getId(stack.getItem()).toString();
                if (id.equals(itemId)) return i;
            }
        }
        return -1;
    }
}
