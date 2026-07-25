package com.autocraft.util;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;

/**
 * Utility for fuzzy-matching ItemStacks (ignoring count and minor NBT differences).
 */
public final class ItemMatcher {

    private ItemMatcher() {}

    /**
     * Check whether two ItemStacks refer to the same item type,
     * ignoring count. For most crafting purposes this is sufficient.
     */
    public static boolean sameItem(ItemStack a, ItemStack b) {
        if (a.isEmpty() || b.isEmpty()) return false;
        return ItemStack.areItemsEqual(a, b);
    }

    /**
     * Check whether a stack matches a specific item ID.
     */
    public static boolean matchesItemId(ItemStack stack, String itemId) {
        if (stack.isEmpty()) return false;
        Item item = Registries.ITEM.get(net.minecraft.util.Identifier.of(itemId));
        return stack.isOf(item);
    }

    /**
     * Get the total count of items matching the given ItemStack in an inventory slot array.
     */
    public static int countMatching(Iterable<ItemStack> inventory, ItemStack reference) {
        int total = 0;
        for (ItemStack stack : inventory) {
            if (sameItem(stack, reference)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    /**
     * Check if a stack is empty or air.
     */
    public static boolean isEmpty(ItemStack stack) {
        return stack == null || stack.isEmpty();
    }

    /**
     * Get the max stack size for an item (usually 64, but some items like
     * ender pearls or buckets have smaller stack limits).
     */
    public static int getMaxStackSize(ItemStack stack) {
        if (stack.isEmpty()) return 64;
        return stack.getMaxCount();
    }
}
