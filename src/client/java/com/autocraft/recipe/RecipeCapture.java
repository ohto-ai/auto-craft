package com.autocraft.recipe;

import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.CraftingScreenHandler;

/**
 * Utility to capture a crafting recipe from an open crafting table screen handler.
 */
public final class RecipeCapture {

    private RecipeCapture() {}

    public static CraftingRecipe capture(CraftingScreenHandler handler) {
        CraftingRecipe recipe = new CraftingRecipe();
        boolean hasAny = false;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = handler.getSlot(i + 1).getStack();
            if (!stack.isEmpty()) {
                // Normalize: recipe pattern stores 1 per occupied slot, batch handles quantity
                recipe.setSlot(i, Registries.ITEM.getId(stack.getItem()).toString(), 1);
                hasAny = true;
            }
        }
        if (!hasAny) return null;
        ItemStack result = handler.getSlot(0).getStack();
        if (!result.isEmpty()) {
            recipe.setResult(Registries.ITEM.getId(result.getItem()).toString(), result.getCount());
        }
        return recipe;
    }
}
