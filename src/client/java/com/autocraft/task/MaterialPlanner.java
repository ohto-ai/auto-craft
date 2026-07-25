package com.autocraft.task;

import com.autocraft.recipe.CraftingRecipe;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;

import java.util.*;

/**
 * Plans material requirements for a crafting batch.
 * Calculates:
 *   1. How many crafts can fit in one grid fill (max 64 per slot)
 *   2. How many crafts are needed to reach target
 *   3. What materials to take from input containers
 */
public class MaterialPlanner {

    public static class MaterialPlan {
        public final Map<String, Integer> playerHas = new LinkedHashMap<>();
        public final Map<String, Integer> needFromContainers = new LinkedHashMap<>();
        public final Map<String, Integer> totalNeeded = new LinkedHashMap<>();
        public int recipeYield;
        public int batchSize; // how many crafts this batch will do
        public boolean sufficient;

        public boolean isEmpty() { return needFromContainers.isEmpty(); }
        public int getUniqueItemTypesNeeded() { return needFromContainers.size(); }
    }

    /**
     * Plan one batch of crafting.
     * @param remaining how many result items still needed
     */
    public static MaterialPlan plan(CraftingRecipe recipe, int remaining) {
        MaterialPlan plan = new MaterialPlan();
        plan.recipeYield = Math.max(1, recipe.getResult().count);

        // — 1. Determine batch size —
        // Max crafts we can do from one grid fill (64 items per slot)
        int maxFromGrid = 64;
        for (var entry : recipe.getFilledSlots().entrySet()) {
            int perSlot = entry.getValue().count;
            if (perSlot > 0) maxFromGrid = Math.min(maxFromGrid, 64 / perSlot);
        }

        // How many crafts needed to reach target
        int remainingCrafts = (int) Math.ceil((double) remaining / plan.recipeYield);

        // This batch: fill grid once, do this many crafts
        plan.batchSize = Math.min(maxFromGrid, remainingCrafts);

        // — 2. Scan player inventory —
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) { plan.sufficient = false; return plan; }
        PlayerInventory inv = client.player.getInventory();
        Map<String, Integer> playerItems = new HashMap<>();
        for (int i = 0; i < 36; i++) {
            ItemStack s = inv.getStack(i);
            if (!s.isEmpty()) playerItems.merge(Registries.ITEM.getId(s.getItem()).toString(), s.getCount(), Integer::sum);
        }

        // — 3. Calculate total needed for this batch —
        int batch = plan.batchSize;
        for (var entry : recipe.getFilledSlots().entrySet()) {
            CraftingRecipe.GridEntry ge = entry.getValue();
            if (ge.itemId == null || ge.itemId.isEmpty()) continue;
            int needed = ge.count * batch;
            plan.totalNeeded.merge(ge.itemId, needed, Integer::sum);
        }

        // — 4. Subtract what player already has —
        for (var entry : plan.totalNeeded.entrySet()) {
            String id = entry.getKey();
            int need = entry.getValue();
            int has = playerItems.getOrDefault(id, 0);
            plan.playerHas.put(id, Math.min(has, need));
            int take = need - has;
            if (take > 0) plan.needFromContainers.put(id, take);
        }

        plan.sufficient = true;
        return plan;
    }
}
